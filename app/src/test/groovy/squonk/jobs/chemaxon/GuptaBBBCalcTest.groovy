package squonk.jobs.chemaxon

import spock.lang.Specification
import squonk.jobs.chemaxon.util.Filters.FilterMode

class GuptaBBBCalcTest extends Specification {

    def "smi bbb no filter"() {

        setup:
        def calc = new GuptaBBBCalc()

        when:
        def counts = calc.calculateBBB("../data/10.smi", null, false, FilterMode.none, null, null)

        then:
        counts[0] == 10
        counts[1] == 0
    }

    def "smi bbb no filter bad"() {

        setup:
        def calc = new GuptaBBBCalc()

        when:
        def counts = calc.calculateBBB("../data/bad.smi", null, false, FilterMode.none, null, null)

        then:
        counts[0] == 10
        counts[1] == 2
    }

    def "sdf bbb no filter"() {

        setup:
        def calc = new GuptaBBBCalc()

        when:
        def counts = calc.calculateBBB("../data/dhfr_3d-10.sdf", null, false, FilterMode.none, null, null)

        then:
        counts[0] == 10
        counts[1] == 0
    }

    def "sdf bbb min filter"() {

        setup:
        def calc = new GuptaBBBCalc()

        when:
        def counts = calc.calculateBBB("../data/dhfr_3d-10.sdf", null, false, FilterMode.pass, 3.0f, null)

        then:
        counts[0] == 9
        counts[1] == 0
    }

    def "sdf bbb max filter"() {

        setup:
        def calc = new GuptaBBBCalc()

        when:
        def counts = calc.calculateBBB("../data/dhfr_3d-10.sdf",null, false, FilterMode.pass, null, 4.0f)

        then:
        counts[0] == 6
        counts[1] == 0
    }

    def "sdf bbb min and max filters"() {

        setup:
        def calc = new GuptaBBBCalc()

        when:
        def counts = calc.calculateBBB("../data/dhfr_3d-10.sdf", null, false, FilterMode.pass, 2.0f, 4.0f)

        then:
        counts[0] == 5
        counts[1] == 0
    }
}
