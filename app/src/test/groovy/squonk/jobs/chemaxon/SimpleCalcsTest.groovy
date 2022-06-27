package squonk.jobs.chemaxon

import spock.lang.Specification
import squonk.jobs.chemaxon.util.ChemTermsCalculator

class SimpleCalcsTest extends Specification {

    def "calc sdf all"() {

        setup:
        def sc = new SimpleCalcs()

        when:
        def count = sc.calculate(ChemTermsCalculator.Calc.values(), "../data/dhfr_3d-10.sdf", null, false)

        then:
        count == 10
    }

    def "calc smi all"() {

        setup:
        def sc = new SimpleCalcs()

        when:
        def count = sc.calculate(ChemTermsCalculator.Calc.values(), "../data/10.smi", null, true)

        then:
        count == 10
    }
}
