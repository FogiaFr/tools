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
    /** Cultural group of this country. */
    private String culture;
    /** Flag saying that the country is part of HRE. */
    private Boolean hre;
    /** Flag saying that the country is an elector of the HRE. */
    private Boolean elector;
    /** Fidelity of this country (high value means it will stay on diplomatic track. */
    private int fidelity;
    /** Basic forces of the country. */
    private List<Limit> basicForces = new ArrayList<>();
    /** Reinforcements of the country. */
    private List<Limit> reinforcements = new ArrayList<>();
    /** Forces of the country. */
    private List<Limit> limits = new ArrayList<>();
    /** Geopolitics preference to another country (name of the major country). */
    private String preference;
    /** Bonus of the geopolitics preference. */
    private Integer preferenceBonus;
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

    /** @return the culture. */
    public String getCulture() {
        return culture;
    }

    /** @param culture the culture to set. */
    public void setCulture(String culture) {
        switch (culture) {
            case "Latin":
                this.culture = "LATIN";
                break;
            case "Orthodoxe":
                this.culture = "ORTHODOX";
                break;
            case "Islam":
                this.culture = "ISLAM";
                break;
            case "Amerique":
                this.culture = "MEDIEVAL";
                break;
            case "ROTW":
                this.culture = "ROTW";
                break;
            default:
                throw new RuntimeException(culture);
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

    /** @return the limits. */
    public List<Limit> getLimits() {
        return limits;
    }

    /**
     * Add a limit.
     *
     * @param number of the limit.
     * @param type   of the limit.
     */
    public void addLimit(int number, String type) {
        String typeCounter = type;
        switch (type) {
            case "corsaire":
                typeCounter = "PIRATE";
            case "ARMY":
            case "ARMY_TIMAR":
            case "FLEET":
            case "FLEET_TRANSPORT":
            case "LDND":
            case "LDND_TIMAR":
            case "LD":
            case "LD_TIMAR":
            case "LDENDE":
            case "LDE":
            case "NDE":
            case "NTD":
            case "PIRATE":
            case "TP":
            case "COL":
            case "TF":
            case "FORT12":
            case "FORT23":
            case "FORT34":
            case "FORT45":
            case "FORT":
            case "ARS23":
            case "ARS23_GIBRALTAR":
            case "ARS34":
            case "MISSION":
            case "SEPOY":
            case "SEPOY_EXPLORATION":
            case "INDIAN":
            case "INDIAN_EXPLORATION":
            case "ROTW_DIPLO":
            case "MNU_ART":
            case "MNU_CEREALS":
            case "MNU_CLOTHES":
            case "MNU_FISH":
            case "MNU_INSTRUMENTS":
            case "MNU_METAL":
            case "MNU_METAL_SCHLESIEN":
            case "MNU_SALT":
            case "MNU_WINE":
            case "MNU_WOOD":
                limits.add(new Limit(number, typeCounter));
                break;
            default:
                throw new RuntimeException(type);
        }
    }

    /** @return the basicForces. */
    public List<Limit> getBasicForces() {
        return basicForces;
    }

    /**
     * Add a basic force.
     *
     * @param number of the force.
     * @param type   of the force.
     */
    public void addBasicForce(int number, String type) {
        basicForces.add(createForce(number, type));
    }

    /** @return the reinforcements. */
    public List<Limit> getReinforcements() {
        return reinforcements;
    }

    /**
     * Add a reinforcements.
     *
     * @param number of the force.
     * @param type   of the force.
     */
    public void addReinforcements(int number, String type) {
        reinforcements.add(createForce(number, type));
    }

    /**
     * Create a force (basic force or reinforcements).
     *
     * @param number of the force.
     * @param type   of the force.
     * @return the force.
     */
    public Limit createForce(int number, String type) {
        String typeCounter;
        switch (type) {
            case "\\ARMY\\faceplus":
                typeCounter = "ARMY_PLUS";
                break;
            case "\\ARMY\\facemoins":
                typeCounter = "ARMY_MINUS";
                break;
            case "\\FLEET\\faceplus":
                typeCounter = "FLEET_PLUS";
                break;
            case "\\FLEET\\facemoins":
                typeCounter = "FLEET_MINUS";
                break;
            case "\\LD":
                typeCounter = "LD";
                break;
            case "\\ND":
            case "\\NTD":
            case "\\VGD":
                typeCounter = "ND";
                break;
            case "\\corsaire\\faceplus":
                typeCounter = "P_PLUS";
                break;
            case "\\corsaire\\facemoins":
                typeCounter = "P_MINUS";
                break;
            case "\\LDND":
                typeCounter = "LDND";
                break;
            case "\\NDE":
                typeCounter = "DE";
                break;
            case "\\fortress":
                typeCounter = "F";
                break;
            case "MC":
                typeCounter = "MC";
                break;
            case "\\LeaderG":
                typeCounter = "LG";
                break;
            case "\\LeaderA":
                typeCounter = "LA";
                break;
            case "LE":
                typeCounter = "LE";
                break;
            case "LC":
                typeCounter = "LC";
                break;
            case "\\LeaderG\\(king)":
            case "\\leader{Caliph}":
            case "\\leader{Giray}":
            case "\\leader{Shah}":
            case "\\leader{ArabAdmiralO}":
                typeCounter = "LK";
                break;
            default:
                throw new RuntimeException(type);
        }
        return new Limit(number, typeCounter);
    }

    /** @return the preference. */
    public String getPreference() {
        return preference;
    }

    /** @param preference the preference to set. */
    public void setPreference(String preference) {
        this.preference = preference;
    }

    /** @return the preferenceBonus. */
    public Integer getPreferenceBonus() {
        return preferenceBonus;
    }

    /** @param preferenceBonus the preferenceBonus to set. */
    public void setPreferenceBonus(Integer preferenceBonus) {
        this.preferenceBonus = preferenceBonus;
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
        switch (armyClass) {
            case "I":
            case "IM":
            case "II":
            case "IIM":
            case "III":
            case "IIIM":
            case "IV":
            case "IVM":
            case "A":
                this.armyClass = armyClass;
                break;
            default:
                throw new RuntimeException(armyClass);
        }
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

    /** Limit of a country. */
    public class Limit {
        /** Number of this type of counter. */
        private Integer number;
        /** Type of force. */
        private String type;

        /**
         * Constructor.
         *
         * @param number the number.
         * @param type   the type.
         */
        private Limit(Integer number, String type) {
            this.number = number;
            this.type = type;
        }

        /** @return the number. */
        public Integer getNumber() {
            return number;
        }

        /** @return the type. */
        public String getType() {
            return type;
        }
    }
}
