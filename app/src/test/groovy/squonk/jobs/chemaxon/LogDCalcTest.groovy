package squonk.jobs.chemaxon

import spock.lang.Specification
import squonk.jobs.chemaxon.util.Filters.FilterMode


class LogDCalcTest extends Specification {

    def "smi logd no filter"() {

        setup:
        def calc = new LogDCalc()

        when:
        def count = calc.calculate("../data/10.smi", null, false, 7.4f, FilterMode.none, null, null)

        then:
        count == 10
    }

    def "sdf logd no filter"() {

        setup:
        def calc = new LogDCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf", null, false, 7.4f, FilterMode.none, null, null)

        then:
        count == 10
    }

    def "sdf logd min filter"() {

        setup:
        def calc = new LogDCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf", null, false, 7.4f, FilterMode.pass, 3.0f, null)

        then:
        count > 0
        count < 10
    }

    def "sdf logd max filter"() {

        setup:
        def calc = new LogDCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf",null, false, 7.4f, FilterMode.pass, null, 5.0f)

        then:
        count > 0
        count < 10
    }

    def "sdf logd min and max filters"() {

        setup:
        def calc = new LogDCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf", null, false, 7.4f, FilterMode.pass, 3.0f, 5.0f)

        then:
        count > 0
        count < 10
    }
}
