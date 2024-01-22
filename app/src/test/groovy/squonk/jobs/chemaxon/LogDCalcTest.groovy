package squonk.jobs.chemaxon

import spock.lang.Specification
import squonk.jobs.chemaxon.util.Filters.FilterMode


class LogDCalcTest extends Specification {

    def "smi logd no filter"() {

        setup:
        def calc = new LogDCalc()

        when:
        def counts = calc.calculate("../data/10.smi", null, false, 7.4f, FilterMode.none, null, null)

        then:
        counts[0] == 10
        counts[1] == 0
    }

    def "smi logd no filter bad"() {

        setup:
        def calc = new LogDCalc()

        when:
        def counts = calc.calculate("../data/bad.smi", null, false, 7.4f, FilterMode.none, null, null)

        then:
        counts[0] == 10
        counts[1] == 2
    }

    def "sdf logd no filter"() {

        setup:
        def calc = new LogDCalc()

        when:
        def counts = calc.calculate("../data/dhfr_3d-10.sdf", null, false, 7.4f, FilterMode.none, null, null)

        then:
        counts[0] == 10
        counts[1] == 0
    }

    def "sdf logd min filter"() {

        setup:
        def calc = new LogDCalc()

        when:
        def counts = calc.calculate("../data/dhfr_3d-10.sdf", null, false, 7.4f, FilterMode.pass, 3.0f, null)

        then:
        counts[0] == 5
        counts[1] == 0
    }

    def "sdf logd max filter"() {

        setup:
        def calc = new LogDCalc()

        when:
        def counts = calc.calculate("../data/dhfr_3d-10.sdf",null, false, 7.4f, FilterMode.pass, null, 5.0f)

        then:
        counts[0] == 7
        counts[1] == 0
    }

    def "sdf logd min and max filters"() {

        setup:
        def calc = new LogDCalc()

        when:
        def counts = calc.calculate("../data/dhfr_3d-10.sdf", null, false, 7.4f, FilterMode.pass, 3.0f, 5.0f)

        then:
        counts[0] == 2
        counts[1] == 0
    }
}
