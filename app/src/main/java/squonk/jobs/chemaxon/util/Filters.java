package squonk.jobs.chemaxon.util;

import squonk.jobs.chemaxon.AbbvieMPSCalc;

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
