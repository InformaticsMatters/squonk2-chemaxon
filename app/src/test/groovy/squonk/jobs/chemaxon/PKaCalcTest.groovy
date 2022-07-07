package squonk.jobs.chemaxon

import spock.lang.Specification

class PKaCalcTest extends Specification {

    def "smi acidic only"() {

        setup:
        def calc = new PKaCalc()

        when:
        def count = calc.calculate("../data/10.smi", null, false, true, false)

        then:
        count == 10
    }

    def "smi basic only"() {

        setup:
        def calc = new PKaCalc()

        when:
        def count = calc.calculate("../data/10.smi", null, false, false, true)

        then:
        count == 10
    }

    def "smi both"() {

        setup:
        def calc = new PKaCalc()

        when:
        def count = calc.calculate("../data/10.smi", null, false, true, true)

        then:
        count == 10
    }

    def "sdf both"() {

        setup:
        def calc = new PKaCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf", null, false, true, true)

        then:
        count == 10
    }

}
