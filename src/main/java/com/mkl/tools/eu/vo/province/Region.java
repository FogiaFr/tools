package com.mkl.tools.eu.vo.province;

import java.util.ArrayList;
import java.util.List;

/**
 * Region (group of rotw provinces).
 *
 * @author MKL
 */
public class Region {
    /** Name of the region. */
    private String name;
    /** Number of province in this region. */
    private int number;
    /** Income of the region. */
    private int income;
    /** Difficulty of the region. */
    private int difficulty;
    /** Tolerance of the region. */
    private int tolerance;
    /** Number of natives in each province of the region. */
    private int nativesNumber;
    /** Type of natives in each province of the region. */
    private String nativesType;
    /** List of resources of the region. */
    private List<Resources> resources = new ArrayList<>();
    /** Penalty for cold area. <code>null</code> for no cold area. */
    private Integer coldArea;

    /**
     * Constructor.
     *
     * @param name the name to set.
     */
    public Region(String name) {
        this.name = name;
    }

    /** @return the name. */
    public String getName() {
        return name;
    }

    /** @return the number. */
    public int getNumber() {
        return number;
    }

    /** @param number the number to set. */
    public void setNumber(int number) {
        this.number = number;
    }

    /** @return the income. */
    public int getIncome() {
        return income;
    }

    /** @param income the income to set. */
    public void setIncome(int income) {
        this.income = income;
    }

    /** @return the difficulty. */
    public int getDifficulty() {
        return difficulty;
    }

    /** @param difficulty the difficulty to set. */
    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    /** @return the tolerance. */
    public int getTolerance() {
        return tolerance;
    }

    /** @param tolerance the tolerance to set. */
    public void setTolerance(int tolerance) {
        this.tolerance = tolerance;
    }

    /** @return the nativesNumber. */
    public int getNativesNumber() {
        return nativesNumber;
    }

    /** @param nativesNumber the nativesNumber to set. */
    public void setNativesNumber(int nativesNumber) {
        this.nativesNumber = nativesNumber;
    }

    /** @return the nativesType. */
    public String getNativesType() {
        return nativesType;
    }

    /** @param nativesType the nativesType to set. */
    public void setNativesType(String nativesType) {
        this.nativesType = nativesType;
    }

    /** @return the resources. */
    public List<Resources> getResources() {
        return resources;
    }

    /** @return the coldArea. */
    public Integer getColdArea() {
        return coldArea;
    }

    /** @param coldArea the coldArea to set. */
    public void setColdArea(Integer coldArea) {
        this.coldArea = coldArea;
    }

    /**
     * Add a ressource to the region.
     *
     * @param name   of the ressource.
     * @param number of the ressource.
     */
    public void addRessource(String name, int number) {
        getResources().add(new Resources(name, number));
    }

    /** Inner class describing the resources of a region. */
    public static class Resources {
        /** Name of the resource. */
        private String name;
        /** Number of the resource. */
        private int number;

        /**
         * Constructor.
         *
         * @param name   the name to set.
         * @param number the number to set.
         */
        public Resources(String name, int number) {
            this.name = name;
            this.number = number;
        }

        /** @return the name. */
        public String getName() {
            return name;
        }

        /** @return the number. */
        public int getNumber() {
            return number;
        }
    }
}