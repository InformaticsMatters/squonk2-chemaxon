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
        options.addOption(Option.builder("n").longOpt("minvalue").hasArg().argName("logD").type(Float.class)
                .desc("Minimum value for filter").build());
        options.addOption(Option.builder("x").longOpt("maxvalue").hasArg().argName("logD").type(Float.class)
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
            Float ph = Float.valueOf(cmd.getOptionValue("ph"));
            Float minValue = cmd.hasOption("minValue") ? Float.valueOf(cmd.getOptionValue("minValue")) : null;
            Float maxValue = cmd.hasOption("maxValue") ? Float.valueOf(cmd.getOptionValue("maxValue")) : null;
            boolean header = Boolean.valueOf(cmd.getOptionValue("header", "true"));

            LogDCalc calc = new LogDCalc();
            calc.calculate(inputFile, outputFile, header, ph, minValue, maxValue);
        }
    }

    public long calculate(String inputFile, String outputFile, boolean includeHeader, Float ph, Float minValue, Float maxValue) throws IOException {
        // read mols as stream
        Stream<MoleculeObject> mols = MoleculeUtils.readMoleculesAsStream(inputFile);
        CalculatorsExec exec = new CalculatorsExec();
        Map<String, Integer> stats = new HashMap<>();
        Object[] params = new Object[]{ph};

        ChemTermsCalculator[] calculators = exec.createCalculators(
                new ChemTermsCalculator.Calc[]{ChemTermsCalculator.Calc.LogD},
                new Object[][]{params});

        Stream<MoleculeObject> str = exec.calculate(mols, calculators, stats);

        // apply the filters
        if (minValue != null) {
            str = str.filter(mo -> {
                Double value = (Double) (mo.getProperty(calculators[0].getPropName()));
                return (value != null && value >= minValue);
            });
        }
        if (maxValue != null) {
            str = str.filter(mo -> {
                Double value = (Double) (mo.getProperty(calculators[0].getPropName()));
                return (value != null && value >= maxValue);
            });
        }

        // if output is defined then set up a MolExporter to write the results
        if (outputFile != null) {
            MoleculeUtils.addFileWriter(str, outputFile, includeHeader);
        }

        // make sure we consume the stream
        long count = str.count();
        DMLOG.logEvent(DMLogger.Level.INFO, "Processed " + count + " molecules");
        DMLOG.logCost((float) count, false);
        return count;
    }
}
