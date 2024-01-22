package squonk.jobs.chemaxon

import spock.lang.Specification
import squonk.jobs.chemaxon.util.ChemTermsCalculator

class SimpleCalcsTest extends Specification {

    def "calc sdf all"() {

        setup:
        def sc = new SimpleCalcs()

        when:
        def counts = sc.calculate(ChemTermsCalculator.Calc.values(), "../data/dhfr_3d-10.sdf", null, false)

        then:
        counts[0] == 10
        counts[1] == 0
    }

    def "calc smi all"() {

        setup:
        def sc = new SimpleCalcs()

        when:
        def counts = sc.calculate(ChemTermsCalculator.Calc.values(), "../data/10.smi", null, true)

        then:
        counts[0] == 10
        counts[1] == 0
    }

    def "calc csv all"() {

        setup:
        def sc = new SimpleCalcs()

        when:
        def counts = sc.calculate(ChemTermsCalculator.Calc.values(), "../data/10.csv", null, true)

        then:
        counts[0] == 10
        counts[1] == 0
    }

    /**
     *  The file has 10 records including 2 bad molecules
     * @return
     */
    def "calc bad all"() {

        setup:
        def sc = new SimpleCalcs()

        when:
        def counts = sc.calculate(ChemTermsCalculator.Calc.values(), "../data/bad.smi", null, true)
        then:
        counts[0] == 10
        counts[1] == 2
    }
}
