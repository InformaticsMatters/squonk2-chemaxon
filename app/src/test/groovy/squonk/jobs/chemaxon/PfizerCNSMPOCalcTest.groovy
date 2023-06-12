package squonk.jobs.chemaxon

import spock.lang.Specification
import squonk.jobs.chemaxon.util.Filters.FilterMode

class PfizerCNSMPOCalcTest extends Specification {

    def "smi mps no filter"() {

        setup:
        def calc = new PfizerCNSMPOCalc()

        when:
        def count = calc.calculate("../data/10.smi", null, false, FilterMode.none, null, null)

        then:
        count == 10
    }

    def "smi mps min and max filters"() {

        setup:
        def calc = new PfizerCNSMPOCalc()

        when:
        def count = calc.calculate("../data/10.smi", null, false, FilterMode.pass, 1f, 5f)

        then:
        count == 4
    }

    def "sdf mps no filter"() {

        setup:
        def calc = new PfizerCNSMPOCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf", null, false, FilterMode.none, null, null)

        then:
        count == 10
    }

    def "sdf mps min filter"() {

        setup:
        def calc = new PfizerCNSMPOCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf", null, false, FilterMode.pass, 5.0f, null)

        then:
        count == 4
    }

    def "sdf mps max filter"() {

        setup:
        def calc = new PfizerCNSMPOCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf",null, false, FilterMode.pass, null, 4.0f)

        then:
        count == 5
    }

    def "sdf mps min and max filters"() {

        setup:
        def calc = new PfizerCNSMPOCalc()

        when:
        def count = calc.calculate("../data/dhfr_3d-10.sdf", null, false, FilterMode.pass, 5.0f, 8.0f)

        then:
        count == 4
    }
}
