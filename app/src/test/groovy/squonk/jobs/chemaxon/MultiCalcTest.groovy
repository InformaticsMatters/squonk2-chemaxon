package squonk.jobs.chemaxon

import spock.lang.FailsWith
import spock.lang.Specification
import squonk.jobs.chemaxon.util.ChemTermsCalculator

class MultiCalcTest extends Specification {

    def calcs = [
            "molecular-weight",
            "atom-count",
            "gupta-bbb"
    ] as String[]

    def "calc sdf"() {

        setup:
        def sc = new MultiCalc()

        when:
        def counts = sc.calculate(calcs, "../data/dhfr_3d-10.sdf", null, false)

        then:
        counts[0] == 10
        counts[1] == 0
    }

    def "calc smi"() {

        setup:
        def sc = new MultiCalc()

        when:
        def counts = sc.calculate(calcs, "../data/10.smi", null, true)

        then:
        counts[0] == 10
        counts[1] == 0
    }

    @FailsWith(chemaxon.formats.MolFormatException)
    def "calc csv"() {

        setup:
        def sc = new MultiCalc()

        when:
        def counts = sc.calculate(calcs, "../data/10.csv", null, true)

        then:
        counts[0] == 10
        counts[1] == 0
    }

    /**
     *  The file has 10 records including 2 bad molecules
     * @return
     */
    def "calc bad"() {

        setup:
        def sc = new SimpleCalcs()

        when:
        def counts = sc.calculate(ChemTermsCalculator.Calc.values(), "../data/bad.smi", null, true)
        then:
        counts[0] == 10
        counts[1] == 2
    }
}
