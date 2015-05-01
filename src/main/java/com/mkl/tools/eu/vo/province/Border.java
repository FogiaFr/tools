package com.mkl.tools.eu.vo.province;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang3.StringUtils;

/**
 * Inner class describing a border between two provinces.
 *
 * @author MKL
 */
@XStreamAlias("border")
public class Border {
    /** First province (alphabetical order) of the border. */
    @XStreamAlias("first")
    private String first;
    /** Second province (alphabetical order) of the border. */
    @XStreamAlias("second")
    private String second;
    /** Type of border. */
    @XStreamAlias("type")
    private String type;

    /**
     * Constructor.
     *
     * @param province1 first province.
     * @param province2 second province.
     * @param type      type.
     */
    public Border(Province province1, Province province2, String type) {
        if (province1 == null || province2 == null || province1 == province2) {
            throw new IllegalStateException();
        }

        if (province1.getName().compareTo(province2.getName()) < 0) {
            first = province1.getName();
            second = province2.getName();
        } else {
            first = province2.getName();
            second = province1.getName();
        }

        this.type = type;
    }

    /** @return the first. */
    public String getFirst() {
        return first;
    }

    /** @return the second. */
    public String getSecond() {
        return second;
    }

    /** @return the type. */
    public String getType() {
        return type;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return 11 + 13 * first.hashCode() + 15 * second.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        boolean equals = false;

        if (obj instanceof Border) {
            Border border = (Border) obj;

            equals = StringUtils.equals(first, border.getFirst())
                    && StringUtils.equals(second, border.getSecond());
        }

        return equals;
    }
}