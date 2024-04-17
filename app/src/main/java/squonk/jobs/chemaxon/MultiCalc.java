/*
 * Copyright (c) 2024 Informatics Matters Ltd.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class MultiCalc {

    private static final Logger LOG = Logger.getLogger(SimpleCalcs.class.getName());
    private static final DMLogger DMLOG = new DMLogger();

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption("h", "help", false, "Display help");
        options.addOption(Option.builder("i").longOpt("input").hasArg().argName("file")
                .desc("Input file with molecules (.sdf)").required().build());
        options.addOption(Option.builder("o").longOpt("output").hasArg().argName("file")
                .desc("Output file for molecules (.sdf)").build());

        options.addOption(Option.builder("c").longOpt("calculators").hasArg().argName("calc")
                .desc("Calculators to run").build());

        options.addOption(Option.builder("h").longOpt("header").hasArg().argName("true/false")
                .desc("Include header line when writing SMILES").type(Boolean.class).build());

        if (args.length == 0 | (args.length == 1 && ("-h".equals(args[0]) | "--help".equals(args[0])))) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(null);
            formatter.printHelp("app", options);
        } else {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            StringBuilder builder = new StringBuilder(MultiCalc.class.getName());
            for (String arg : args) {
                builder.append(" ").append(arg);
            }
            DMLOG.logEvent(DMLogger.Level.INFO, builder.toString());

            MultiCalc calcs = new MultiCalc();
            calcs.calculate(cmd);
        }
    }

    private void calculate(CommandLine cmd) throws Exception {

        String calcOption = cmd.getOptionValue("calculators", "");
        if (calcOption.isBlank()) {
            DMLOG.logEvent(DMLogger.Level.WARNING, "No calculators specified");
            return;
        }

        String inputFile = cmd.getOptionValue("input");
        String outputFile = null;
        if (cmd.hasOption("output")) {
            outputFile = cmd.getOptionValue("output");
        }
        boolean header = Boolean.valueOf(cmd.getOptionValue("header", "true"));

        String[] tokens = calcOption.trim().split(" ");
        calculate(tokens, inputFile, outputFile, header);
    }

    public int[] calculate(String[] calculatorNames, String inputFile, String outputFile, boolean includeHeader) throws Exception {

        List<Calculator> calcs = new ArrayList<>();

        for (String token : calculatorNames) {

            switch (token.strip().toLowerCase()) {
                case "molecular-weight":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.MolecularWeight));
                    break;
                case "molecular-formula":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.MolecularFormula));
                    break;
                case "atom-count":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.AtomCount));
                    break;
                case "heavy-atom-count":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.HeavyAtomCount));
                    break;
                case "bond-count":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.BondCount));
                    break;
                case "logp":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.LogP));
                    break;
                case "logd":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.LogD));
                    break;
                case "hba-count":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.HBondAcceptorCount));
                    break;
                case "hbd-count":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.HBondDonorCount));
                    break;
                case "hba-sites":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.HBondAcceptorSites));
                    break;
                case "hbd-sites":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.HBondDonorSites));
                    break;
                case "ring-count":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.RingCount));
                    break;
                case "ring-atom-count":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.RingAtomCount));
                    break;
                case "aromatic-ring-count":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.AromaticRingCount));
                    break;
                case "aromatic-atom-count":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.AromaticAtomCount));
                    break;
                case "rotatable-bond-count":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.RotatableBondCount));
                    break;
                case "tpsa":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.TPSA));
                    break;
                case "acidic-pka":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.AcidicPKa));
                    break;
                case "basic-pka":
                    calcs.add(ChemTermsCalculator.create(ChemTermsCalculator.Calc.BasicPKa));
                    break;
                case "gupta-bbb":
                    calcs.add(new GuptaBBBCalc());
                    break;
                case "pfizer-cns-mpo":
                    calcs.add(new PfizerCNSMPOCalc());
                    break;
                case "abbvie-mps":
                    calcs.add(new AbbvieMPSCalc());
                    break;
                case "kids-mpo":
                    calcs.add(new KidsMPOCalc());
                    break;
                default:
                    DMLOG.logEvent(DMLogger.Level.WARNING, "Invalid calculator specified: " + token);
                    break;
            }
        }
        return calculate(calcs, inputFile, outputFile, includeHeader);
    }

    /**
     * @param calculators
     * @param inputFile
     * @param outputFile
     * @param includeHeader
     * @return int array of length 2. Fist value is the number of inputs, the second the number of errors
     * @throws Exception
     */
    public int[] calculate(List<Calculator> calculators, String inputFile, String outputFile,
                           boolean includeHeader) throws
            Exception {
//        String opts = null;
//        if (inputFile.endsWith(".csv")) {
//            opts = "csv:headless,struc0";
//        }
        // read mols as stream
        Stream<MoleculeObject> mols = MoleculeUtils.readMoleculesAsStream(inputFile);
        Map<String, Integer> stats = new HashMap<>();

        AtomicInteger errorCount = new AtomicInteger(0);
        mols = mols.peek(mo -> {
            if (mo == null) {
                errorCount.incrementAndGet();
            } else {
                for (Calculator calc : calculators) {
                    calc.calculate(mo, stats);
                }
            }
        });

        if (outputFile != null) {
            mols = MoleculeUtils.addFileWriter(mols, outputFile, includeHeader);
        }

        // make sure we consume the stream
        long count = mols.count();
        DMLOG.logEvent(DMLogger.Level.INFO, "Processed " + count + " molecules");
        DMLOG.logCost((float) count, false);
        return new int[]{(int) count, errorCount.intValue()};
    }
}

