package squonk.jobs.chemaxon

import spock.lang.Specification
import squonk.jobs.chemaxon.util.Filters.FilterMode

class GuptaBBBCalcTest extends Specification {

    def "smi bbb no filter"() {

        setup:
        def calc = new GuptaBBBCalc()

        when:
        def count = calc.calculate("../data/10.smi", null, false, FilterMode.none, null, null)

        then:
        count == 10
    }

    def "sdf bbb no filter"() {

        setup:
        def calc = new GuptaBBBCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf", null, false, FilterMode.none, null, null)

        then:
        count == 10
    }

    def "sdf bbb min filter"() {

        setup:
        def calc = new GuptaBBBCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf", null, false, FilterMode.pass, 3.0f, null)

        then:
        count > 0
        count < 10
    }

    def "sdf bbb max filter"() {

        setup:
        def calc = new GuptaBBBCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf",null, false, FilterMode.pass, null, 4.0f)

        then:
        count > 0
        count < 10
    }

    def "sdf bbb min and max filters"() {

        setup:
        def calc = new GuptaBBBCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf", null, false, FilterMode.pass, 2.0f, 4.0f)

        then:
        count > 0
        count < 10
    }
}
