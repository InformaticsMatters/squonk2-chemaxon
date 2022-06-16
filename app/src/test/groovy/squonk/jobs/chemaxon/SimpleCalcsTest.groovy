package squonk.jobs.chemaxon

import spock.lang.Specification
import squonk.jobs.chemaxon.util.ChemTermsCalculator

class SimpleCalcsTest extends Specification {

    def "calc sdf all"() {

        setup:
        def sc = new SimpleCalcs()

        when:
        def count = sc.calculate(ChemTermsCalculator.Calc.values(), "../data/dhfr_3d.sdf", "../data/output.sdf")

        then:
        count == 756

    }
}
