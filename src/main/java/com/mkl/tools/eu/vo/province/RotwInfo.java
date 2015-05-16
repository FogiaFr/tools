package com.mkl.tools.eu.vo.province;

import java.util.*;

/**
 * Additional information on a rotw province.
 *
 * @author MKL
 */
public class RotwInfo {
    /** Name of the region. */
    private String region;
    /** Level of the natural fortress (can be <code>null</code>). */
    private Integer fortress;
    /** Principal name of the city. */
    private String nameCity;
    /** Additional names of the city. */
    private List<String> altNameCity = new ArrayList<>();

    /** @return the region. */
    public String getRegion() {
        return region;
    }

    /** @param region the region to set. */
    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * Transfer the informations in a ProvinceInfo to a RotwInfo.
     *
     * @param info to transfer.
     */
    public void transfertFromProvinceInfo(ProvinceInfo info) {
        if (info != null) {
            this.nameCity = info.getNameCity();
            this.altNameCity = info.getAltNameCity();
            if (info.getFortress() > 0) {
                fortress = info.getFortress();
            }
        }
    }

    /** @return the fortress. */
    public Integer getFortress() {
        return fortress;
    }

    /** @return the nameCity. */
    public String getNameCity() {
        return nameCity;
    }

    /** @return the altNameCity. */
    public List<String> getAltNameCity() {
        return altNameCity;
    }

    /**
     * Returns a Collection of distinct names of cities/provinces.
     *
     * @return a Collection of distinct names of cities/provinces.
     */
    public Collection<String> getMetadata() {
        Set<String> metadata = new HashSet<>();

        metadata.add(region);
        metadata.add(nameCity);
        metadata.addAll(altNameCity);

        return metadata;
    }
}