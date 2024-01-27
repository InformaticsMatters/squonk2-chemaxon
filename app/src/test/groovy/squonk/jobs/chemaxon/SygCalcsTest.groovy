package squonk.jobs.chemaxon

import spock.lang.Specification
import squonk.jobs.chemaxon.util.ChemTermsCalculator

class SygCalcsTest extends Specification {

    def "calc smi all"() {

        setup:
        def sc = new SygCalcs()
        def w = new StringWriter()

        when:
        def counts = sc.calculate("../data/10.smi", w)
        def data = w.toString()
        println data

        then:
        counts[0] == 10
        counts[1] == 0
        data.split("\n").length == 11
    }

}
