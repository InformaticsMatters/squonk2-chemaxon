package squonk.jobs.chemaxon;

import chemaxon.marvin.calculations.pKaPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.struc.Molecule;
import org.apache.commons.cli.*;
import squonk.jobs.chemaxon.util.ChemTermsCalculator;
import squonk.jobs.chemaxon.util.DMLogger;
import squonk.jobs.chemaxon.util.MoleculeObject;
import squonk.jobs.chemaxon.util.MoleculeUtils;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class PKaCalc {

    private static final Logger LOG = Logger.getLogger(PKaCalc.class.getName());
    private static final DMLogger DMLOG = new DMLogger();

    private static final DecimalFormat df = new DecimalFormat("0.0");

    private static final double DEFAULT_TEMPERATURE = 298d;
    private static final double DEFAULT_MIN_BASIC_VALUE = -2d;
    private static final double DEFAULT_MIN_ACIDIC_VALUE = 16d;
    private static final int DEFAULT_MAX_IONS = 6;

    private final pKaPlugin plugin;

    public PKaCalc() {
        this(DEFAULT_TEMPERATURE, DEFAULT_MIN_BASIC_VALUE, DEFAULT_MIN_ACIDIC_VALUE, DEFAULT_MAX_IONS);
    }
    public PKaCalc(double temperature_k, double minBasic, double maxAcidic, int maxIons) {
        plugin = new pKaPlugin();
        plugin.setTemperature(temperature_k);
        plugin.setMaxIons(maxIons);
        plugin.setBasicpKaLowerLimit(minBasic);
        plugin.setAcidicpKaUpperLimit(maxAcidic);
    }

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption("h", "help", false, "Display help");
        options.addOption(Option.builder("i").longOpt("input").hasArg().argName("file")
                .desc("Input file with molecules (.sdf)").required().build());
        options.addOption(Option.builder("o").longOpt("output").hasArg().argName("file")
                .desc("Output file for molecules (.sdf)").build());
        options.addOption(Option.builder("a").longOpt("acidic").hasArg(false).argName("true/false").type(Boolean.class)
                .desc("Calculate acidic pKa").build());
        options.addOption(Option.builder("b").longOpt("basic").hasArg(false).argName("true/false").type(Boolean.class)
                .desc("Calculate basic pKa").build());
        options.addOption(Option.builder("c").longOpt("count").hasArg().argName("number").type(Integer.class)
                .desc("Number of values to calculate (default 3)").build());
        options.addOption(Option.builder(null).longOpt("min-basic").hasArg().argName("value").type(Double.class)
                .desc("Minimum basic pKa limit (default -2)").build());
        options.addOption(Option.builder(null).longOpt("max-acidic").hasArg().argName("value").type(Double.class)
                .desc("Maximum acidic pKa limit (default 16)").build());
        options.addOption(Option.builder(null).longOpt("max-ions").hasArg().argName("value").type(Integer.class)
                .desc("Maximum number of ions (default 6)").build());
        options.addOption(Option.builder("t").longOpt("temperature").hasArg().argName("value").type(Double.class)
                .desc("Temperature (default 298K)").build());

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

            if (!acidic && !basic) {
                throw new IllegalArgumentException("Must specify at least one of acidic and basic options. "
                        + cmd.getOptionValue("acidic") + cmd.getOptionValue("basic"));
            }

            double minBasic = Double.valueOf(cmd.getOptionValue("min-basic", ""+DEFAULT_MIN_BASIC_VALUE));
            double maxAcidic = Double.valueOf(cmd.getOptionValue("max-acidic", ""+DEFAULT_MIN_ACIDIC_VALUE));
            double temperature_k = Double.valueOf(cmd.getOptionValue("temperature", ""+DEFAULT_TEMPERATURE));
            int maxIons = Integer.valueOf(cmd.getOptionValue("max-ions", ""+DEFAULT_MAX_IONS));
            int count = Integer.valueOf(cmd.getOptionValue("count", "3"));

            boolean header = Boolean.valueOf(cmd.getOptionValue("header", "false"));

            PKaCalc calc = new PKaCalc(temperature_k, minBasic, maxAcidic, maxIons);
            calc.calculate(inputFile, outputFile, header, acidic, basic, count);
        }
    }

    public long calculate(String inputFile, String outputFile, boolean includeHeader, boolean acidic, boolean basic, int numStrengths)
            throws IOException {

        if (numStrengths < 1 || numStrengths > 5) {
            throw new IllegalArgumentException("Number of pKa values to calculate must be between 1 and 5 (inclusive)");
        }

        // read mols as stream
        Stream<MoleculeObject> mols = MoleculeUtils.readMoleculesAsStream(inputFile);

        mols = mols.peek(mo -> {
            calcPka(mo, acidic, basic, numStrengths);
        });

        // we need to count the actual molecules calculated as the final number may be filtered
        final AtomicInteger total = new AtomicInteger(0);
        mols = mols.peek(mo -> total.incrementAndGet());

        // if output is defined then set up a MolExporter to write the results
        if (outputFile != null) {
            mols = MoleculeUtils.addFileWriter(mols, outputFile, includeHeader);
        }

        // make sure we consume the stream
        long count = mols.count();
        DMLOG.logEvent(DMLogger.Level.INFO, "Processed " + count + " molecules");
        DMLOG.logCost((float) count, false);
        return count;
    }


    protected void calcPka(MoleculeObject mo, boolean acidic, boolean basic, int count) {

        try {
            plugin.setMolecule(mo.getMol());
            plugin.run();

            if (acidic) {
                StringBuffer acidicSummary = new StringBuffer();
                double[] acidicpKas = new double[count];
                int[] acidicIndexes = new int[count];
                plugin.getMacropKaValues(pKaPlugin.ACIDIC, acidicpKas, acidicIndexes);
                for (int i = 0; i < count; i++) {
                    if (acidicIndexes[i] >= 0) {
                        mo.setProperty("CXN_APKA" + (i + 1), df.format(acidicpKas[i]));
                        if (i > 0) {
                            acidicSummary.append("\n");
                        }
                        acidicSummary.append(acidicIndexes[i]).append(" ").append(df.format(acidicpKas[i]));
                    }
                }
                String s = acidicSummary.toString();
                if (s.length() > 0) {
                    mo.setProperty("CXN_APKA", s);
                }
            }
            if (basic) {
                StringBuffer basicSummary = new StringBuffer();
                double[] basicpKas = new double[count];
                int[] basicIndexes = new int[count];
                plugin.getMacropKaValues(pKaPlugin.BASIC, basicpKas, basicIndexes);
                for (int i = 0; i < count; i++) {
                    if (basicIndexes[i] >= 0) {
                        mo.setProperty("CXN_BPKA" + (i + 1), df.format(basicpKas[i]));
                        if (i > 0) {
                            basicSummary.append("\n");
                        }
                        basicSummary.append(basicIndexes[i]).append(" ").append(df.format(basicpKas[i]));
                    }
                }
                String s = basicSummary.toString();
                if (s.length() > 0) {
                    mo.setProperty("CXN_BPKA", s);
                }
            }
        } catch (PluginException pe) {
            LOG.log(Level.INFO, "Failed to calculate pKa", pe);
        }
    }
}
