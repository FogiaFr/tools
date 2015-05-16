package com.mkl.tools.eu.vo.province;

/**
 * Additional information on a sea zone.
 *
 * @author MKL
 */
public class SeaInfo {
    /** Difficulty of the sea zone (positive). */
    private int difficulty;
    /** Penalty of the sea zone (positive or 0). */
    private int penalty;

    /** @return the difficulty. */
    public int getDifficulty() {
        return difficulty;
    }

    /** @param difficulty the difficulty to set. */
    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    /** @return the penalty. */
    public int getPenalty() {
        return penalty;
    }

    /** @param penalty the penalty to set. */
    public void setPenalty(int penalty) {
        this.penalty = penalty;
    }
}