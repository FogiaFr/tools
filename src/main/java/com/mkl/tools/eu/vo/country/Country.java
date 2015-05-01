package com.mkl.tools.eu.vo.country;

import java.util.ArrayList;
import java.util.List;

/**
 * Inner class describing a country.
 *
 * @author MKL.
 */
public class Country {
    /** Name of the country. */
    private String name;
    /** Short label of the country. */
    private String shortLabel;
    /** Long label of the country. */
    private String longLabel;
    /** Type of the country. */
    private String type;
    /** Religion at start of the country. */
    private String religion;
    /** Flag saying that the country is part of HRE. */
    private Boolean hre;
    /** Flag saying that the country is an elector of the HRE. */
    private Boolean elector;
    /** Fidelity of this country (high value means it will stay on diplomatic track. */
    private int fidelity;
    /** Capitals of the country (may be empty). */
    private List<String> capitals = new ArrayList<>();
    /** Provinces of the country. */
    private List<String> provinces = new ArrayList<>();
    /** Army class of this country. */
    private String armyClass;

    /*********************************************************************************************************
     *                       Diplomatic track  (null for unreachable box)                                      *
     *********************************************************************************************************/
    /** Dowry paid or received by a major country concluding a royal marriage with this country. */
    private Integer royalMarriage;
    /** Subsidies paid or received by a major country concluding a subsidies with this country (100 - subsidies earned). */
    private Integer subsidies;
    /** Number of boxes from previous state to enable a military alliance with this country. */
    private Integer militaryAlliance;
    /** Number of boxes from previous state to enable an expeditionary corps with this country. */
    private Integer expCorps;
    /** Number of boxes from previous state to enable an entry in war with this country. */
    private Integer entryInWar;
    /** Number of boxes from previous state to enable a vassalisation of this country. */
    private Integer vassal;
    /** Number of boxes from previous state to enable an annexation of this country. */
    private Integer annexion;

    /**
     * Constructor.
     *
     * @param type of the country.
     */
    public Country(String type) {
        this.type = type;
    }

    /** @return the name. */
    public String getName() {
        return name;
    }

    /** @param name the name to set. */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the shortLabel. */
    public String getShortLabel() {
        return shortLabel;
    }

    /** @param shortLabel the shortLabel to set. */
    public void setShortLabel(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    /** @return the longLabel. */
    public String getLongLabel() {
        return longLabel;
    }

    /** @param longLabel the longLabel to set. */
    public void setLongLabel(String longLabel) {
        this.longLabel = longLabel;
    }

    /** @return the type. */
    public String getType() {
        return type;
    }

    /** @param type the type to set. */
    public void setType(String type) {
        this.type = type;
    }

    /** @return the religion. */
    public String getReligion() {
        return religion;
    }

    /** @param religion the religion to set. */
    public void setReligion(String religion) {
        switch (religion) {
            case "chiite":
                this.religion = "SHIITE";
                break;
            case "sunnite":
                this.religion = "SUNNITE";
                break;
            case "catholique":
                this.religion = "CATHOLIC";
                break;
            case "protestant":
                this.religion = "PROTESTANT";
                break;
            case "cathoprote":
            case "protecatho":
                this.religion = "CATHO_PROT";
                break;
            case "orthodoxe":
                this.religion = "ORTHODOX";
                break;
            case "autrereligion":
                this.religion = "OTHER";
                break;
            default:
                throw new RuntimeException(religion);
        }
    }

    /** @return the hre. */
    public Boolean isHre() {
        return hre;
    }

    /** @param hre the hre to set. */
    public void setHre(Boolean hre) {
        this.hre = hre;
    }

    /** @return the elector. */
    public Boolean isElector() {
        return elector;
    }

    /** @param elector the elector to set. */
    public void setElector(Boolean elector) {
        this.elector = elector;
    }

    /** @return the fidelity. */
    public int getFidelity() {
        return fidelity;
    }

    /** @param fidelity the fidelity to set. */
    public void setFidelity(int fidelity) {
        this.fidelity = fidelity;
    }

    /** @return the capital. */
    public List<String> getCapitals() {
        return capitals;
    }

    /** @return the provinces. */
    public List<String> getProvinces() {
        return provinces;
    }

    /** @return the armyClass. */
    public String getArmyClass() {
        return armyClass;
    }

    /** @param armyClass the armyClass to set. */
    public void setArmyClass(String armyClass) {
        this.armyClass = armyClass;
    }

    /** @return the royalMarriage. */
    public Integer getRoyalMarriage() {
        return royalMarriage;
    }

    /** @param royalMarriage the royalMarriage to set. */
    public void setRoyalMarriage(Integer royalMarriage) {
        this.royalMarriage = royalMarriage;
    }

    /** @return the subsidies. */
    public Integer getSubsidies() {
        return subsidies;
    }

    /** @param subsidies the subsidies to set. */
    public void setSubsidies(Integer subsidies) {
        this.subsidies = subsidies;
    }

    /** @return the militaryAlliance. */
    public Integer getMilitaryAlliance() {
        return militaryAlliance;
    }

    /** @param militaryAlliance the militaryAlliance to set. */
    public void setMilitaryAlliance(Integer militaryAlliance) {
        this.militaryAlliance = militaryAlliance;
    }

    /** @return the expCorps. */
    public Integer getExpCorps() {
        return expCorps;
    }

    /** @param expCorps the expCorps to set. */
    public void setExpCorps(Integer expCorps) {
        this.expCorps = expCorps;
    }

    /** @return the entryInWar. */
    public Integer getEntryInWar() {
        return entryInWar;
    }

    /** @param entryInWar the entryInWar to set. */
    public void setEntryInWar(Integer entryInWar) {
        this.entryInWar = entryInWar;
    }

    /** @return the vassal. */
    public Integer getVassal() {
        return vassal;
    }

    /** @param vassal the vassal to set. */
    public void setVassal(Integer vassal) {
        this.vassal = vassal;
    }

    /** @return the annexion. */
    public Integer getAnnexion() {
        return annexion;
    }

    /** @param annexion the annexion to set. */
    public void setAnnexion(Integer annexion) {
        this.annexion = annexion;
    }
}
