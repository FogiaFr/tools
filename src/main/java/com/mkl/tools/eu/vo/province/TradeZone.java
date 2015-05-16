package com.mkl.tools.eu.vo.province;

/**
 * Trade zone (ZP / ZM).
 *
 * @author MKL.
 */
public class TradeZone {
    /** Name of the sea zone where the trade zone is located. */
    private String seaZone;
    /** Type of the trade zone (ZP or ZM). */
    private String type;
    /** In case of ZP, name of the country where the trade zone is. */
    private String countryName;
    /** Income earned by a total monopoly (halved for partial monopoly). */
    private int monopoly;
    /** Income earned by presence (none if partial or total monopoly). */
    private int presence;

    /** @return the seaZone. */
    public String getSeaZone() {
        return seaZone;
    }

    /** @param seaZone the seaZone to set. */
    public void setSeaZone(String seaZone) {
        this.seaZone = seaZone;
    }

    /** @return the type. */
    public String getType() {
        return type;
    }

    /** @param type the type to set. */
    public void setType(String type) {
        this.type = type;
    }

    /** @return the countryName. */
    public String getCountryName() {
        return countryName;
    }

    /** @param countryName the countryName to set. */
    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    /** @return the monopoly. */
    public int getMonopoly() {
        return monopoly;
    }

    /** @param monopoly the monopoly to set. */
    public void setMonopoly(int monopoly) {
        this.monopoly = monopoly;
    }

    /** @return the presence. */
    public int getPresence() {
        return presence;
    }

    /** @param presence the presence to set. */
    public void setPresence(int presence) {
        this.presence = presence;
    }
}
