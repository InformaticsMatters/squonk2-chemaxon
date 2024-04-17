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
 * Calculates BBB MPS score as defined in Gupta et al., J. Med. Chem. 2019, 62, 9824−9836.
 * DOI: 10.1021/acs.jmedchem.9b01220
 */
public class GuptaBBBCalc implements Calculator {

    private final ChemTermsCalculator[] calculators;

    public GuptaBBBCalc() {
        final CalculatorsExec exec = new CalculatorsExec();

        this.calculators = exec.createCalculators(
                new ChemTermsCalculator.Calc[]{
                        ChemTermsCalculator.Calc.AromaticRingCount,
                        ChemTermsCalculator.Calc.HeavyAtomCount,
                        ChemTermsCalculator.Calc.HBondAcceptorCount,
                        ChemTermsCalculator.Calc.HBondDonorCount,
                        ChemTermsCalculator.Calc.MolecularWeight,
                        ChemTermsCalculator.Calc.TPSA,
                        ChemTermsCalculator.Calc.RotatableBondCount,
                        ChemTermsCalculator.Calc.AcidicPKa,
                        ChemTermsCalculator.Calc.BasicPKa
                },
                null);
    }

    private static final Logger LOG = Logger.getLogger(GuptaBBBCalc.class.getName());
    private static final DMLogger DMLOG = new DMLogger();

    public static final String SCORE_FIELD = "Gupta_BBB";


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

            GuptaBBBCalc calc = new GuptaBBBCalc();
            calc.calculateBBB(inputFile, outputFile, header, filterMode, minValue, maxValue);
        }
    }

    public int[] calculateBBB(String inputFile, String outputFile, boolean includeHeader, FilterMode mode,
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

        // this does the calculations that are used to generate the BBB score
        Integer aro = (Integer)calculators[0].processMolecule(mol, stats);
        Number hac_num = (Number)calculators[1].processMolecule(mol, stats);
        Integer hac = hac_num == null ? null : hac_num.intValue();
        Integer hba = (Integer)calculators[2].processMolecule(mol, stats);
        Integer hbd = (Integer)calculators[3].processMolecule(mol, stats);
        Double mw = (Double)calculators[4].processMolecule(mol, stats);
        Double tpsa = (Double)calculators[5].processMolecule(mol, stats);
        Integer rot = (Integer)calculators[6].processMolecule(mol, stats);
        Double apka = (Double)calculators[7].processMolecule(mol, stats);
        Double bpka = (Double)calculators[8].processMolecule(mol, stats);

        if (mw == null || tpsa == null || aro == null || hac == null || hba == null || hbd == null || rot == null) {
            LOG.info(String.format("Data missing. Inputs apka=%s bpka=%s mw=%s tpsa=%s aro=%s hac=%s hba=%s hbd=%s rot=%s", apka, bpka, mw, tpsa, aro, hac, hba, hbd, rot));
            return null;
        }

        LOG.finer(String.format("Inputs are: apka=%s, bpka=%s, mw=%s, tpsa=%s, aro=%s, hac=%s, hba=%s, hbd=%s, rot=%s",
                apka, bpka, mw, tpsa, aro, hac, hba, hbd, rot));

        double pka = findCorrectPKa(apka, bpka);

        mw = Utils.roundToSignificantFigures(mw, 4);
        tpsa = Utils.roundToSignificantFigures(tpsa, 4);
        pka = Utils.roundToSignificantFigures(pka, 4);

        double score_aro = calculateAROScore(aro);
        double score_hac = calculateHACScore(hac);
        double mwhbn = calculateMWHBN(mw, hbd, hba);
        double score_mwhbn = calculateMWHBNScore(mwhbn);
        double score_tpsa = calculateTPSAScore(tpsa);
        double score_pka = calculatePKAScore(pka);

        double score_mps = Utils.roundToSignificantFigures(
                score_aro + score_hac + (1.5d * score_mwhbn) + (2d * score_tpsa) + (0.5d * score_pka), 4);
        LOG.fine(String.format("Scores are: aro=%s, hac=%s, mwhbn=%s, tpsa=%s, pka=%s, bbb=%s",
                score_aro, score_hac, score_mwhbn, score_tpsa, score_pka, score_mps));

        mo.setProperty(SCORE_FIELD, score_mps);

        return score_mps;
    }


    protected double findCorrectPKa(Double apka, Double bpka) {

        /*
        1.) If the molecule has basic pKa's ≥ 5, take the most basic pKa
        2.) If molecule has no basic pKa, take the most acidic pKa ≤ 9
        3.) If molecule has neither (neutral molecule), take the pKa that
        would give the maximal score for pKa descriptor (pKa=0 for MPO,
        pKa = 8.81 for BBB)
         */

        if (bpka != null && bpka >= 5d) {
            return bpka;
        }
        // a check if bpka was null was removed from the next line at Sygnature's request
        if (apka != null && apka <= 9d) {
            return apka;
        }
        return 8.81d;
    }

    /**
     * Score component for aromatic ring count
     *
     * @param aro
     * @return
     */
    protected double calculateAROScore(int aro) {
        switch (aro) {
            case 0:
                return 0.336376d;
            case 1:
                return 0.816016d;
            case 2:
                return 1d;
            case 3:
                return 0.691115d;
            case 4:
                return 0.199399d;
            default:
                return 0d;
        }
    }

    /**
     * Score component for heavy atom count
     *
     * @param hac
     * @return
     */
    protected double calculateHACScore(int hac) {
        if (hac > 5 && hac <= 45) {
            return (
                    (0.0000443d * Math.pow(hac, 3d))
                            - (0.004556d * Math.pow(hac, 2d))
                            + (0.12775d * (double)hac)
                            - 0.463d
            ) / 0.624231d;
        } else {
            return 0d;
        }
    }

    /**
     * Generate the MWHBM value which is needed in order to calculate the MWHBN score
     *
     * @param mw
     * @param hbd
     * @param hba
     * @return
     */
    protected double calculateMWHBN(double mw, int hbd, int hba) {
        return (double)(hbd + hba) / Math.sqrt(mw);
    }

    /**
     * Score component for MWHBM
     *
     * @param mwhbn
     * @return
     */
    protected double calculateMWHBNScore(double mwhbn) {
        if (mwhbn > 0.05d && mwhbn <= 0.45d) {
            return (
                    (26.733d * Math.pow(mwhbn, 3d))
                            - (31.495d * Math.pow(mwhbn, 2d))
                            + (9.5202d * mwhbn)
                            - 0.1358d
            ) / 0.72258d;
        } else {
            return 0d;
        }
    }

    /**
     * Score component for TPSA
     *
     * @param tpsa
     * @return
     */
    protected double calculateTPSAScore(double tpsa) {
        if (tpsa > 0d && tpsa <= 120d) {
            return ((-0.0067d * tpsa) + 0.9598d) / 0.9598d;
        } else {
            return 0d;
        }
    }

    /**
     * Score component for pKa
     *
     * @param pka
     * @return
     */
    protected double calculatePKAScore(double pka) {
        if (pka > 3d && pka <= 11d) {
            return (
                    (0.00045068d * Math.pow(pka, 4d))
                            - (0.016331d * Math.pow(pka, 3d))
                            + (0.18618d * Math.pow(pka, 2d))
                            - (0.71043d * pka)
                            + 0.8579d
            ) / 0.597488d;
        } else {
            return 0d;
        }
    }

}
