package squonk.jobs.chemaxon;

import chemaxon.formats.MolExporter;
import org.apache.commons.cli.*;
import squonk.jobs.chemaxon.util.ChemTermsCalculator;
import squonk.jobs.chemaxon.util.DMLogger;
import squonk.jobs.chemaxon.util.MoleculeObject;
import squonk.jobs.chemaxon.util.MoleculeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
        options.addOption(null, "atomCount", false, "Calculate atom count");
        options.addOption(null, "bondCount", false, "Calculate bond count");
        options.addOption(null, "logp", false, "Calculate cLogP");
        options.addOption(null, "logd", false, "Calculate cLogD at pH 7.4");
        options.addOption(null, "hba", false, "Calculate H-bond acceptors");
        options.addOption(null, "hbd", false, "Calculate H-bond donors");
        options.addOption(Option.builder("h").longOpt("header").hasArg().argName("true/false")
                .desc("Include header line when writing SMILES").type(Boolean.class).build());

        options.addOption(Option.builder(null).longOpt("addhs").hasArg().argName("addhs").desc("Include hydrogens in output (true/false). If not specified then no changes made").build());

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

            if (cmd.hasOption("atomCount")) {
                calcs.add(ChemTermsCalculator.Calc.AtomCount);
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
            if (cmd.hasOption("hba")) {
                calcs.add(ChemTermsCalculator.Calc.HBondAcceptorCount);
            }
            if (cmd.hasOption("hbd")) {
                calcs.add(ChemTermsCalculator.Calc.HBondDonorCount);
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
