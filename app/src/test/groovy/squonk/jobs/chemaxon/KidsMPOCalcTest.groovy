package squonk.jobs.chemaxon

import spock.lang.Specification
import squonk.jobs.chemaxon.util.Filters.FilterMode

class KidsMPOCalcTest extends Specification {

    def "smi no filter"() {

        setup:
        def calc = new KidsMPOCalc()

        when:
        def count = calc.calculate("../data/10.smi", null, false, FilterMode.none, null, null)

        then:
        count == 10
    }

    def "sdf no filter"() {

        setup:
        def calc = new KidsMPOCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf", null, false, FilterMode.none, null, null)

        then:
        count == 10
    }

    def "sdf min filter"() {

        setup:
        def calc = new KidsMPOCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf", null, false, FilterMode.pass, 5.0f, null)

        then:
        count > 0
        count < 10
    }

    def "sdf max filter"() {

        setup:
        def calc = new KidsMPOCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf",null, false, FilterMode.pass, null, 4.0f)

        then:
        count > 0
        count < 10
    }

    def "sdf min and max filters"() {

        setup:
        def calc = new KidsMPOCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf", null, false, FilterMode.pass, 5.0f, 8.0f)

        then:
        count > 0
        count < 10
    }
}
