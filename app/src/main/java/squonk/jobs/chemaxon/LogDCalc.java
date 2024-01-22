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

import org.apache.commons.cli.*;
import squonk.jobs.chemaxon.util.*;
import squonk.jobs.chemaxon.util.Filters.FilterMode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class LogDCalc {

    private static final Logger LOG = Logger.getLogger(LogDCalc.class.getName());
    private static final DMLogger DMLOG = new DMLogger();

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption("h", "help", false, "Display help");
        options.addOption(Option.builder("i").longOpt("input").hasArg().argName("file")
                .desc("Input file with molecules (.sdf)").required().build());
        options.addOption(Option.builder("o").longOpt("output").hasArg().argName("file")
                .desc("Output file for molecules (.sdf)").build());
        options.addOption(Option.builder("p").longOpt("ph").hasArg().argName("value").type(Float.class)
                .desc("pH to use").required().build());
        options.addOption(Option.builder("m").longOpt("mode").hasArg().argName("mode")
                .desc("Filter mode [none, pass, fail]").build());
        options.addOption(Option.builder("n").longOpt("min-value").hasArg().argName("logD").type(Float.class)
                .desc("Minimum value for filter").build());
        options.addOption(Option.builder("x").longOpt("max-value").hasArg().argName("logD").type(Float.class)
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
            StringBuilder builder = new StringBuilder(LogDCalc.class.getName());
            for (String arg : args) {
                builder.append(" ").append(arg);
            }
            DMLOG.logEvent(DMLogger.Level.INFO, builder.toString());

            String inputFile = cmd.getOptionValue("input");
            String outputFile = cmd.getOptionValue("output");
            String mode = cmd.getOptionValue("mode");
            FilterMode filterMode = (mode == null ? FilterMode.none : Filters.FilterMode.valueOf(mode));
            Float ph = Float.valueOf(cmd.getOptionValue("ph"));
            Float minValue = cmd.hasOption("min-value") ? Float.valueOf(cmd.getOptionValue("min-value")) : null;
            Float maxValue = cmd.hasOption("max-value") ? Float.valueOf(cmd.getOptionValue("max-value")) : null;
            if (minValue != null || maxValue != null) {
                DMLOG.logEvent(DMLogger.Level.INFO, "Applying min:max filters " + minValue + ":" + maxValue);
            }
            boolean header = Boolean.valueOf(cmd.getOptionValue("header", "true"));

            LogDCalc calc = new LogDCalc();
            calc.calculate(inputFile, outputFile, header, ph, filterMode, minValue, maxValue);
        }
    }

    public int[] calculate(String inputFile, String outputFile, boolean includeHeader, Float ph,
                          FilterMode filterMode, Float minValue, Float maxValue) throws IOException {
        // read mols as stream
        Stream<MoleculeObject> mols = MoleculeUtils.readMoleculesAsStream(inputFile);
        CalculatorsExec exec = new CalculatorsExec();
        Map<String, Integer> stats = new HashMap<>();
        Object[] params = new Object[]{ph};

        ChemTermsCalculator[] calculators = exec.createCalculators(
                new ChemTermsCalculator.Calc[]{ChemTermsCalculator.Calc.LogD},
                new Object[][]{params});

        mols = exec.calculate(mols, calculators, stats);

        // we need to count the actual molecules calculated as the final number may be filtered
        final AtomicInteger total = new AtomicInteger(0);
        mols = mols.peek(mo -> total.incrementAndGet());

        // apply the filters
        mols = Filters.applyFilters(mols, filterMode, calculators[0].getPropName(), minValue, maxValue);

        // if output is defined then set up a MolExporter to write the results
        if (outputFile != null) {
            mols = MoleculeUtils.addFileWriter(mols, outputFile, includeHeader);
        }

        // make sure we consume the stream
        long count = mols.count();
        DMLOG.logEvent(DMLogger.Level.INFO, "Processed " + total + " molecules, " + count + " passed filters");
        DMLOG.logCost((float) total.get(), false);
        return new int[] {(int)count, exec.getErrorCount()};
    }
}
