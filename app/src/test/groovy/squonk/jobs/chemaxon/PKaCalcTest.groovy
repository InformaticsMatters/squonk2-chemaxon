package squonk.jobs.chemaxon

import spock.lang.Specification

class PKaCalcTest extends Specification {

    def "smi acidic only"() {

        setup:
        def calc = new PKaCalc()

        when:
        def count = calc.calculate("../data/10.smi", null, false, true, false, 3)

        then:
        count == 10
    }

    def "smi basic only"() {

        setup:
        def calc = new PKaCalc()

        when:
        def count = calc.calculate("../data/10.smi", null, false, false, true,3)

        then:
        count == 10
    }

    def "smi both"() {

        setup:
        def calc = new PKaCalc()

        when:
        def count = calc.calculate("../data/10.smi", '/tmp/pkas.sdf', false, true, true, 3)

        then:
        count == 10
    }

    def "sdf both 1"() {

        setup:
        def calc = new PKaCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf", '/tmp/pkas-1.sdf', false, true, true, 1)

        then:
        count == 10
    }

    def "sdf both 2"() {

        setup:
        def calc = new PKaCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf", '/tmp/pkas-2.sdf', false, true, true, 2)

        then:
        count == 10
    }

}
