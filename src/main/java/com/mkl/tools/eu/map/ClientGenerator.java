package com.mkl.tools.eu.map;

import com.mkl.tools.eu.util.ToolsUtil;
import com.mkl.tools.eu.vo.province.*;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class that gather all injection data for client.
 *
 * @author MKL.
 */
public class ClientGenerator {

    /**
     * Create the geo.json file used by the application.
     *
     * @param provinces data gathered by the input.
     * @param log       log writer.
     * @throws Exception exception.
     */

    public static void createMapData(Map<String, Province> provinces, Writer log) throws Exception {
        Writer writer = ToolsUtil.createFileWriter("src/main/resources/output/countries.geo.json", false);
        writer.append("{\"type\":\"FeatureCollection\",\"features\":[\n");
        boolean first = true;

        for (String prov : provinces.keySet()) {
            Province province = provinces.get(prov);
            if (!first) {
                writer.append(",\n");
            } else {
                first = false;
            }
            writer.append("    {\"type\":\"Feature\",\"properties\":{\"terrain\":\"").append(province.getTerrain())
                    .append("\"");

            if (!province.getPortions().get(0).isRotw()) {
                writer.append(",\"rotw\":\"false\"");
            } else {
                writer.append(",\"rotw\":\"true\"");
            }

            if (province.getInfo() != null) {
                writer
                        .append(",\"income\":\"").append(Integer.toString(province.getInfo().getIncome())).append("\"")
                        .append(",\"owner\":\"").append(province.getInfo().getDefaultOwner()).append("\"")
                        .append(",\"fortress\":\"").append(Integer.toString(province.getInfo().getFortress())).append("\"")
                        .append(",\"capital\":\"").append(Boolean.toString(province.getInfo().isCapital())).append("\"")
                        .append(",\"port\":\"").append(Boolean.toString(province.getInfo().isPort())).append("\"")
                        .append(",\"arsenal\":\"").append(Boolean.toString(province.getInfo().isArsenal())).append("\"")
                        .append(",\"praesidiable\":\"").append(Boolean.toString(province.getInfo().isPraesidiable())).append("\"")
                        .append(",\"metadata\":\"").append(String.join(";;", province.getInfo().getMetadata(province.getName()))).append("\"");

                if (province.getInfo().getX() != null) {
                    double coordinate = getXMapCoordinate(province.getInfo().getX(), province.getPortions().get(0).isRotw());
                    writer.append(",\"xFortress\":\"").append(Double.toString(coordinate)).append("\"");
                }
                if (province.getInfo().getY() != null) {
                    double coordinate = getYMapCoordinate(province.getInfo().getY(), province.getPortions().get(0).isRotw());
                    writer.append(",\"yFortress\":\"").append(Double.toString(coordinate)).append("\"");
                }
                if (province.getInfo().getXPort() != null) {
                    double coordinate = getXMapCoordinate(province.getInfo().getXPort(), province.getPortions().get(0).isRotw());
                    writer.append(",\"xPort\":\"").append(Double.toString(coordinate)).append("\"");
                }
                if (province.getInfo().getYPort() != null) {
                    double coordinate = getYMapCoordinate(province.getInfo().getYPort(), province.getPortions().get(0).isRotw());
                    writer.append(",\"yPort\":\"").append(Double.toString(coordinate)).append("\"");
                }
            } else if (province.getRotwInfo() != null) {
                writer.append(",\"region\":\"").append(province.getRotwInfo().getRegion()).append("\"");
                if (province.getRotwInfo().getFortress() != null) {
                    writer.append(",\"fortress\":\"").append(Integer.toString(province.getRotwInfo().getFortress())).append("\"")
                            .append(",\"metadata\":\"").append(String.join(";;", province.getRotwInfo().getMetadata())).append("\"");
                }
            } else if (province.getSeaInfo() != null) {
                writer
                        .append(",\"difficulty\":\"").append(Integer.toString(province.getSeaInfo().getDifficulty())).append("\"")
                        .append(",\"penalty\":\"").append(Integer.toString(province.getSeaInfo().getPenalty())).append("\"");
            } else if (province.getTradeInfo() != null) {
                writer
                        .append(",\"type\":\"").append(province.getTradeInfo().getType()).append("\"")
                        .append(",\"country\":\"").append(province.getTradeInfo().getCountryName()).append("\"")
                        .append(",\"monopoly\":\"").append(Integer.toString(province.getTradeInfo().getMonopoly())).append("\"")
                        .append(",\"presence\":\"").append(Integer.toString(province.getTradeInfo().getPresence())).append("\"");
            }

            writer.append("},\"geometry\":{\"type\":\"");
            if (province.getCoords().size() == 1) {
                writer.append("Polygon");
            } else if (province.getCoords().size() > 1) {
                writer.append("MultiPolygon");
            } else {
                log.append(province.getName()).append("\t").append("No border.").append("\n");
            }
            writer.append("\",\"coordinates\":[");

            if (province.getCoords().size() == 1) {
                writePolygone(province.getCoords().get(0), writer);
            } else {
                boolean firstPolygon = true;
                for (Pair<List<List<Pair<Double, Double>>>, Boolean> polygon : province.getCoords()) {
                    if (!firstPolygon) {
                        writer.append(", ");
                    } else {
                        firstPolygon = false;
                    }
                    writer.append("[");

                    writePolygone(polygon, writer);

                    writer.append("]");
                }
            }

            writer.append("]},\"id\":\"").append(province.getName()).append("\"}");
        }

        writer.append("\n]}");

        writer.flush();
        writer.close();
    }

    /**
     * Write a polygon in a geo.json format.
     *
     * @param polygons List of coordinates of the polygons.
     * @param writer   File Writer.
     * @throws Exception exception.
     */
    private static void writePolygone(Pair<List<List<Pair<Double, Double>>>, Boolean> polygons, Writer writer) throws Exception {
        boolean firstPolygon = true;
        for (List<Pair<Double, Double>> polygon : polygons.getLeft()) {
            if (!firstPolygon) {
                writer.append(", ");
            } else {
                firstPolygon = false;
            }
            writer.append("[");


            boolean firstCoord = true;
            for (Pair<Double, Double> coord : polygon) {
                if (!firstCoord) {
                    writer.append(", ");
                } else {
                    firstCoord = false;
                }
                double x = getXMapCoordinate(coord.getLeft(), polygons.getRight());
                double y = getYMapCoordinate(coord.getRight(), polygons.getRight());
                writer.append("[").append(Double.toString(x)).append(", ").append(Double.toString(y)).append("]");
            }

            writer.append("]");
        }
    }

    /**
     * Transform a client x coordinate to a map x coordinate.
     *
     * @param xCoordinate client x coordinate.
     * @param rotw        flag saying that the coordinate is in the rotw map.
     * @return the map x coordinate.
     */
    private static double getXMapCoordinate(double xCoordinate, boolean rotw) {
        double x;
        if (rotw) {
            x = 4.659 + xCoordinate * 12.484 / 8183;
        } else {
            x = 4.658 + xCoordinate * 12.859 / 8425;
        }

        return x;
    }

    /**
     * Transform a client y coordinate to a map y coordinate.
     *
     * @param yCoordinate client y coordinate.
     * @param rotw        flag saying that the coordinate is in the rotw map.
     * @return the map y coordinate.
     */
    private static double getYMapCoordinate(double yCoordinate, boolean rotw) {
        double y;
        /*
            The map is linear on the X axis but not on the Y axis.
            I have no idea of the real function on the Y axis.
         */
        if (rotw) {
            double factor = 0.001499 - 0.00000519 * (yCoordinate / 1000);
            y = 11.409 + yCoordinate * factor;
        } else {
            double factor = 0.001525 - 0.00000179 * (yCoordinate / 1000);
            y = 2.109 + yCoordinate * factor;
        }

        return y;
    }

    /**
     * Create provinces neighbour file used by the application.
     *
     * @param borders        existing borders.
     * @param provinces      data gathered by the input.
     * @param specialBorders rivers, moutain passes and straits.
     * @param log            log writer.
     * @throws Exception exception.
     */
    public static void createBorderData(List<Border> borders, Map<String, Province> provinces, Map<String, List<Path>> specialBorders, Writer log) throws Exception {
        Map<Path, List<Province>> provincesByPath = new HashMap<>();
        for (Province province : provinces.values()) {
            for (SubProvince subProvince : province.getPortions()) {
                for (DirectedPath path : subProvince.getPaths()) {
                    if (!provincesByPath.containsKey(path.getPath())) {
                        provincesByPath.put(path.getPath(), new ArrayList<>());
                    }

                    List<Province> provincesForPath = provincesByPath.get(path.getPath());
                    if (!provincesForPath.contains(province)) {
                        provincesForPath.add(province);
                    }
                }
            }
        }

        createBorders(borders, provincesByPath, specialBorders, log);

        XStream xstream = new XStream();
        xstream.processAnnotations(Border.class);

        Writer borderWriter = ToolsUtil.createFileWriter("src/main/resources/output/borders.xml", false);
        xstream.toXML(borders, borderWriter);
    }

    /**
     * Create borders object from arranged provinces by paths.
     *
     * @param borders         existing borders.
     * @param provincesByPath provinces arranged by paths.
     * @param specialBorders  rivers, moutain passes and straits.
     * @param log             log writer.
     * @throws IOException exception.
     */
    private static void createBorders(List<Border> borders, Map<Path, List<Province>> provincesByPath, Map<String, List<Path>> specialBorders, Writer log) throws IOException {
        for (Path path : provincesByPath.keySet()) {
            List<Province> provincesForPath = provincesByPath.get(path);
            if (path.getName().contains("bord")) {
                continue;
            }

            for (int i = 0; i < provincesForPath.size(); i++) {
                for (int j = i + 1; j < provincesForPath.size(); j++) {
                    Province first = provincesForPath.get(i);
                    Province second = provincesForPath.get(j);

                    if (first != second) {
                        String type = null;
                        for (String specialType : specialBorders.keySet()) {
                            if (specialBorders.get(specialType).contains(path)) {
                                type = specialType;
                            }
                        }

                        // no special border between heterogeneous terrains
                        if ((StringUtils.equals("SEA", first.getTerrain()) && second.getTerrain() != null && !StringUtils.equals("SEA", second.getTerrain()))
                                || (StringUtils.equals("SEA", second.getTerrain()) && first.getTerrain() != null && !StringUtils.equals("SEA", first.getTerrain()))) {
                            type = null;
                        }

                        Border border = new Border(first, second, type);
                        if (borders.contains(border)) {
                            Border existingBorder = borders.get(borders.indexOf(border));
                            if (!StringUtils.equals(border.getType(), existingBorder.getType())) {
                                log.append(first.getName()).append("\t").append("Duplicate borders").append("\t")
                                        .append(second.getName()).append("\n");
                            }

                            // if double and heterogeneous types, we take the worst one.
                            if (existingBorder.getType() == null) {
                                existingBorder.setType(border.getType());
                            }
                        } else {
                            borders.add(border);
                        }
                    }
                }
            }
        }
    }
}
