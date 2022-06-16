/*
 *
 *  * Copyright (c) 2022  Informatics Matters Ltd.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package squonk.jobs.chemaxon

import chemaxon.formats.MolImporter
import spock.lang.Specification
import squonk.jobs.chemaxon.util.ChemTermsCalculator
import squonk.jobs.chemaxon.util.MoleculeObject

import java.text.MessageFormat
import java.util.stream.Stream


class CalculatorsExecTest extends Specification {

    def "run calcs on single molecule"() {

        setup:
        def calculators = ChemTermsCalculator.Calc.values()
        def mol = MolImporter.importMol("c1ccccc1") // benzene
        def mo = new MoleculeObject(mol)
        def stats = [:]
        def exec = new CalculatorsExec()

        when:
        exec.calculate(mo, calculators, null, stats)
        //println("Props found = " + mo.getProperties().size())

        then:
        mo.getProperties().size() == calculators.length
    }

    def "run calcs on multiple molecule"() {

        setup:
        def calculators = ChemTermsCalculator.Calc.values()
        def mols = [
                new MoleculeObject(MolImporter.importMol("c1ccccc1")),
                new MoleculeObject(MolImporter.importMol("C1CCCCC1")),
                new MoleculeObject(MolImporter.importMol("CCC")),
                new MoleculeObject(MolImporter.importMol("CCCC")),
                new MoleculeObject(MolImporter.importMol("CNC")),
                new MoleculeObject(MolImporter.importMol("CCl"))
        ]
        def stats = [:]
        def exec = new CalculatorsExec()

        when:
        Stream str = exec.calculate(mols.stream(), calculators, null, stats)

        then:
        str.count() == mols.size()
    }

    def "format logd"() {

        setup:
        Object[] params = new Object[]{7.4f}

        when:
        def result = MessageFormat.format("logD(''{0}'')", params)

        then:
        result == "logD('7.4')"
    }

    def "calc logd with param"() {
        setup:
        def calculators = [ChemTermsCalculator.Calc.LogD] as ChemTermsCalculator.Calc[]
        def mol = MolImporter.importMol("c1ccccc1") // benzene
        def mo = new MoleculeObject(mol)
        def stats = [:]
        def exec = new CalculatorsExec()
        def params = [6.6f] as Object[]

        when:
        exec.calculate(mo, calculators, [params] as Object[][], stats)

        then:
        mo.getProperties().size() == calculators.length
        Math.abs(mo.getProperties().get(ChemTermsCalculator.Calc.LogD.symbol) - 1.97) < 0.1

    }

}
