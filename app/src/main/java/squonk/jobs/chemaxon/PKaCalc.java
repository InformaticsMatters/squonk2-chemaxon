package squonk.jobs.chemaxon;

import org.apache.commons.cli.*;
import squonk.jobs.chemaxon.util.ChemTermsCalculator;
import squonk.jobs.chemaxon.util.DMLogger;
import squonk.jobs.chemaxon.util.MoleculeObject;
import squonk.jobs.chemaxon.util.MoleculeUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class PKaCalc {

    private static final Logger LOG = Logger.getLogger(PKaCalc.class.getName());
    private static final DMLogger DMLOG = new DMLogger();

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption("h", "help", false, "Display help");
        options.addOption(Option.builder("i").longOpt("input").hasArg().argName("file")
                .desc("Input file with molecules (.sdf)").required().build());
        options.addOption(Option.builder("o").longOpt("output").hasArg().argName("file")
                .desc("Output file for molecules (.sdf)").build());
        options.addOption(Option.builder("a").longOpt("acidic").hasArg(false).argName("value").type(Boolean.class)
                .desc("Calculate acidic pKa").build());
        options.addOption(Option.builder("b").longOpt("basic").hasArg(false).argName("value").type(Boolean.class)
                .desc("Calculate basic pKa").build());

        options.addOption(Option.builder(null).longOpt("header").hasArg().argName("true/false")
                .desc("Include header line when writing SMILES").type(Boolean.class).build());

        if (args.length == 0 | (args.length == 1 && ("-h".equals(args[0]) | "--help".equals(args[0])))) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(null);
            formatter.printHelp("app", options);
        } else {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            StringBuilder builder = new StringBuilder(PKaCalc.class.getName());
            for (String arg : args) {
                builder.append(" ").append(arg);
            }
            DMLOG.logEvent(DMLogger.Level.INFO, builder.toString());

            String inputFile = cmd.getOptionValue("input");
            String outputFile = cmd.getOptionValue("output");
            boolean acidic = cmd.hasOption("acidic");
            boolean basic = cmd.hasOption("basic");
            System.out.println(acidic + " " + basic);

            if (!acidic && !basic) {
                throw new IllegalArgumentException("Must specify at least one of acidic and basic options");
            }

            boolean header = Boolean.valueOf(cmd.getOptionValue("header", "true"));

            PKaCalc calc = new PKaCalc();
            calc.calculate(inputFile, outputFile, header, acidic, basic);
        }
    }

    public long calculate(String inputFile, String outputFile, boolean includeHeader, boolean acidic, boolean basic) throws IOException {
        // read mols as stream
        Stream<MoleculeObject> mols = MoleculeUtils.readMoleculesAsStream(inputFile);
        CalculatorsExec exec = new CalculatorsExec();
        Map<String, Integer> stats = new HashMap<>();

        ChemTermsCalculator[] calculators = null;
        if (acidic && basic) {
            calculators = exec.createCalculators(
                    new ChemTermsCalculator.Calc[]{
                            ChemTermsCalculator.Calc.AcidicPKa, ChemTermsCalculator.Calc.BasicPKa},
                    null);
        } else if (acidic) {
            calculators = exec.createCalculators(
                    new ChemTermsCalculator.Calc[]{ChemTermsCalculator.Calc.AcidicPKa},null);
        } else if (basic) {
            calculators = exec.createCalculators(
                    new ChemTermsCalculator.Calc[]{ChemTermsCalculator.Calc.BasicPKa}, null);
        } else {
            throw new IllegalArgumentException("Must specify at least one of acidic and basic options");
        }

        Stream<MoleculeObject> str = exec.calculate(mols, calculators, stats);

        // we need to count the actual molecules calculated as the final number may be filtered
        final AtomicInteger total = new AtomicInteger(0);
        str = str.peek(mo -> total.incrementAndGet());

        // if output is defined then set up a MolExporter to write the results
        if (outputFile != null) {
            str = MoleculeUtils.addFileWriter(str, outputFile, includeHeader);
        }

        // make sure we consume the stream
        long count = str.count();
        DMLOG.logEvent(DMLogger.Level.INFO, "Processed " + count + " molecules");
        DMLOG.logCost((float) count, false);
        return count;
    }
}
