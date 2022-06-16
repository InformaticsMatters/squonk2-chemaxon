/*
 * Copyright (c) 2020 Informatics Matters Ltd.
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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author timbo
 */
public class ChemTermsCalculator {

    private static final Logger LOG = Logger.getLogger(ChemTermsCalculator.class.getName());


    public enum Calc {
        AtomCount("CXN_atomCount", "atomCount()"),
        BondCount("CXN_bondCount", "bondCount()"),
        LogP("CXN_cLogP", "logP()"),
        // single quote has to be represented as 2 single quotes as the expansion uses MessageFormat
        LogD("CXN_logD", "logD(''{0}'')", new Class[]{Float.class}, new Object[]{7.4f}),
        HBondDonorCount("CXN_donorCount", "donorCount()"),
        HBondAcceptorCount("CXN_acceptorCount", "acceptorCount()"),
//        ("", "", "CXN_",

        ;

        private String symbol;
        private String chemTermsExpr;
        private String defaultPropName;
        Class[] paramTypes;
        Object[] defaultParams;

        Calc(String symbol, String chemTermsExpr) {
            this(symbol, chemTermsExpr, symbol, new Class[0], new Object[0]);
        }

        Calc(String symbol, String chemTermsExpr, Class[] paramTypes, Object[] defaultParams) {
            this(symbol, chemTermsExpr, symbol, paramTypes, defaultParams);
        }

        Calc(String symbol, String chemTermsExpr, String defaultPropName, Class[] paramTypes, Object[] defaultParams) {
            this.symbol = symbol;
            this.chemTermsExpr = chemTermsExpr;
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
     * Process a molecule. If this is a standard evaluator the result of the
     * chemical terms evaluation is added as a property of the molecule. If the
     * evaluator is a filter then null is returned to signify that the filter
     * has failed
     *
     * @param mol The molecule.
     * @return The molecule with the calculated property set, or null if this
     * evaluator is a filter and the filter fails.
     */
    public Molecule processMolecule(Molecule mol, Map<String, Integer> stats) {
        if (mol == null) {
            return null;
        }
        MolContext context = new MolContext();
        context.setMolecule(mol);
        return evaluateMoleculeImpl(context, stats);
    }

    public MoleculeObject processMoleculeObject(MoleculeObject mo, Map<String, Integer> stats) throws IOException {
        if (mo == null || mo.getMol() == null) {
            return mo;
        }

        Molecule mol = mo.getMol();
        mol = processMolecule(mol, stats);

        if (mol == null) {
            return null;
        } else {
            // TODO - review this
            Map<String, Object> results = getResults(mol);
            if (mol != mo.getMol()) {
                mo = new MoleculeObject(mol);
            }
            mo.setProperties(results);
            return mo;
        }
    }

    private Molecule evaluateMoleculeImpl(MolContext context, Map<String, Integer> stats) {
        final ChemJEP chemJEP = pool.checkout();

        try {
            Object result = chemJEP.evaluate(context);
            if (result != null) {
                context.getMolecule().setPropertyObject(propName, result);
                ExecutionStats.increment(stats, calc.symbol, 1);
            }
        } catch (ParseException ex) {
            LOG.log(Level.WARNING, "Failed to evaluate chem terms expression. Property will be missing.", ex);
        } finally {
            pool.checkin(chemJEP);
        }
        return context.getMolecule();
    }

    /**
     * Allows the result to be extracted from the Molecule once it has been
     * calculated. If not Mode.Calculate an empty Map is returned
     *
     * @param mol
     * @return the calculated value as a Map, key being the property name.
     */
    public Map<String, Object> getResults(Molecule mol) {

        Object value = mol.getPropertyObject(propName);
        if (value != null) {
            if (value instanceof Double) {
                Double d = (Double) value;
                if (!d.isNaN() && !d.isInfinite()) {
                    return Collections.singletonMap(propName, d);
                }
            } else if (value instanceof Float) {
                Float f = (Float) value;
                if (!f.isNaN() && !f.isInfinite()) {
                    return Collections.singletonMap(propName, f);
                }
            } else {
                return Collections.singletonMap(propName, value);
            }
        }

        return Collections.emptyMap();
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
