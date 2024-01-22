/*
 * Copyright (c) 2023 Informatics Matters Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package squonk.jobs.chemaxon;

import chemaxon.struc.Molecule;
import org.apache.commons.cli.*;
import squonk.jobs.chemaxon.util.*;
import squonk.jobs.chemaxon.util.Filters.FilterMode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Calculates KIDS MPO score
 */
public class KidsMPOCalc {

    private static final Logger LOG = Logger.getLogger(KidsMPOCalc.class.getName());
    private static final DMLogger DMLOG = new DMLogger();

    public static final String SCORE_FIELD = "KIDS_MPO";

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption("h", "help", false, "Display help");
        options.addOption(Option.builder("i").longOpt("input").hasArg().argName("file")
                .desc("Input file with molecules (.sdf, .smi)").required().build());
        options.addOption(Option.builder("o").longOpt("output").hasArg().argName("file")
                .desc("Output file for molecules (.sdf, .smi)").build());
        options.addOption(Option.builder("m").longOpt("mode").hasArg().argName("mode")
                .desc("Filter mode [none, pass, fail]").build());
        options.addOption(Option.builder("n").longOpt("min-value").hasArg().argName("score").type(Float.class)
                .desc("Minimum value for filter").build());
        options.addOption(Option.builder("x").longOpt("max-value").hasArg().argName("score").type(Float.class)
                .desc("Maximum value for filter").build());
        options.addOption(Option.builder("h").longOpt("header").hasArg().argName("true/false")
                .desc("Include header line when writing SMILES").type(Boolean.class).build());

        if (args.length == 0 | (args.length == 1 && ("-h".equals(args[0]) | "--help".equals(args[0])))) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(null);
            formatter.printHelp("app", options);
        } else {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            StringBuilder builder = new StringBuilder(PfizerCNSMPOCalc.class.getName());
            for (String arg : args) {
                builder.append(" ").append(arg);
            }
            DMLOG.logEvent(DMLogger.Level.INFO, builder.toString());

            String inputFile = cmd.getOptionValue("input");
            String outputFile = cmd.getOptionValue("output");
            String mode = cmd.getOptionValue("mode");
            FilterMode filterMode = (mode == null ? FilterMode.none : FilterMode.valueOf(mode));
            Float minValue = cmd.hasOption("min-value") ? Float.valueOf(cmd.getOptionValue("min-value")) : null;
            Float maxValue = cmd.hasOption("max-value") ? Float.valueOf(cmd.getOptionValue("max-value")) : null;
            if (minValue != null || maxValue != null) {
                DMLOG.logEvent(DMLogger.Level.INFO, "Applying min:max filters " + minValue + ":" + maxValue +
                        " filter mode is " + filterMode.toString());
            }
            boolean header = Boolean.valueOf(cmd.getOptionValue("header", "true"));

            KidsMPOCalc calc = new KidsMPOCalc();
            calc.calculate(inputFile, outputFile, header, filterMode, minValue, maxValue);
        }
    }

    public int[] calculate(String inputFile, String outputFile, boolean includeHeader, FilterMode mode,
                          Float minValue, Float maxValue) throws IOException {
        // read mols as stream
        Stream<MoleculeObject> mols = MoleculeUtils.readMoleculesAsStream(inputFile);
        final CalculatorsExec exec = new CalculatorsExec();
        final Map<String, Integer> stats = new HashMap<>();

        final ChemTermsCalculator[] calculators = exec.createCalculators(
                new ChemTermsCalculator.Calc[]{
                        ChemTermsCalculator.Calc.TPSA,
                        ChemTermsCalculator.Calc.RotatableBondCount,
                        ChemTermsCalculator.Calc.ElementCount,
                        ChemTermsCalculator.Calc.ElementCount,
                        ChemTermsCalculator.Calc.HBondDonorCount,
                        ChemTermsCalculator.Calc.AromaticRingCount
                },
                new Object[][]{
                        null,
                        null,
                        new Object[] {7},
                        new Object[] {8},
                        null,
                        null
                });

        NumberTransform[] transforms = new NumberTransform[]{
                MpoFunctions.createHump1Function(0d, 1d, 0d, 64.63d, 75.85d, 92.40d, 138.3d),
                MpoFunctions.createHump1Function(0.2d, 1d, 0d, 1d, 2d, 3d, 5d),
                MpoFunctions.createHump2Function(0d, 1d, 0.2d, 0d, 2d, 4d, 5d, 6d, 8d, 9d),
                MpoFunctions.createHump1Function(0.2d, 1d, 0d, 0d, 1d, 1d, 3d),
                MpoFunctions.createHump2Function(0d, 1d, 0.2d, 0d, 0d, 2d, 3d, 4d, 6d, 7d),
                MpoFunctions.createHump2Function(0d, 1d, 0.2d, 0d, 1d, 3d, 3d, 4d, 4d, 5d)
        };

        AtomicInteger errorCount = new AtomicInteger(0);
        mols = mols.peek(mo -> {
            if (mo == null) {
                errorCount.incrementAndGet();
            } else {
                Molecule mol = mo.getMol();
                Double score = doCalculate(mol, calculators, transforms, stats);
                if (score != null) {
                    mo.setProperty(SCORE_FIELD, score);
                }
            }
        });


        // we need to count the actual molecules calculated as the final number may be filtered
        final AtomicInteger total = new AtomicInteger(0);
        mols = mols.peek(mo -> total.incrementAndGet());

        // apply the filters
        mols = Filters.applyFilters(mols, mode, SCORE_FIELD, minValue, maxValue);

        // if output is defined then set up a MolExporter to write the results
        if (outputFile != null) {
            mols = MoleculeUtils.addFileWriter(mols, outputFile, includeHeader);
        }

        // make sure we consume the stream
        long count = mols.count();
        DMLOG.logEvent(DMLogger.Level.INFO, "Processed " + total + " molecules, " + count + " passed filters");
        DMLOG.logCost((float) total.get(), false);
        return new int[] {(int)count, errorCount.get()};
    }

    /**
     * Performs the CNS MPO calculation
     * @param mol
     * @param calculators
     * @param stats
     * @return
     */
    protected Double doCalculate(Molecule mol, ChemTermsCalculator[] calculators,
                                 NumberTransform[] transforms,
                                 Map<String, Integer> stats) {

//        ChemTermsCalculator.Calc.TPSA,
//                ChemTermsCalculator.Calc.RotatableBondCount,
//                ChemTermsCalculator.Calc.ElementCount,
//                ChemTermsCalculator.Calc.ElementCount,
//                ChemTermsCalculator.Calc.HBondDonorCount,
//                ChemTermsCalculator.Calc.AromaticRingCount

        // this does the calculations that are used to generate the MPO score
        Double tpsa = (Double)calculators[0].processMolecule(mol, stats);
        Integer rotb = (Integer)calculators[1].processMolecule(mol, stats);
        Integer n_count = (Integer)calculators[2].processMolecule(mol, stats);
        Integer o_count = (Integer)calculators[3].processMolecule(mol, stats);
        Integer hbd = (Integer)calculators[4].processMolecule(mol, stats);
        Integer aro = (Integer)calculators[5].processMolecule(mol, stats);

        if (tpsa == null || rotb == null || n_count == null || o_count == null || hbd == null || aro == null) {
            LOG.info(String.format("Data missing. Inputs tpsa=%s rotb=%s n_count=%s o_count=%s hdb=%s aro=%s",
                    tpsa, rotb, n_count, o_count, hbd, aro));
            return null;
        }

        LOG.finer(String.format("Inputs are: tpsa=%s rotb=%s n_count=%s o_count=%s hdb=%s aro=%s",
                tpsa, rotb, n_count, o_count, hbd, aro));

        Double tpsa_score = transforms[0].transform(tpsa.doubleValue());
        Double rotb_score = transforms[1].transform(rotb.doubleValue());
        Double n_count_score = transforms[2].transform(n_count.doubleValue());
        Double o_count_score = transforms[3].transform(o_count.doubleValue());
        Double hbd_score = transforms[4].transform(hbd.doubleValue());
        Double aro_score = transforms[5].transform(aro.doubleValue());

        Double score_mpo = Utils.roundToSignificantFigures(
                tpsa_score + rotb_score + n_count_score + o_count_score + hbd_score + aro_score, 4);
        return score_mpo;
    }

}
