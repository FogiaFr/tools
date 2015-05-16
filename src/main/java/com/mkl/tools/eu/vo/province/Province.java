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
    /** Additional info for european province. */
    private ProvinceInfo info;
    /** Additional info for rotw province. */
    private RotwInfo rotwInfo;
    /** Additional info for sea zone. */
    private SeaInfo seaInfo;
    /** Additional info for trade zone. */
    private TradeZone tradeInfo;
    /** Portions of the province. */
    private List<SubProvince> portions = new ArrayList<>();
    /** Restructuring of the coordinates for the geo.json export. */
    private List<Pair<List<List<Pair<Double, Double>>>, Boolean>> coords;
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
        if (name.contains("~")) {
            setRotwInfo(new RotwInfo());
            getRotwInfo().setRegion(name.substring(1, name.indexOf('~')));
        }
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

    /** @return the rotwInfo. */
    public RotwInfo getRotwInfo() {
        return rotwInfo;
    }

    /** @param rotwInfo the rotwInfo to set. */
    public void setRotwInfo(RotwInfo rotwInfo) {
        this.rotwInfo = rotwInfo;
    }

    /** @return the seaInfo. */
    public SeaInfo getSeaInfo() {
        return seaInfo;
    }

    /** @param seaInfo the seaInfo to set. */
    public void setSeaInfo(SeaInfo seaInfo) {
        this.seaInfo = seaInfo;
    }

    /** @return the tradeInfo. */
    public TradeZone getTradeInfo() {
        return tradeInfo;
    }

    /** @param tradeInfo the tradeInfo to set. */
    public void setTradeInfo(TradeZone tradeInfo) {
        this.tradeInfo = tradeInfo;
    }

    /** @return the portions. */
    public List<SubProvince> getPortions() {
        return portions;
    }

    /** @return the coords. */
    public List<Pair<List<List<Pair<Double, Double>>>, Boolean>> getCoords() {
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
                    terrain = "DESERT";
                    break;
                case "foret":
                    terrain = "DENSE_FOREST";
                    break;
                case "foreto":
                    terrain = "SPARSE_FOREST";
                    break;
                case "marais":
                    terrain = "SWAMP";
                    break;
                case "mer":
                    terrain = "SEA";
                    break;
                case "monts":
                    terrain = "MOUNTAIN";
                    break;
                case "plaine":
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