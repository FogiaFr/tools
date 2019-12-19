package com.mkl.tools.eu.vo;

/**
 * Simple leader.
 *
 * @author MKL.
 */
public class Leader {
    private LeaderType type;
    private String code;
    private String country;
    private String code2;
    private String country2;

    /** @return the type. */
    public LeaderType getType() {
        return type;
    }

    /** @param type the type to set. */
    public void setType(LeaderType type) {
        this.type = type;
    }

    /** @return the code. */
    public String getCode() {
        return code;
    }

    /** @param code the code to set. */
    public void setCode(String code) {
        this.code = code;
    }

    /** @return the country. */
    public String getCountry() {
        return country;
    }

    /** @param country the country to set. */
    public void setCountry(String country) {
        this.country = country;
    }

    /** @return the code2. */
    public String getCode2() {
        return code2;
    }

    /** @param code2 the code2 to set. */
    public void setCode2(String code2) {
        this.code2 = code2;
    }

    /** @return the country2. */
    public String getCountry2() {
        return country2;
    }

    /** @param country2 the country2 to set. */
    public void setCountry2(String country2) {
        this.country2 = country2;
    }

    public enum LeaderType {
        LEADER,
        LEADERDOUBLE,
        LEADERPAIRE
    }
}
