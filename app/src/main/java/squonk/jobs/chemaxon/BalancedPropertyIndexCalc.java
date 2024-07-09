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
 * Calculates Balanced Property Index (BPI).
 * See Weiss et al. https://doi.org/10.1021/acsmedchemlett.3c00542
 */
public class BalancedPropertyIndexCalc implements Calculator {

    private final ChemTermsCalculator[] calculators;

    public BalancedPropertyIndexCalc() {
        final CalculatorsExec exec = new CalculatorsExec();

        this.calculators = exec.createCalculators(
                new ChemTermsCalculator.Calc[]{
                        ChemTermsCalculator.Calc.HeavyAtomCount,
                        ChemTermsCalculator.Calc.TPSA,
                        ChemTermsCalculator.Calc.LogD
                },
                null);
    }

    private static final Logger LOG = Logger.getLogger(GuptaBBBCalc.class.getName());
    private static final DMLogger DMLOG = new DMLogger();

    public static final String SCORE_FIELD = "BPI";


    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption("h", "help", false, "Display help");
        options.addOption(Option.builder("i").longOpt("input").hasArg().argName("file")
                .desc("Input file with molecules (.sdf)").required().build());
        options.addOption(Option.builder("o").longOpt("output").hasArg().argName("file")
                .desc("Output file for molecules (.sdf)").build());
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
            StringBuilder builder = new StringBuilder(GuptaBBBCalc.class.getName());
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

            BalancedPropertyIndexCalc calc = new BalancedPropertyIndexCalc();
            calc.calculateBPI(inputFile, outputFile, header, filterMode, minValue, maxValue);
        }
    }

    public int[] calculateBPI(String inputFile, String outputFile, boolean includeHeader, FilterMode mode,
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
     * Performs the BBB calculation
     * @param mo
     * @param stats
     * @return
     */
    public Double calculate(MoleculeObject mo, Map<String, Integer> stats) {

        Molecule mol = mo.getMol();

        // this does the calculations that are used to generate the BPI score
        Number hac_num = (Number)calculators[0].processMolecule(mol, stats);
        Integer hac = hac_num == null ? null : hac_num.intValue();
        Double tpsa = (Double)calculators[1].processMolecule(mol, stats);
        Double logd = (Double)calculators[2].processMolecule(mol, stats);

        if (hac == null || tpsa == null || logd == null) {
            LOG.info(String.format("Data missing. Inputs hac=%s tpsa=%s logd=%s", hac, tpsa, logd));
            return null;
        }

        LOG.finer(String.format("Inputs are: hac=%s, tpsa=%s, logd=%s", hac, tpsa, logd));

        double score_bpi = Utils.roundToSignificantFigures(1000 * logd / (tpsa * hac), 4);
        LOG.fine(String.format("Scores are: hac=%s, tpsa=%s, logd=%s, bpi=%s", hac, tpsa, logd, score_bpi));

        mo.setProperty(SCORE_FIELD, score_bpi);

        return score_bpi;
    }

}
