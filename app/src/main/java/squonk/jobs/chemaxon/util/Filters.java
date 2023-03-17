/*
 * Copyright (c) 2023 Informatics Matters Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package squonk.jobs.chemaxon.util;

import java.util.logging.Logger;
import java.util.stream.Stream;

public class Filters {

    private static final Logger LOG = Logger.getLogger(Filters.class.getName());

    public enum FilterMode {none, pass, fail}

    public static Stream<MoleculeObject> applyFilters(Stream<MoleculeObject> mols, FilterMode mode,
                                                      String fieldName, Number minValue, Number maxValue) {

        if (mode == null || mode == FilterMode.none) {
            return mols;
        }

        if (mode == FilterMode.pass) {
            LOG.info(String.format("Adding pass filters of %s and %s", minValue, maxValue));
            if (minValue != null && maxValue != null) { // both min and max values
                mols = mols.filter(mo -> {
                    Number value = (Number) (mo.getProperty(fieldName));
                    boolean b =  (value != null &&
                            value.doubleValue() >= minValue.doubleValue() &&
                            value.doubleValue() <= maxValue.doubleValue());
                    LOG.finest("Value (pass, min,max): " + value + " " + b);
                    return b;
                });
            } else if (minValue != null) { // only a min value
                mols = mols.filter(mo -> {
                    Number value = (Number) (mo.getProperty(fieldName));
                    boolean b =  (value != null && value.doubleValue() >= minValue.doubleValue());
                    LOG.finest("Value (pass, min: " + value + " " + b);
                    return b;
                });
            } else if (maxValue != null) { // only a max value
                mols = mols.filter(mo -> {
                    Number value = (Number) (mo.getProperty(fieldName));
                    boolean b =  (value != null && value.doubleValue() <= maxValue.doubleValue());
                    LOG.finest("Value (pass, max): " + value + " " + b);
                    return b;
                });
            }
        } else if (mode == FilterMode.fail) {
            LOG.info(String.format("Adding fail filters of %s and %s", minValue, maxValue));
            if (minValue != null && maxValue != null) { // both min and max values
                mols = mols.filter(mo -> {
                    Number value = (Number) mo.getProperty(fieldName);
                    boolean b = (value == null ||
                            value.doubleValue() < minValue.doubleValue() ||
                            value.doubleValue() > maxValue.doubleValue());
                    LOG.finest("Value (fail, min,max): " + value + " " + b);
                    return b;
                });
            } else if (minValue != null) { // only a min value
                mols = mols.filter(mo -> {
                    Number value = (Number) mo.getProperty(fieldName);
                    boolean b = (value == null || value.doubleValue() < minValue.doubleValue());
                    LOG.finest("Value (fail, min): " + value + " " + b);
                    return b;
                });
            } else if (maxValue != null) { // only a max value
                mols = mols.filter(mo -> {
                    Number value = (Number) mo.getProperty(fieldName);
                    boolean b = (value == null || value.doubleValue() > maxValue.doubleValue());
                    LOG.finest("Value (fail, min): " + value + " " + b);
                    return b;
                });
            }
        }
        return mols;
    }
}
