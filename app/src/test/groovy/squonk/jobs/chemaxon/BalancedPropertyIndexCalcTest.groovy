package squonk.jobs.chemaxon

import spock.lang.Specification
import squonk.jobs.chemaxon.util.Filters.FilterMode

class BalancedPropertyIndexCalcTest extends Specification {

    def "smi bpi no filter"() {

        setup:
        def calc = new BalancedPropertyIndexCalc()

        when:
        def counts = calc.calculateBPI("../data/10.smi", null, false, FilterMode.none, null, null)

        then:
        counts[0] == 10
        counts[1] == 0
    }

    def "smi bpi no filter bad"() {

        setup:
        def calc = new BalancedPropertyIndexCalc()

        when:
        def counts = calc.calculateBPI("../data/bad.smi", null, false, FilterMode.none, null, null)

        then:
        counts[0] == 10
        counts[1] == 2
    }

    def "sdf bpi no filter"() {

        setup:
        def calc = new BalancedPropertyIndexCalc()

        when:
        def counts = calc.calculateBPI("../data/dhfr_3d-10.sdf", null, false, FilterMode.none, null, null)

        then:
        counts[0] == 10
        counts[1] == 0
    }

    def "sdf bpi min filter"() {

        setup:
        def calc = new BalancedPropertyIndexCalc()

        when:
        def counts = calc.calculateBPI("../data/dhfr_3d-10.sdf", null, false, FilterMode.pass, 2.0f, null)

        then:
        counts[0] == 4
        counts[1] == 0
    }

    def "sdf bpi max filter"() {

        setup:
        def calc = new BalancedPropertyIndexCalc()

        when:
        def counts = calc.calculateBPI("../data/dhfr_3d-10.sdf",null, false, FilterMode.pass, null, 2.0f)

        then:
        counts[0] == 6
        counts[1] == 0
    }

    def "sdf bpi min and max filters"() {

        setup:
        def calc = new BalancedPropertyIndexCalc()

        when:
        def counts = calc.calculateBPI("../data/dhfr_3d-10.sdf", null, false, FilterMode.pass, 1.5f, 2.0f)

        then:
        counts[0] == 4
        counts[1] == 0
    }
}
