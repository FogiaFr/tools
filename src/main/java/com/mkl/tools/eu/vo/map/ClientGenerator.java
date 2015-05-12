package com.mkl.tools.eu.vo.map;

import com.mkl.tools.eu.util.ToolsUtil;
import com.mkl.tools.eu.vo.country.Country;
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
     * @param provinces    data gathered by the input.
     * @param specialBoxes special boxes on the maps.
     * @param log          log writer.
     * @throws Exception exception.
     */

    public static void createMapData(Map<String, Province> provinces, Map<String, Province> specialBoxes, Writer log) throws Exception {
        Writer writer = ToolsUtil.createFileWriter("src/main/resources/output/countries.geo.json", false);
        writer.append("{\"type\":\"FeatureCollection\",\"features\":[\n");
        boolean first = true;

        for (String prov : provinces.keySet()) {
            Province province = provinces.get(prov);
            province.restructure();
            if (!first) {
                writer.append(",\n");
            } else {
                first = false;
            }
            writer.append("    {\"type\":\"Feature\",\"properties\":{\"terrain\":\"").append(province.getTerrain())
                    .append("\"");

            if (province.getInfo() != null) {
                writer.append(",\"rotw\":\"false\"")
                        .append(",\"income\":\"").append(Integer.toString(province.getInfo().getIncome())).append("\"")
                        .append(",\"fortress\":\"").append(Integer.toString(province.getInfo().getFortress())).append("\"")
                        .append(",\"capital\":\"").append(Boolean.toString(province.getInfo().isCapital())).append("\"")
                        .append(",\"port\":\"").append(Boolean.toString(province.getInfo().isPort())).append("\"")
                        .append(",\"arsenal\":\"").append(Boolean.toString(province.getInfo().isArsenal())).append("\"")
                        .append(",\"praesidiable\":\"").append(Boolean.toString(province.getInfo().isPraesidiable())).append("\"")
                        .append(",\"metadata\":\"").append(String.join(";;", province.getInfo().getMetadata(province.getName()))).append("\"");
            } else if (!province.getPortions().get(0).isRotw()) {
                writer.append(",\"rotw\":\"false\"");
            } else {
                writer.append(",\"rotw\":\"true\"");
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
                for (Pair<List<List<Pair<Integer, Integer>>>, Boolean> polygon : province.getCoords()) {
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

        for (String prov : specialBoxes.keySet()) {
            Province province = specialBoxes.get(prov);
            province.restructure();
            writer.append(",\n");
            writer.append("    {\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[");

            if (province.getCoords().size() == 1) {
                writePolygone(province.getCoords().get(0), writer);
            } else {
                boolean firstPolygon = true;
                for (Pair<List<List<Pair<Integer, Integer>>>, Boolean> polygon : province.getCoords()) {
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
    private static void writePolygone(Pair<List<List<Pair<Integer, Integer>>>, Boolean> polygons, Writer writer) throws Exception {
        boolean firstPolygon = true;
        for (List<Pair<Integer, Integer>> polygone : polygons.getLeft()) {
            if (!firstPolygon) {
                writer.append(", ");
            } else {
                firstPolygon = false;
            }
            writer.append("[");


            boolean firstCoord = true;
            for (Pair<Integer, Integer> coord : polygone) {
                if (!firstCoord) {
                    writer.append(", ");
                } else {
                    firstCoord = false;
                }
                double x;
                double y;
                if (polygons.getRight()) {
                    x = 4.659 + coord.getLeft() * 12.484 / 8183;
                    y = 11.409 + coord.getRight() * 5.251 / 3546;
                } else {
                    x = 4.658 + coord.getLeft() * 12.859 / 8425;
                    y = 2.109 + coord.getRight() * 8.845 / 5840;
                }
                writer.append("[").append(Double.toString(x)).append(", ").append(Double.toString(y)).append("]");
            }

            writer.append("]");
        }
    }

    /**
     * Create provinces neighbour file used by the application.
     *
     * @param provinces      data gathered by the input.
     * @param specialBorders rivers, moutain passes and straits.
     * @param countries      list of countries.
     * @param log            log writer.
     * @throws Exception exception.
     */
    public static List<Border> createProvincesData(Map<String, Province> provinces, Map<String, List<Path>> specialBorders,
                                                   Map<String, Country> countries, Writer log) throws Exception {
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

        List<Border> borders = createBorders(provincesByPath, specialBorders, log);

        XStream xstream = new XStream();
        xstream.processAnnotations(Border.class);

        Writer borderWriter = ToolsUtil.createFileWriter("src/main/resources/output/borders.xml", false);
        xstream.toXML(borders, borderWriter);

        return borders;
    }

    /**
     * Create borders object from arranged provinces by paths.
     *
     * @param provincesByPath provinces arranged by paths.
     * @param specialBorders  rivers, moutain passes and straits.
     * @param log             log writer.
     * @return the borders.
     * @throws IOException exception.
     */
    private static List<Border> createBorders(Map<Path, List<Province>> provincesByPath, Map<String, List<Path>> specialBorders, Writer log) throws IOException {
        List<Border> borders = new ArrayList<>();
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

                        Border border = new Border(first, second, type);
                        if (borders.contains(border)) {
                            Border existingBorder = borders.get(borders.indexOf(border));
                            if (!StringUtils.equals(border.getType(), existingBorder.getType())) {
                                log.append(first.getName()).append("\t").append("Duplicate borders").append("\t")
                                        .append(second.getName()).append("\n");
                            }
                        } else {
                            borders.add(border);
                        }
                    }
                }
            }
        }

        return borders;
    }
}
