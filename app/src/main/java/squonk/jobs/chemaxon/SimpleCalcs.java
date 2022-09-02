package squonk.jobs.chemaxon;

import org.apache.commons.cli.*;
import squonk.jobs.chemaxon.util.ChemTermsCalculator;
import squonk.jobs.chemaxon.util.DMLogger;
import squonk.jobs.chemaxon.util.MoleculeObject;
import squonk.jobs.chemaxon.util.MoleculeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SimpleCalcs {


    private static final Logger LOG = Logger.getLogger(SimpleCalcs.class.getName());
    private static final DMLogger DMLOG = new DMLogger();

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption("h", "help", false, "Display help");
        options.addOption(Option.builder("i").longOpt("input").hasArg().argName("file")
                .desc("Input file with molecules (.sdf)").required().build());
        options.addOption(Option.builder("o").longOpt("output").hasArg().argName("file")
                .desc("Output file for molecules (.sdf)").build());
        options.addOption("a", "all", false, "Calculate all descriptors");
        options.addOption(null, "molecularWeight", false, "Calculate molecular weight");
        options.addOption(null, "molecularFormula", false, "Calculate molecular formula");
        options.addOption(null, "atomCount", false, "Calculate atom count");
        options.addOption(null, "heavyAtomCount", false, "Calculate heavy atom count");
        options.addOption(null, "bondCount", false, "Calculate bond count");
        options.addOption(null, "logp", false, "Calculate cLogP");
        options.addOption(null, "logd", false, "Calculate cLogD at pH 7.4");
        options.addOption(null, "hba-count", false, "Calculate H-bond acceptor count");
        options.addOption(null, "hbd-count", false, "Calculate H-bond donor count");
        options.addOption(null, "hba-sites", false, "Calculate H-bond acceptor sites");
        options.addOption(null, "hbd-sites", false, "Calculate H-bond donor sites");
        options.addOption(null, "ringCount", false, "Calculate ring count");
        options.addOption(null, "ringAtomCount", false, "Calculate ring atom count");
        options.addOption(null, "aromaticRingCount", false, "Calculate aromatic ring count");
        options.addOption(null, "aromaticAtomCount", false, "Calculate aromatic atom count");
        options.addOption(null, "rotatableBondCount", false, "Calculate rotatable bond count");
        options.addOption(null, "tpsa", false, "Calculate topological polar surface area");

        options.addOption(Option.builder("h").longOpt("header").hasArg().argName("true/false")
                .desc("Include header line when writing SMILES").type(Boolean.class).build());

        if (args.length == 0 | (args.length == 1 && ("-h".equals(args[0]) | "--help".equals(args[0])))) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(null);
            formatter.printHelp("app", options);
        } else {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            StringBuilder builder = new StringBuilder(SimpleCalcs.class.getName());
            for (String arg : args) {
                builder.append(" ").append(arg);
            }
            DMLOG.logEvent(DMLogger.Level.INFO, builder.toString());

            SimpleCalcs exec = new SimpleCalcs();
            exec.calculate(cmd);
        }
    }

    private void calculate(CommandLine cmd) throws Exception {

        String inputFile = cmd.getOptionValue("input");
        String outputFile = null;
        if (cmd.hasOption("output")) {
            outputFile = cmd.getOptionValue("output");
        }
        boolean header = Boolean.valueOf(cmd.getOptionValue("header", "true"));

        if (cmd.hasOption("all")) {
            calculate(ChemTermsCalculator.Calc.values(), inputFile, outputFile, header);
        } else {
            List<ChemTermsCalculator.Calc> calcs = new ArrayList<>();

            if (cmd.hasOption("molecularWeight")) {
                calcs.add(ChemTermsCalculator.Calc.MolecularWeight);
            }
            if (cmd.hasOption("molecularFormula")) {
                calcs.add(ChemTermsCalculator.Calc.MolecularFormula);
            }
            if (cmd.hasOption("atomCount")) {
                calcs.add(ChemTermsCalculator.Calc.AtomCount);
            }
            if (cmd.hasOption("heavyAtomCount")) {
                calcs.add(ChemTermsCalculator.Calc.HeavyAtomCount);
            }
            if (cmd.hasOption("bondCount")) {
                calcs.add(ChemTermsCalculator.Calc.BondCount);
            }
            if (cmd.hasOption("logp")) {
                calcs.add(ChemTermsCalculator.Calc.LogP);
            }
            if (cmd.hasOption("logd")) {
                calcs.add(ChemTermsCalculator.Calc.LogD);
            }
            if (cmd.hasOption("hba-count")) {
                calcs.add(ChemTermsCalculator.Calc.HBondAcceptorCount);
            }
            if (cmd.hasOption("hbd-count")) {
                calcs.add(ChemTermsCalculator.Calc.HBondDonorCount);
            }
            if (cmd.hasOption("hba-sites")) {
                calcs.add(ChemTermsCalculator.Calc.HBondAcceptorSites);
            }
            if (cmd.hasOption("hbd-sites")) {
                calcs.add(ChemTermsCalculator.Calc.HBondDonorSites);
            }
            if (cmd.hasOption("ringCount")) {
                calcs.add(ChemTermsCalculator.Calc.RingCount);
            }
            if (cmd.hasOption("ringAtomCount")) {
                calcs.add(ChemTermsCalculator.Calc.RingAtomCount);
            }
            if (cmd.hasOption("aromaticRingCount")) {
                calcs.add(ChemTermsCalculator.Calc.AromaticRingCount);
            }
            if (cmd.hasOption("aromaticAtomCount")) {
                calcs.add(ChemTermsCalculator.Calc.AromaticAtomCount);
            }
            if (cmd.hasOption("rotatableBondCount")) {
                calcs.add(ChemTermsCalculator.Calc.RotatableBondCount);
            }
            if (cmd.hasOption("tpsa")) {
                calcs.add(ChemTermsCalculator.Calc.TPSA);
            }
//            if (cmd.hasOption("")) {
//                calcs.add(ChemTermsCalculator.Calc.);
//            }

            calculate(calcs.toArray(new ChemTermsCalculator.Calc[calcs.size()]), inputFile, outputFile, header);
        }
    }

    public long calculate(ChemTermsCalculator.Calc calcs[], String inputFile, String outputFile, boolean includeHeader) throws Exception {

        // read mols as stream
        Stream<MoleculeObject> mols = MoleculeUtils.readMoleculesAsStream(inputFile);
        CalculatorsExec exec = new CalculatorsExec();
        Map<String,Integer> stats = new HashMap<>();

        Stream<MoleculeObject> str = exec.calculate(mols, calcs, null, stats);

        if (outputFile != null) {
            str = MoleculeUtils.addFileWriter(str, outputFile, includeHeader);
        }

        // make sure we consume the stream
        long count = str.count();
        DMLOG.logEvent(DMLogger.Level.INFO, "Processed " + count + " molecules");
        DMLOG.logCost((float)count, false);
        return count;
    }
}
