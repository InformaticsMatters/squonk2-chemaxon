package squonk.jobs.chemaxon

import spock.lang.Specification

class SygCalcsTest extends Specification {

    def "calc text"() {

        setup:
        def sc = new SygCalcs()
        def w = new StringWriter()

        when:
        def counts = sc.calculate("../data/10.csv", w)
        def data = w.toString()
        println data

        then:
        counts[0] == 10
        counts[1] == 0
        data.split("\n").length == 11
    }

    def "calc file"() {

        def f = File.createTempFile("sygcalc_", ".csv")

        setup:
        def sc = new SygCalcs()
        def w = new FileWriter(f)

        when:
        def counts = sc.calculate("../data/10.smi", w)

        then:
        counts[0] == 10
        counts[1] == 0
        f.readLines().size() == 11

    }

}
