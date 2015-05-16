package com.mkl.tools.eu.vo.province;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inner class describing a path (can be border, mer or path).
 *
 * @author MKL
 */
public class Path {
    /** Name of the path. */
    private String name;
    /** Flag saying that the path begins by itself (and does not continue the previous one). */
    private boolean begin;
    /** Flag saying that this SubProvince is located in the ROTW map. */
    private boolean rotw;
    /** Coordinates of the path. */
    private List<Pair<Double, Double>> coords = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param name  of the path.
     * @param begin of the path.
     * @param rotw  flag saying that the object is in the rotw map.
     */
    public Path(String name, boolean begin, boolean rotw) {
        this.name = name;
        this.begin = begin;
        this.rotw = rotw;
    }

    /** @return the name. */
    public String getName() {
        return name;
    }

    /** @return the begin. */
    public boolean isBegin() {
        return begin;
    }

    /** @return the rotw. */
    public boolean isRotw() {
        return rotw;
    }

    /** @return the coords. */
    public List<Pair<Double, Double>> getCoords() {
        return coords;
    }

    /**
     * Returns the inverted coords of the path.
     *
     * @return the inverted coords of the path.
     */
    public List<Pair<Double, Double>> getInvertedCoords() {
        List<Pair<Double, Double>> invertedCoords = new ArrayList<>(coords);
        Collections.reverse(invertedCoords);

        return invertedCoords;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        boolean equals = false;

        if (obj instanceof Path) {
            equals = StringUtils.equals(name, ((Path) obj).getName())
                    && rotw == ((Path) obj).isRotw();
        }

        return equals;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(rotw).hashCode();
    }
}