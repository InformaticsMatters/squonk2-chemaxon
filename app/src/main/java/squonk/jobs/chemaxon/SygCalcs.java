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
import squonk.jobs.chemaxon.util.ChemTermsCalculator;
import squonk.jobs.chemaxon.util.DMLogger;
import squonk.jobs.chemaxon.util.MoleculeObject;
import squonk.jobs.chemaxon.util.MoleculeUtils;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SygCalcs {


    private static final Logger LOG = Logger.getLogger(SygCalcs.class.getName());
    private static final DMLogger DMLOG = new DMLogger();

    private final ChemTermsCalculator.Calc[] calcs = new ChemTermsCalculator.Calc[]{
            ChemTermsCalculator.Calc.LogP,
            ChemTermsCalculator.Calc.LogD,
            ChemTermsCalculator.Calc.MolecularWeight,
            ChemTermsCalculator.Calc.TPSA,
            ChemTermsCalculator.Calc.HBondDonorCount,
            ChemTermsCalculator.Calc.AromaticRingCount,
            ChemTermsCalculator.Calc.CHIRALC,
            ChemTermsCalculator.Calc.FSP3,
            ChemTermsCalculator.Calc.INCHIK
    };
    private final Object[][] options = new Object[][]{
            null,
            new Object[]{7.4f},
            null,
            null,
            null,
            null,
            null,
            null
    };

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption("h", "help", false, "Display help");
        options.addOption(Option.builder("i").longOpt("input").hasArg().argName("file")
                .desc("Input file with molecules (.csv)").required().build());
        options.addOption(Option.builder("o").longOpt("output").hasArg().argName("file")
                .desc("Output file for molecules (.csv)").build());


        if (args.length == 0 | (args.length == 1 && ("-h".equals(args[0]) | "--help".equals(args[0])))) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(null);
            formatter.printHelp("app", options);
        } else {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            StringBuilder builder = new StringBuilder(SygCalcs.class.getName());
            for (String arg : args) {
                builder.append(" ").append(arg);
            }
            DMLOG.logEvent(DMLogger.Level.INFO, builder.toString());

            SygCalcs exec = new SygCalcs();
            exec.calculate(cmd);
        }
    }

    private void calculate(CommandLine cmd) throws Exception {

        String inputFile = cmd.getOptionValue("input");
        String outputFile = null;
        if (cmd.hasOption("output")) {
            outputFile = cmd.getOptionValue("output");
        }

        calculate(inputFile, outputFile);
    }


    public int[] calculate(String inputFile, String outputFile) throws Exception {
        return calculate(inputFile, outputFile == null ? null : new FileWriter(outputFile));
    }


    /**
     * @param inputFile
     * @param output
     * @return int array of length 2. Fist value is the number of inputs, the second the number of errors
     * @throws Exception
     */
    public int[] calculate(String inputFile, Writer output)
            throws Exception {

        final PKaCalc pka = new PKaCalc();

        // read mols as stream
        Stream<MoleculeObject> mols = MoleculeUtils.readMoleculesAsStream(inputFile, "csv:headless");
        CalculatorsExec exec = new CalculatorsExec();
        Map<String, Integer> stats = new HashMap<>();

        Stream<MoleculeObject> stream = exec.calculate(mols, calcs, options, stats);

        stream = stream.peek(mo -> pka.calcPka(mo, true, true, 2));

        if (output != null) {
            Reporter reporter = new Reporter(output);
            stream = stream.peek(reporter);
        }

        // make sure we consume the stream
        long count = stream.count();
        if (output != null) {
            output.close();
        }
        DMLOG.logEvent(DMLogger.Level.INFO, "Processed " + count + " molecules");
        DMLOG.logCost((float) count, false);
        return new int[]{(int) count, exec.getErrorCount()};
    }

    class Reporter implements Consumer<MoleculeObject> {

        private final Writer out;
        private final AtomicInteger numMols = new AtomicInteger(0);
        private final AtomicInteger numErrors = new AtomicInteger(0);

        private final DecimalFormat df = new DecimalFormat("##.##");

        private static final String HEADER = "Identifier,LogP,LogD7.4,CNS_MPO,Acidic_pKa_1,Acidic_pKa_2,Basic_pKa_1,Basic_pKa_2," +
                "Chiral_centers,Aromatic_rings,fsp3,VAL11,VAL12,VAL13,VAL14,VAL15,InChIKey" +
                "TEXT2,TEXT3,TEXT4,TEXT5\n";

        Reporter(Writer out) {
            this.out = out;
        }

        protected String formatValue(Object value) {
            if (value == null) {
                return "";
            } else if (value instanceof Double) {
                return df.format(value);
            } else {
                return value.toString();
            }
        }

        @Override
        public void accept(MoleculeObject mo) {

            Double cnsmpo = PfizerCNSMPOCalc.calculateScore(
                    (Double)mo.getProperty(ChemTermsCalculator.Calc.LogP.getSymbol()),
                    (Double)mo.getProperty(ChemTermsCalculator.Calc.LogD.getSymbol()),
                    (Double)mo.getProperty(ChemTermsCalculator.Calc.MolecularWeight.getSymbol()),
                    (Double)mo.getProperty(ChemTermsCalculator.Calc.TPSA.getSymbol()),
                    (Integer)mo.getProperty(ChemTermsCalculator.Calc.HBondDonorCount.getSymbol()),
                    // pKa values are already formatted strings
                    mo.getProperty("CXN_BPKA1") == null ? null : Double.valueOf(mo.getProperty("CXN_BPKA1").toString())
            );

            // We have to munge the InCHI key that is returned as it looks like InChIKey=SRBWERXDMQKTOZ-UHFFFAOYSA-N
            Object inchik_orig = mo.getProperty(ChemTermsCalculator.Calc.INCHIK.getSymbol());
            String inchik = inchik_orig == null ? "" : inchik_orig.toString().substring(9);

            List<String> values = new ArrayList<>();
            values.add(formatValue(mo.getMol().getName())); // identifier
            values.add(formatValue(mo.getProperty(ChemTermsCalculator.Calc.LogP.getSymbol())));
            values.add(formatValue(mo.getProperty(ChemTermsCalculator.Calc.LogD.getSymbol())));
            values.add(formatValue(cnsmpo));
            values.add(formatValue(mo.getProperty("CXN_APKA1")));
            values.add(formatValue(mo.getProperty("CXN_APKA2")));
            values.add(formatValue(mo.getProperty("CXN_BPKA1")));
            values.add(formatValue(mo.getProperty("CXN_BPKA2")));
            values.add(formatValue(mo.getProperty(ChemTermsCalculator.Calc.CHIRALC.getSymbol())));
            values.add(formatValue(mo.getProperty(ChemTermsCalculator.Calc.AromaticRingCount.getSymbol())));
            values.add(formatValue(mo.getProperty(ChemTermsCalculator.Calc.FSP3.getSymbol())));
            values.add(""); // VAL
            values.add(""); // VAL
            values.add(""); // VAL
            values.add(""); // VAL
            values.add(""); // VAL
            values.add(formatValue(inchik));
            values.add(""); // TEXT
            values.add(""); // TEXT
            values.add(""); // TEXT
            values.add(""); // TEXT


            String line = String.join(",", values);
            try {
                if (numMols.get() == 0) {
                    out.write(HEADER);
                }
                numMols.incrementAndGet();
                out.write(line + "\n");
            } catch (IOException e) {
                numErrors.incrementAndGet();
                DMLOG.logEvent(DMLogger.Level.WARNING, "Failed to write line");
            }
        }
    }
}
