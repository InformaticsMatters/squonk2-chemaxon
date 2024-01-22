package squonk.jobs.chemaxon

import spock.lang.Specification
import squonk.jobs.chemaxon.util.Filters.FilterMode

class KidsMPOCalcTest extends Specification {

    def "smi no filter"() {

        setup:
        def calc = new KidsMPOCalc()

        when:
        def counts = calc.calculate("../data/10.smi", null, false, FilterMode.none, null, null)

        then:
        counts[0] == 10
        counts[1] == 0
    }

    def "smi no filter bad"() {

        setup:
        def calc = new KidsMPOCalc()

        when:
        def counts = calc.calculate("../data/bad.smi", null, false, FilterMode.none, null, null)

        then:
        counts[0] == 10
        counts[1] == 2
    }

    def "sdf no filter"() {

        setup:
        def calc = new KidsMPOCalc()

        when:
        def counts = calc.calculate("../data/dhfr_3d-10.sdf", null, false, FilterMode.none, null, null)

        then:
        counts[0] == 10
        counts[1] == 0
    }

    def "sdf min filter"() {

        setup:
        def calc = new KidsMPOCalc()

        when:
        def counts = calc.calculate("../data/dhfr_3d-10.sdf", null, false, FilterMode.pass, 5.0f, null)

        then:
        counts[0] == 2
        counts[1] == 0
    }

    def "sdf max filter"() {

        setup:
        def calc = new KidsMPOCalc()

        when:
        def counts = calc.calculate("../data/dhfr_3d-10.sdf",null, false, FilterMode.pass, null, 4.0f)

        then:
        counts[0] == 2
        counts[1] == 0
    }

    def "sdf min and max filters"() {

        setup:
        def calc = new KidsMPOCalc()

        when:
        def counts = calc.calculate("../data/dhfr_3d-10.sdf", null, false, FilterMode.pass, 5.0f, 8.0f)

        then:
        counts[0] == 2
        counts[1] == 0
    }
}
