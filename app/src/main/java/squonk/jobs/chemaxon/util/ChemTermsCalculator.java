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

package squonk.jobs.chemaxon.util;

import chemaxon.jep.ChemJEP;
import chemaxon.jep.Evaluator;
import chemaxon.jep.context.MolContext;
import chemaxon.nfunk.jep.ParseException;
import chemaxon.struc.Molecule;


import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author timbo
 */
public class ChemTermsCalculator implements Calculator {

    private static final Logger LOG = Logger.getLogger(ChemTermsCalculator.class.getName());


    public enum Calc {
        MolecularWeight("CXN_molecularWeight", "mass()", Double.class),
        MolecularFormula("CXN_molecularFormula", "formula()", String.class),
        AtomCount("CXN_atomCount", "atomCount()", Integer.class),
        ElementCount("CXN_elementCount", "atomCount(''{0}'')", Integer.class, new Class[]{Integer.class}, new Object[]{1}),
        HeavyAtomCount("CXN_heavyAtomCount", "atomCount() - atomCount('1')", Double.class),
        BondCount("CXN_bondCount", "bondCount()", Integer.class),
        LogP("CXN_cLogP", "logP()", Double.class),
        // single quote has to be represented as 2 single quotes as the expansion uses MessageFormat
        LogD("CXN_logD", "logD(''{0}'')", Double.class, new Class[]{Float.class}, new Object[]{7.4f}),
        HBondDonorCount("CXN_donorCount", "donorCount()", Integer.class),
        HBondAcceptorCount("CXN_acceptorCount", "acceptorCount()", Integer.class),
        HBondDonorSites("CXN_donorSites", "donorSiteCount()", Integer.class),
        HBondAcceptorSites("CXN_acceptorSites", "acceptorSiteCount()", Integer.class),
        RingCount("CXN_ringCount", "ringCount()", Integer.class),
        RingAtomCount("CXN_ringAtomCount", "ringAtomCount()", Integer.class),
        AromaticRingCount("CXN_aromaticRingCount", "aromaticRingCount()", Integer.class),
        AromaticAtomCount("CXN_aromaticAtomCount", "aromaticAtomCount()", Integer.class),
        RotatableBondCount("CXN_rotatableBondCount", "rotatableBondCount()", Integer.class),
        AcidicPKa("CXN_aPKa", "acidicpKa(''{0}'')", Double.class, new Class[]{Integer.class}, new Object[]{1}),
        BasicPKa("CXN_bPKa", "basicpKa(''{0}'')", Double.class, new Class[]{Integer.class}, new Object[]{1}),
        TPSA("CXN_tpsa", "topologicalPolarSurfaceArea()", Double.class),
        FSP3("CXN_fsp3", "fsp3()", Double.class),
        INCHIS("CXN_inchis", "molString('inchi')", String.class),
        INCHIK("CXN_inchik", "molString('inchikey')", String.class),
        CHIRALC("CXN_chiral_centers", "chiralCenterCount()", Integer.class),
//        ("CXN_", "()", .class),

        ;

        private String symbol;
        private String chemTermsExpr;
        private Class resultType;
        private String defaultPropName;
        Class[] paramTypes;
        Object[] defaultParams;

        Calc(String symbol, String chemTermsExpr, Class resultType) {
            this(symbol, chemTermsExpr, resultType, symbol, new Class[0], new Object[0]);
        }

        Calc(String symbol, String chemTermsExpr, Class resultType, Class[] paramTypes, Object[] defaultParams) {
            this(symbol, chemTermsExpr, resultType, symbol, paramTypes, defaultParams);
        }

        Calc(String symbol, String chemTermsExpr, Class resultType, String defaultPropName, Class[] paramTypes, Object[] defaultParams) {
            this.symbol = symbol;
            this.chemTermsExpr = chemTermsExpr;
            this.resultType = resultType;
            this.defaultPropName = defaultPropName;
            this.paramTypes = paramTypes;
            this.defaultParams = defaultParams;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getChemTermsExpr() {
            return chemTermsExpr;
        }

        public Class getResultType() {
            return resultType;
        }

        public String getDefaultPropName() {
            return defaultPropName;
        }

        public Class[] getParamTypes() {
            return paramTypes;
        }

        public Object[] getDefaultParams() {
            return defaultParams;
        }
    }


    private final Calc calc;
    private final String propName;
    private final Object[] params;
    private final String chemTermsExpr;
    private final ChemJEPPool pool;

    public Calc getCalc() {
        return calc;
    }

    public String getPropName() {
        return propName;
    }

    public String getChemTermsExpr() {
        return chemTermsExpr;
    }

    /**
     * Constructor to standard ChemTerms evaluator. The property is calculated
     * and added to the molecule.
     *
     * @param calc     The calculator definition
     * @param propName Name for the calculated property
     * @throws ParseException
     */
    private ChemTermsCalculator(Calc calc, String propName, Object[] params) {
        this.calc = calc;
        this.propName = propName;
        this.params = params;
        this.chemTermsExpr = generateChemTermsExpr(calc, params);
        try {
            this.pool = new ChemJEPPool(this.chemTermsExpr, 25);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to create ChemTermsCalculator for expression " + chemTermsExpr, e);
        }
    }

    private String generateChemTermsExpr(Calc calc, Object[] params) {
        if (calc.defaultParams != null && calc.defaultParams.length > 0) {
            Object[] myParams = new Object[calc.defaultParams.length];
            for (int i = 0; i < calc.defaultParams.length; i++) {
                if (params.length > i && params[i] != null) {
                    myParams[i] = params[i];
                } else {
                    myParams[i] = calc.defaultParams[i];
                }
            }
            String result = MessageFormat.format(calc.chemTermsExpr, myParams);
            LOG.fine("Generated ChemTerms expression " + result);
            return result;
        } else {
            return calc.chemTermsExpr;
        }
    }

    public static ChemTermsCalculator create(Calc calc, String propName, Object[] params) {
        return new ChemTermsCalculator(calc, propName, params);
    }

    public static ChemTermsCalculator create(Calc calc, Object[] params) {
        return new ChemTermsCalculator(calc, calc.defaultPropName, params);
    }

    public static ChemTermsCalculator create(Calc calc) {
        return new ChemTermsCalculator(calc, calc.defaultPropName, new Object[0]);
    }

    /**
     * Evaluate the chemical terms expression and set the result to the MoleculeObject that is returned
     * @param mo
     * @param stats
     */
    @Override
    public Object calculate(MoleculeObject mo, Map<String, Integer> stats) {
        if (mo == null || mo.getMol() == null) {
            return null;
        }

        Molecule mol = mo.getMol();
        Object result = processMolecule(mol, stats);

        if (result != null) {
            mo.setProperty(propName, result);
        }
        return result;
    }

    /**
     * Process a molecule and return its calculated value.
     * The result is NOT set as a property of the molecule.
     *
     * @param mol The molecule.
     * @return The result.
     */
    public Object processMolecule(Molecule mol, Map<String, Integer> stats) {
        if (mol == null) {
            return null;
        }
        MolContext context = new MolContext();
        context.setMolecule(mol);
        return evaluateMoleculeImpl(context, stats);
    }

    private Object evaluateMoleculeImpl(MolContext context, Map<String, Integer> stats) {
        final ChemJEP chemJEP = pool.checkout();
        try {
            Object result = chemJEP.evaluate(context);
            result = filterResult(result);
            if (result != null) {
                ExecutionStats.increment(stats, calc.symbol, 1);
            }
            return result;
        } catch (ParseException ex) {
            LOG.log(Level.WARNING, "Failed to evaluate chem terms expression. Property will be missing.", ex);
            return null;
        } finally {
            pool.checkin(chemJEP);
        }
    }

    /**
     * Do some necessary transformations on the result
     *
     * @param value
     * @return The possible transformed result.
     */
    public Object filterResult(Object value) {

        if (value != null) {
            if (value instanceof Double) {
                Double d = (Double) value;
                if (!d.isNaN() && !d.isInfinite()) {
                    return d;
                }
            } else if (value instanceof Float) {
                Float f = (Float) value;
                if (!f.isNaN() && !f.isInfinite()) {
                    return f;
                }
            } else {
                return value;
            }
        }
        return null;
    }

    class ChemJEPPool extends Pool<ChemJEP> {

        final String chemTermsFunction;
        final Evaluator evaluator;

        ChemJEPPool(String chemTermsFunction, int size) throws ParseException {
            super(size);
            this.chemTermsFunction = chemTermsFunction;
            this.evaluator = new Evaluator();
            checkin(doCreate());
        }

        @Override
        protected ChemJEP create() {
            try {
                return doCreate();
            } catch (ParseException ex) {
                throw new RuntimeException("Failed to create ChemJEP", ex);
            }
        }

        private ChemJEP doCreate() throws ParseException {
            return evaluator.compile(chemTermsFunction, MolContext.class);
        }
    }
}
