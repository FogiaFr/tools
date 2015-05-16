package com.mkl.tools.eu.vo.province;

import java.util.*;

/**
 * Additional information on a european province.
 *
 * @author MKL
 */
public class ProvinceInfo {
    /** Principal name of the city. */
    private String nameCity;
    /** Additional names of the city. */
    private List<String> altNameCity = new ArrayList<>();
    /** Principal name of the province. */
    private String nameProvince;
    /** Additional names of the province. */
    private List<String> altNameProvince = new ArrayList<>();
    /** Income of the province. */
    private int income = 0;
    /** Default owner of the province. */
    private String defaultOwner;
    /** Flag saying that the province is a capital. */
    private boolean capital = false;
    /** X coordinate of the fortress. */
    private int x;
    /** Y coordinate of the fortress. */
    private int y;
    /** Level of the natural fortress. */
    private int fortress = 0;
    /** Flag saying that the province has a natural port. */
    private boolean port = false;
    /** Flag saying that the port has a natural arsenal. */
    private boolean arsenal = false;
    /** Flag saying that the natural port/arsenal can be blocked by a fortress. */
    private boolean praesidiable = false;

    /** @return the nameCity. */
    public String getNameCity() {
        return nameCity;
    }

    /** @param nameCity the nameCity to set. */
    public void setNameCity(String nameCity) {
        this.nameCity = nameCity;
    }

    /** @return the altNameCity. */
    public List<String> getAltNameCity() {
        return altNameCity;
    }

    /** @return the nameProvince. */
    public String getNameProvince() {
        return nameProvince;
    }

    /** @param nameProvince the nameProvince to set. */
    public void setNameProvince(String nameProvince) {
        this.nameProvince = nameProvince;
    }

    /** @return the altNameProvince. */
    public List<String> getAltNameProvince() {
        return altNameProvince;
    }

    /** @return the income. */
    public int getIncome() {
        return income;
    }

    /** @param income the income to set. */
    public void setIncome(int income) {
        this.income = income;
    }

    /** @return the defaultOwner. */
    public String getDefaultOwner() {
        return defaultOwner;
    }

    /** @param defaultOwner the defaultOwner to set. */
    public void setDefaultOwner(String defaultOwner) {
        this.defaultOwner = defaultOwner;
    }

    /** @return the capital. */
    public boolean isCapital() {
        return capital;
    }

    /** @param capital the capital to set. */
    public void setCapital(boolean capital) {
        this.capital = capital;
    }

    /** @return the y. */
    public int getY() {
        return y;
    }

    /** @param y the y to set. */
    public void setY(int y) {
        this.y = y;
    }

    /** @return the x. */
    public int getX() {
        return x;
    }

    /** @param x the x to set. */
    public void setX(int x) {
        this.x = x;
    }

    /** @return the fortress. */
    public int getFortress() {
        return fortress;
    }

    /** @param fortress the fortress to set. */
    public void setFortress(int fortress) {
        this.fortress = fortress;
    }

    /** @return the port. */
    public boolean isPort() {
        return port;
    }

    /** @param port the port to set. */
    public void setPort(boolean port) {
        this.port = port;
    }

    /** @return the arsenal. */
    public boolean isArsenal() {
        return arsenal;
    }

    /** @param arsenal the arsenal to set. */
    public void setArsenal(boolean arsenal) {
        this.arsenal = arsenal;
    }

    /** @return the praesidiable. */
    public boolean isPraesidiable() {
        return praesidiable;
    }

    /** @param praesidiable the praesidiable to set. */
    public void setPraesidiable(boolean praesidiable) {
        this.praesidiable = praesidiable;
    }

    /**
     * Returns a Collection of distinct names of cities/provinces.
     *
     * @param realNameProvince the name of the owning province.
     * @return a Collection of distinct names of cities/provinces.
     */
    public Collection<String> getMetadata(String realNameProvince) {
        Set<String> metadata = new HashSet<>();

        metadata.add(realNameProvince);
        metadata.add(nameProvince);
        metadata.addAll(altNameProvince);
        metadata.add(nameCity);
        metadata.addAll(altNameCity);

        return metadata;
    }
}