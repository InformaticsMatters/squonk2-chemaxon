/*
 *
 *  * Copyright (c) 2022  Informatics Matters Ltd.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package squonk.jobs.chemaxon.util

import spock.lang.Specification

import squonk.jobs.chemaxon.util.DMLogger.Level

class DMLoggerTest extends Specification {

    def "generate event message"() {

        setup:
        def logger = new DMLogger()
        def d = new Date(1647941704123)

        when:
        def txt = logger.createEventMessage(Level.INFO, d, 'Hello world!')
        println txt

        then:
        txt == '2022-03-22T09:35:04+00:00 # INFO -EVENT- Hello world!'
    }

    def "incremental cost message"() {

        setup:
        def logger = new DMLogger()
        def d = new Date(1647941704123)

        when:
        def txt1 = logger.createCostMessage(d,1.2, true)
        def txt2 = logger.createCostMessage(d,1.3, true)
        println txt1

        then:
        txt1 == '2022-03-22T09:35:04+00:00 # INFO -COST- +1.2 1'
        txt2 == '2022-03-22T09:35:04+00:00 # INFO -COST- +1.3 2'
    }

    def "non incremental cost message"() {

        setup:
        def logger = new DMLogger()
        def d = new Date(1647941704123)

        when:
        def txt1 = logger.createCostMessage(d,1.2, false)
        def txt2 = logger.createCostMessage(d,1.3, false)
        println txt1

        then:
        txt1 == '2022-03-22T09:35:04+00:00 # INFO -COST- 1.2 1'
        txt2 == '2022-03-22T09:35:04+00:00 # INFO -COST- 1.3 2'
    }
}
