package com.mkl.tools.eu.vo.province;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Inner class describing a province.
 *
 * @author MKL
 */
public class Province {
    /** The name of the province. */
    private String name;
    /** Terrain derived from the portions. */
    private String terrain;
    /** Additional info. */
    private ProvinceInfo info;
    /** Portions of the province. */
    private List<SubProvince> portions = new ArrayList<>();
    /** Restructuring of the coordinates for the geo.json export. */
    private List<Pair<List<List<Pair<Integer, Integer>>>, Boolean>> coords;
    /** Log writer. */
    private Writer log;

    /**
     * Constructor.
     *
     * @param name of the province.
     * @param log  log writer.
     */
    public Province(String name, Writer log) {
        this.name = name;
        this.log = log;
    }

    /** @return the name. */
    public String getName() {
        return name;
    }

    /** @return the terrain. */
    public String getTerrain() {
        return terrain;
    }

    /** @return the info. */
    public ProvinceInfo getInfo() {
        return info;
    }

    /** @param info the info to set. */
    public void setInfo(ProvinceInfo info) {
        this.info = info;
    }

    /** @return the portions. */
    public List<SubProvince> getPortions() {
        return portions;
    }

    /** @return the coords. */
    public List<Pair<List<List<Pair<Integer, Integer>>>, Boolean>> getCoords() {
        return coords;
    }

    /**
     * Generates the restructurated coords of the province.
     *
     * @throws Exception exception.
     */
    public void restructure() throws Exception {
        coords = new ArrayList<>();
        for (SubProvince portion : portions) {
            coords.add(portion.getStructuratedCoords(this, log));
            if (terrain == null && !StringUtils.equals("lac", portion.getTerrain()) && portion.getTerrain() != null && !portion.getTerrain().startsWith("europe")) {
                terrain = portion.getTerrain();
            } else if (!StringUtils.equals(terrain, portion.getTerrain()) && !StringUtils.equals("lac", portion.getTerrain())
                    && portion.getTerrain() != null && !portion.getTerrain().startsWith("europe")) {
                log.append(getName()).append("\t").append("Terrain not consistent").append("\t").append(terrain).append("\t").append(portion.getTerrain()).append("\n");
            }
        }

        convertTerrain();
    }

    /**
     * Convert the terrain to the value of TerrainEnum.
     */
    private void convertTerrain() {
        if (terrain != null) {
            switch (terrain) {
                case "desert":
                case "kdesert":
                    terrain = "DESERT";
                    break;
                case "foret":
                case "kforet":
                    terrain = "DENSE_FOREST";
                    break;
                case "foreto":
                    terrain = "SPARSE_FOREST";
                    break;
                case "marais":
                case "kmarais":
                    terrain = "SWAMP";
                    break;
                case "mer":
                    terrain = "SEA";
                    break;
                case "monts":
                case "kmonts":
                    terrain = "MOUNTAIN";
                    break;
                case "plaine":
                case "kplaine":
                    terrain = "PLAIN";
                    break;
                default:
                    break;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        boolean equals = false;

        if (obj instanceof Province) {
            equals = StringUtils.equals(name, ((Province) obj).getName());
        }

        return equals;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}