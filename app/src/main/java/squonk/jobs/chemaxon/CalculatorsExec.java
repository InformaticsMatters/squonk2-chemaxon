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

import squonk.jobs.chemaxon.util.ChemTermsCalculator;
import squonk.jobs.chemaxon.util.MoleculeObject;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class CalculatorsExec {

    private static final Logger LOG = Logger.getLogger(CalculatorsExec.class.getName());

    public ChemTermsCalculator[] createCalculators(ChemTermsCalculator.Calc[] calcs, Object[][] params) {
        String[] propNames = new String[calcs.length];
        for (int i=0; i<calcs.length; i++) {
            propNames[i] = calcs[i].getDefaultPropName();
        }
        return createCalculators(calcs,  params, propNames);
    }

    public ChemTermsCalculator[] createCalculators(ChemTermsCalculator.Calc[] calcs, Object[][] params, String[] propNames) {
        ChemTermsCalculator[] calculators = new ChemTermsCalculator[calcs.length];

        for (int i = 0; i < calcs.length; i++) {
            if (params != null && params.length > i && params[i] != null) {
                calculators[i] = ChemTermsCalculator.create(calcs[i], propNames[i], params[i]);
            } else {
                calculators[i] = ChemTermsCalculator.create(calcs[i], propNames[i], new Object[0]);
            }
        }
        return calculators;
    }

    protected void doCalculate(MoleculeObject mo, ChemTermsCalculator[] calculators, Map<String, Integer> stats) {
        for (ChemTermsCalculator calculator : calculators) {
            try {
                calculator.processMoleculeObject(mo, stats);
            } catch (IOException e) {
                LOG.log(Level.INFO, "Failed to calculate " + calculator.getChemTermsExpr(), e);
            }
        }
    }

    public void calculate(
            MoleculeObject mo,
            ChemTermsCalculator.Calc[] calcs,
            Object[][] params,
            Map<String, Integer> stats) {
        ChemTermsCalculator[] calculators = createCalculators(calcs, params);
        doCalculate(mo, calculators, stats);
    }


    /**
     * Prepare calculation for a stream of molecules.
     * NOTE: you must perform a terminal operation on the stream in order for the calculations to happen.
     * All this method does is to create a new stream that has the calculations added.
     *
     * @param mols the stream of MoleculeObjects
     * @param calcs Array of calculator definitions
     * @param params Parameters for each calculator
     * @param stats Stats recorder
     * @return
     */
    public Stream<MoleculeObject> calculate(
            Stream<MoleculeObject> mols,
            ChemTermsCalculator.Calc[] calcs,
            Object[][] params,
            Map<String, Integer> stats) {
        ChemTermsCalculator[] calculators = createCalculators(calcs, params);
        return mols.peek(mo -> doCalculate(mo, calculators, stats));
    }

    public Stream<MoleculeObject> calculate(
            Stream<MoleculeObject> mols,
            ChemTermsCalculator[] calculators,
            Map<String, Integer> stats) {
        return mols.peek(mo -> doCalculate(mo, calculators, stats));
    }

}
