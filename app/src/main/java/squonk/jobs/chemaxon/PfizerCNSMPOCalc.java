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
 * Calculates CNS MPO score
 * See Wager et al. DOI: 10.1021/cn100008c
 */
public class PfizerCNSMPOCalc implements Calculator {

    private static final Logger LOG = Logger.getLogger(PfizerCNSMPOCalc.class.getName());
    private static final DMLogger DMLOG = new DMLogger();

    public static final String SCORE_FIELD = "Pfizer_CNS_MPO";

    private final ChemTermsCalculator[] calculators;
    private static final NumberTransform[] transforms = new NumberTransform[]{
        MpoFunctions.createRampFunction(1d, 0d, 3d, 5d),
                MpoFunctions.createRampFunction(1d, 0d, 2d, 4d),
                MpoFunctions.createRampFunction(1d, 0d, 360d, 500d),
                MpoFunctions.createHump1Function(0d, 1d, 0d, 20d, 40d, 90d, 120d),
                MpoFunctions.createRampFunction(1d, 0d, 0.5d, 3.5d),
                MpoFunctions.createRampFunction(1d, 0d, 8d, 10d)
    };

    public PfizerCNSMPOCalc() {
        final CalculatorsExec exec = new CalculatorsExec();

        this.calculators = exec.createCalculators(
                new ChemTermsCalculator.Calc[]{
                        ChemTermsCalculator.Calc.LogP,
                        ChemTermsCalculator.Calc.LogD,
                        ChemTermsCalculator.Calc.MolecularWeight,
                        ChemTermsCalculator.Calc.TPSA,
                        ChemTermsCalculator.Calc.HBondDonorCount,
                        ChemTermsCalculator.Calc.BasicPKa
                },
                new Object[][]{
                        null,
                        new Object[]{7.4f},
                        null,
                        null,
                        null,
                        null
                });
    }

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

            PfizerCNSMPOCalc calc = new PfizerCNSMPOCalc();
            calc.calculate(inputFile, outputFile, header, filterMode, minValue, maxValue);
        }
    }

    public int[] calculate(String inputFile, String outputFile, boolean includeHeader, FilterMode mode,
                           Float minValue, Float maxValue) throws IOException {
        // read mols as stream
        Stream<MoleculeObject> mols = MoleculeUtils.readMoleculesAsStream(inputFile);
        final Map<String, Integer> stats = new HashMap<>();

        AtomicInteger errorCount = new AtomicInteger(0);

        mols = mols.peek(mo -> {
            if (mo == null) {
                errorCount.incrementAndGet();
            } else {
                calculate(mo, stats);
            }
        });


        // we need to count the actual molecules calculated as the final number may be filtered
        final AtomicInteger total = new AtomicInteger(0);
        mols = mols.peek(m -> {
            if (m != null) {
                total.incrementAndGet();
            }
        });

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
        return new int[]{(int) count, (int) errorCount.get()};
    }

    /**
     * Performs the CNS MPO calculation
     *
     * @param mo
     * @param stats
     * @return
     */
    public Double calculate(MoleculeObject mo, Map<String, Integer> stats) {

        Molecule mol = mo.getMol();

        // this does the calculations that are used to generate the MPO score
        Double logp = (Double) calculators[0].processMolecule(mol, stats);
        Double logd = (Double) calculators[1].processMolecule(mol, stats);
        Double mw = (Double) calculators[2].processMolecule(mol, stats);
        Double tpsa = (Double) calculators[3].processMolecule(mol, stats);
        Integer hbd = (Integer) calculators[4].processMolecule(mol, stats);
        Double bpka = (Double) calculators[5].processMolecule(mol, stats);

        Double score = calculateScore(logp, logd, mw, tpsa, hbd, bpka);
        if (score != null) {
            mo.setProperty(SCORE_FIELD, score);
        }
        return score;
    }

    protected static Double calculateScore(Double logp, Double logd, Double mw, Double tpsa, Integer hbd, Double bpka) {

        LOG.finer(String.format("Inputs are: logp=%s logd=%s mw=%s tpsa=%s hbd=%s bpka=%s",
                logp, logd, mw, tpsa, hbd, bpka));

        if (logp == null || logd == null || mw == null || tpsa == null || hbd == null) {
            LOG.info(String.format("Data missing. Inputs logp=%s logd=%s mw=%s tpsa=%s hbd=%s bpka=%s",
                    logp, logd, mw, tpsa, hbd, bpka));
            return null;
        }

        Double logp_score = transforms[0].transform(logp);
        Double logd_score = transforms[1].transform(logd);
        Double mw_score = transforms[2].transform(mw);
        Double tpsa_score = transforms[3].transform(tpsa);
        Double hbd_score = transforms[4].transform(hbd.doubleValue());
        // if no pKa score then value of 1 should be used
        Double bpka_score = (bpka == null ? 1d : transforms[5].transform(bpka));

        Double score_mpo = Utils.roundToSignificantFigures(
                logp_score + logd_score + mw_score + tpsa_score + hbd_score + bpka_score, 4);

        return score_mpo;
    }

}
