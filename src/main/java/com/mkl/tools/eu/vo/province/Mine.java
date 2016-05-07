package com.mkl.tools.eu.vo.province;

/**
 * This class describes where the gold mines are.
 *
 * @author MKL.
 */
public class Mine {
    /** Default amount of gold on a mine. */
    public static Integer DEFAULT_GOLD = 20;
    /** Name of the province where the gold is. */
    private String province;
    /** Amount of gold. */
    private Integer gold = DEFAULT_GOLD;

    /**
     * Constructor.
     *
     * @param province the province to set.
     */
    public Mine(String province) {
        this.province = province;
    }

    /** @return the province. */
    public String getProvince() {
        return province;
    }

    /** @return the gold. */
    public Integer getGold() {
        return gold;
    }

    /** @param gold the gold to set. */
    public void setGold(Integer gold) {
        this.gold = gold;
    }
}
