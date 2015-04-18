package com.mkl.tools.eu;


import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to generate various files for the EU Map whose the map in the format .geo.json.
 *
 * @author MKL
 */
public final class MapGenerator {
    /** Size of a square. */
    private static final int SQUARE_SIZE = 113;

    /** No constructor for utility class. */
    private MapGenerator() {

    }

    /**
     * Do all the stuff.
     *
     * @param args no args.
     * @throws Exception exception.
     */
    public static void main(String[] args) throws Exception {
        Writer log = createFileWriter("src/main/resources/log.txt", false);
        Map<String, List<Path>> specialBorders = new HashMap<>();
        Map<String, Province> provinces = new HashMap<>();

        extractPaths(provinces, specialBorders, "input/europe.grid.ps", false, log);
        extractPaths(provinces, specialBorders, "input/rotw.grid.ps", true, log);

        Map<String, Province> provs = new HashMap<>();
        for (String prov : provinces.keySet()) {
            Province province = provinces.get(prov);
            province.restructure();
            if (!StringUtils.isEmpty(province.getTerrain()) && !StringUtils.equals("noman", province.getTerrain())) {
                provs.put(prov, province);
            }
        }

        extractMapData(provs, log);

        extractProvincesData(provs, specialBorders, log);

        log.flush();
        log.close();
    }

    /**
     * Extract the paths data in order to have the provinces shapes and borders.
     *
     * @param provinces      List of provinces shapes.
     * @param specialBorders List of special borders.
     * @param inputFile      Name of the file to parse.
     * @param rotw           flag saying that the file is for the ROTW map.
     * @param log            log writer.
     * @throws IOException exception.
     */
    private static void extractPaths(Map<String, Province> provinces, Map<String, List<Path>> specialBorders, String inputFile, boolean rotw, Writer log) throws IOException {
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(MapGenerator.class.getClassLoader().getResourceAsStream(inputFile)));
        Map<String, Path> paths = new HashMap<>();
        Path currentPath = null;
        List<Path> pathsBorder = null;
        boolean specialBordersParsing = true;
        boolean zoomParsing = false;
        Pattern mer = Pattern.compile("/mer\\d+ beginpath.*");
        Pattern bord = Pattern.compile("/bord\\d+ beginpath.*");
        Pattern path = Pattern.compile("/path\\d+ beginpath.*");
        Pattern multiPath = Pattern.compile("\\s*(/mer|/path)\\d+ .*");
        Pattern square = Pattern.compile("\\s*(\\d{4}) (\\d{4}) \\d\\([^\\)]*\\)\\(([^\\)]*)\\)\\([^\\)]*\\) (true|false) carre\\s*");
        while ((line = reader.readLine()) != null) {
            if (mer.matcher(line).matches() || bord.matcher(line).matches() || path.matcher(line).matches()) {
                currentPath = new Path(line.split(" ")[0], !line.contains("contpath"), rotw);
            } else if (line.startsWith("endpath") && currentPath != null) {
                paths.put(currentPath.getName(), currentPath);
                currentPath = null;
            } else if (currentPath != null) {
                String[] coords = line.split(" ");
                for (int i = 0; i < line.length() - 1; i = i + 2) {
                    try {
                        currentPath.getCoords().add(new ImmutablePair<>(Integer.parseInt(coords[i]), Integer.parseInt(coords[i + 1])));
                    } catch (NumberFormatException e) {
                        break;
                    }
                }
            } else if (line.startsWith("/prov")) {
                addSubProvince(line, provinces, paths, rotw, zoomParsing, log);
            } else if (line.startsWith("%#%% Zoom")) {
                zoomParsing = true;
            } else if (specialBordersParsing && StringUtils.equals("[", line.trim())) {
                pathsBorder = new ArrayList<>();
            } else if (line.startsWith("} if % river / pass / strait")) {
                specialBordersParsing = false;
                pathsBorder = null;
            } else if (line.endsWith("change pathtype to river")) {
                if (!specialBorders.containsKey("river")) {
                    specialBorders.put("river", new ArrayList<>());
                }
                specialBorders.get("river").addAll(pathsBorder);
                pathsBorder = new ArrayList<>();
            } else if (line.endsWith("change pathtype to pass")) {
                if (!specialBorders.containsKey("pass")) {
                    specialBorders.put("pass", new ArrayList<>());
                }
                specialBorders.get("pass").addAll(pathsBorder);
                pathsBorder = new ArrayList<>();
            } else if (line.endsWith("change pathtype to strait")) {
                if (!specialBorders.containsKey("strait")) {
                    specialBorders.put("strait", new ArrayList<>());
                }
                specialBorders.get("strait").addAll(pathsBorder);
                pathsBorder = new ArrayList<>();
            } else if (specialBordersParsing && multiPath.matcher(line).matches()) {
                String[] specialsBorder = line.trim().split(" ");
                for (String specialBorder : specialsBorder) {
                    Path pathBorder = paths.get(specialBorder);
                    if (pathBorder == null) {
                        log.append(specialBorder).append("\tBorder not found\n");
                    } else {
                        pathsBorder.add(pathBorder);
                    }
                }
            } else if (square.matcher(line).matches()) {
                addSquare(line, provinces, paths, rotw, zoomParsing, log);
            }
        }

        reader.close();
    }

    /**
     * Adds a portion to an existing province.
     *
     * @param line        to parse.
     * @param provinces   existing provinces.
     * @param paths       list of paths.
     * @param rotw        flag saying that the subProvince is for the ROTW map.
     * @param zoomParsing flag saying that the subProvince is in a zoom (for Europe map).
     * @param log         log writer.
     * @throws IOException exception.
     */
    private static void addSubProvince(String line, Map<String, Province> provinces, Map<String, Path> paths, boolean rotw, boolean zoomParsing, Writer log) throws IOException {
        Matcher m = Pattern.compile(".*\\((.*)\\) ?ppdef.*").matcher(line);
        if (!m.matches()) {
            return;
        }
        String provinceName = m.group(1);
        boolean secondary = false;
        if (provinceName.startsWith("*")) {
            provinceName = provinceName.substring(1);
            secondary = true;
        }
        if (StringUtils.equals("Caption", provinceName) || StringUtils.equals("Special", provinceName) || StringUtils.isEmpty(provinceName)
                || provinceName.startsWith("Zone") || StringUtils.equals("SaintEmpire", provinceName)
                || StringUtils.equals("zone", line.split(" ")[2])) {
            return;
        }
        Province province = provinces.get(provinceName);
        if (province == null) {
            province = new Province(provinceName, log);
            provinces.put(provinceName, province);
        }
        SubProvince portion = new SubProvince(line.split(" ")[1], secondary, rotw);
        m = Pattern.compile("(/path\\d+ AR?)|(/bord\\d+ AR?)|(/mer\\d+ AR?)|(carre[a-zA-Z]+ AR?)").matcher(line);
        while (m.find()) {
            String string = m.group();
            Path pathFound = paths.get(string.split(" ")[0]);
            if (pathFound != null) {
                portion.getPaths().add(new DirectedPath(pathFound, string.split(" ")[1].contains("R")));
            } else {
                log.append(province.getName()).append("\t").append("Path not found").append("\t").append(string.split(" ")[0]).append("\n");
            }
        }

        if ((zoomParsing && !portion.isLight() && !portion.isSecondary())
                || (!province.getPortions().isEmpty() && province.getPortions().get(0).isSecondary() && !portion.isSecondary())
                || (!province.getPortions().isEmpty() && province.getPortions().get(0).isLight() && !portion.isLight())) {
            province.getPortions().add(0, portion);
        } else {
            province.getPortions().add(portion);
        }
    }

    /**
     * Adds a portion to an existing province.
     *
     * @param line        to parse.
     * @param provinces   existing provinces.
     * @param paths       list of paths.
     * @param rotw        flag saying that the subProvince is for the ROTW map.
     * @param zoomParsing flag saying that the subProvince is in a zoom (for Europe map).
     * @param log         log writer.
     * @throws IOException exception.
     */
    private static void addSquare(String line, Map<String, Province> provinces, Map<String, Path> paths, boolean rotw, boolean zoomParsing, Writer log) throws IOException {
        Matcher m = Pattern.compile("\\s*(\\d{4}) (\\d{4}) \\d\\([^\\)]*\\)\\(([^\\)]*)\\)\\(([^\\)]*)\\) (true|false) carre\\s*").matcher(line);
        if (!m.matches()) {
            return;
        }
        int x = Integer.parseInt(m.group(1)) - SQUARE_SIZE / 2;
        int y = Integer.parseInt(m.group(2)) + 6 - SQUARE_SIZE / 2;
        String provinceName = m.group(3);
        String squareName = "carre" + m.group(4);
        boolean secondary = false;
        if (provinceName.startsWith("*")) {
            provinceName = provinceName.substring(1);
            secondary = true;
        }
        if (StringUtils.equals("Caption", provinceName)) {
            return;
        }
        Province province = provinces.get(provinceName);
        if (province == null) {
            province = new Province(provinceName, log);
            provinces.put(provinceName, province);
        }
        SubProvince portion = new SubProvince("lac", secondary, rotw);
        Path squarePath = new Path(squareName, true, rotw);
        squarePath.getCoords().add(new ImmutablePair<>(x, y));
        squarePath.getCoords().add(new ImmutablePair<>(x + SQUARE_SIZE, y));
        squarePath.getCoords().add(new ImmutablePair<>(x + SQUARE_SIZE, y + SQUARE_SIZE));
        squarePath.getCoords().add(new ImmutablePair<>(x, y + SQUARE_SIZE));
        squarePath.getCoords().add(new ImmutablePair<>(x, y));
        paths.put(squareName, squarePath);
        portion.getPaths().add(new DirectedPath(squarePath, false));


        if ((zoomParsing && !portion.isLight() && !portion.isSecondary())
                || (!province.getPortions().isEmpty() && province.getPortions().get(0).isSecondary() && !portion.isSecondary())
                || (!province.getPortions().isEmpty() && province.getPortions().get(0).isLight() && !portion.isLight())) {
            province.getPortions().add(0, portion);
        } else {
            province.getPortions().add(portion);
        }
    }

    /**
     * Create the geo.json file used by the application.
     *
     * @param provinces data gathered by the input.
     * @param log       log writer.
     * @throws Exception exception.
     */
    private static void extractMapData(Map<String, Province> provinces, Writer log) throws Exception {
        Writer writer = createFileWriter("src/main/resources/output/countries.geo.json", false);
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
                    .append("\"},\"geometry\":{\"type\":\"");
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
        // Military rounds tiles
        int roundXBegin = 1550;
        int roundYBegin = 32;
        for (int i = 0; i <= 5; i++) {
            writeSquare(writer, roundXBegin + 2 * i * SQUARE_SIZE, roundYBegin, SQUARE_SIZE, "MR_W" + i);
            String name = "MR_S" + (i + 1);
            if (i == 5) {
                name = "MR_End";
            }
            writeSquare(writer, roundXBegin + 2 * i * SQUARE_SIZE, roundYBegin + 2 * SQUARE_SIZE, SQUARE_SIZE, name);
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
     * Write a square in a geo.json format.
     *
     * @param writer to write the square in.
     * @param xBegin coordinate x of the square.
     * @param yBegin coordinate y of the square.
     * @param size   size of the square.
     * @param name   name of the square.
     * @throws IOException exception.
     */
    private static void writeSquare(Writer writer, int xBegin, int yBegin, int size, String name) throws IOException {
        writer.append(",\n");
        writer.append("    {\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[");

        double x = 1.204 + xBegin * 12.859 / 8425;
        double y = 1.204 + yBegin * 8.862 / 5840;
        writer.append("[").append(Double.toString(x)).append(", ").append(Double.toString(y)).append("], ");

        x = 1.204 + (xBegin + size) * 12.859 / 8425;
        y = 1.204 + yBegin * 8.862 / 5840;
        writer.append("[").append(Double.toString(x)).append(", ").append(Double.toString(y)).append("], ");

        x = 1.204 + (xBegin + size) * 12.859 / 8425;
        y = 1.204 + (yBegin + size) * 8.862 / 5840;
        writer.append("[").append(Double.toString(x)).append(", ").append(Double.toString(y)).append("], ");

        x = 1.204 + xBegin * 12.859 / 8425;
        y = 1.204 + (yBegin + size) * 8.862 / 5840;
        writer.append("[").append(Double.toString(x)).append(", ").append(Double.toString(y)).append("]");

        writer.append("]]},\"id\":\"").append(name).append("\"}");
    }

    /**
     * Create provinces neighbour file used by the application.
     *
     * @param provinces      data gathered by the input.
     * @param specialBorders rivers, moutain passes and straits.
     * @param log            log writer.
     * @throws Exception exception.
     */
    private static void extractProvincesData(Map<String, Province> provinces, Map<String, List<Path>> specialBorders, Writer log) throws Exception {
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

        Writer borderWriter = createFileWriter("src/main/resources/output/borders.xml", false);
        xstream.toXML(borders, borderWriter);

        createDBInjection(provinces, borders, log);
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

    /**
     * Create a SQL injection script for provinces and borders.
     *
     * @param provinces list of provinces.
     * @param borders   list of borders.
     * @param log       log writer.
     * @throws IOException exception.
     */
    private static void createDBInjection(Map<String, Province> provinces, List<Border> borders, Writer log) throws IOException {
        Writer sqlWriter = createFileWriter("src/main/resources/output/provinces_borders.sql", false);

        sqlWriter.append("DELETE FROM BORDER;\n").append("DELETE FROM PROVINCE_EU;\n")
                .append("DELETE FROM PROVINCE;\n\n");

        for (Province province : provinces.values()) {
            sqlWriter.append("INSERT INTO PROVINCE (NAME, TERRAIN)\n")
                    .append("    VALUES ('").append(province.getName())
                    .append("', '").append(province.getTerrain())
                    .append("');\n");
        }

        sqlWriter.append("\n");

        for (Border border : borders) {
            sqlWriter.append("INSERT INTO BORDER (TYPE, ID_PROVINCE_FROM, ID_PROVINCE_TO)\n")
                    .append("    VALUES ('").append(border.getType()).append("',\n")
                    .append("        (SELECT ID FROM PROVINCE WHERE NAME = '")
                    .append(border.getFirst()).append("'),\n")
                    .append("        (SELECT ID FROM PROVINCE WHERE NAME = '")
                    .append(border.getSecond()).append("'));\n");
            sqlWriter.append("INSERT INTO BORDER (TYPE, ID_PROVINCE_FROM, ID_PROVINCE_TO)\n")
                    .append("    VALUES ('").append(border.getType()).append("',\n")
                    .append("        (SELECT ID FROM PROVINCE WHERE NAME = '")
                    .append(border.getSecond()).append("'),\n")
                    .append("        (SELECT ID FROM PROVINCE WHERE NAME = '")
                    .append(border.getFirst()).append("'));\n");
        }

        sqlWriter.flush();
        sqlWriter.close();
    }

    /**
     * Creates a FileWriter given a path. If parent directory does not exist, will attempt to create it and then retry.
     *
     * @param fileName the path
     * @param append   true to writes at the end of the file.
     * @return a FileWriter
     * @throws java.io.IOException erreur de lecture.
     */
    private static Writer createFileWriter(final String fileName, final boolean append) throws IOException {
        Writer writer;
        try {
            writer = new FileWriter(fileName, append);
        } catch (FileNotFoundException e) {
            // if parent directory does not exist then
            // attempt to create it and try to create file
            String parentName = new File(fileName).getParent();
            if (parentName != null) {
                File parentDir = new File(parentName);
                if (!parentDir.exists() && parentDir.mkdirs()) {
                    writer = new FileWriter(fileName, append);
                } else {
                    throw e;
                }
            } else {
                throw e;
            }
        }

        return writer;
    }

    /**
     * Returns the first element of the list.
     *
     * @param list the list.
     * @param <E>  generic of the list.
     * @return the first element of the list.
     */
    private static <E> E firstElement(List<E> list) {
        E elem = null;

        if (list != null && !list.isEmpty()) {
            elem = list.get(0);
        }

        return elem;
    }

    /**
     * Returns the last element of the list.
     *
     * @param list the list.
     * @param <E>  generic of the list.
     * @return the last element of the list.
     */
    private static <E> E lastElement(List<E> list) {
        E elem = null;

        if (list != null && !list.isEmpty()) {
            elem = list.get(list.size() - 1);
        }

        return elem;
    }

    /**
     * Return the distance that is needed in order to close the polygone. Returns 0 if the polygone is closed.
     *
     * @param polygone to check.
     * @return the distance that is needed in order to close the polygone. Returns 0 if the polygone is closed.
     */
    private static double distanceToClosePolygone(List<Pair<Integer, Integer>> polygone) {
        return distance(firstElement(polygone), lastElement(polygone));
    }

    /**
     * Returns the distance between the two points.
     *
     * @param first  one point.
     * @param second another point.
     * @return the distance between the two points.
     */
    private static double distance(Pair<Integer, Integer> first, Pair<Integer, Integer> second) {
        if (first == null || second == null) {
            return 0;
        }

        return Math.sqrt((first.getLeft() - second.getLeft()) * (first.getLeft() - second.getLeft()) + (first.getRight() - second.getRight()) * (first.getRight() - second.getRight()));
    }

    /** Inner class describing a path (can be border, mer or path). */
    private static class Path {
        /** Name of the path. */
        private String name;
        /** Flag saying that the path begins by itself (and does not continue the previous one). */
        private boolean begin;
        /** Flag saying that this SubProvince is located in the ROTW map. */
        private boolean rotw;
        /** Coordinates of the path. */
        private List<Pair<Integer, Integer>> coords = new ArrayList<>();

        /**
         * Constructor.
         *
         * @param name  of the path.
         * @param begin of the path.
         */
        public Path(String name, boolean begin, boolean rotw) {
            this.name = name;
            this.begin = begin;
            this.rotw = rotw;
        }

        /** @return the name. */
        public String getName() {
            return name;
        }

        /** @return the begin. */
        public boolean isBegin() {
            return begin;
        }

        /** @return the rotw. */
        public boolean isRotw() {
            return rotw;
        }

        /** @return the coords. */
        public List<Pair<Integer, Integer>> getCoords() {
            return coords;
        }

        /**
         * Returns the inverted coords of the path.
         *
         * @return the inverted coords of the path.
         */
        public List<Pair<Integer, Integer>> getInvertedCoords() {
            List<Pair<Integer, Integer>> invertedCoords = new ArrayList<>(coords);
            Collections.reverse(invertedCoords);

            return invertedCoords;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            boolean equals = false;

            if (obj instanceof Path) {
                equals = StringUtils.equals(name, ((Path) obj).getName())
                        && rotw == ((Path) obj).isRotw();
            }

            return equals;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(name).append(rotw).hashCode();
        }
    }

    /** Inner class describing a directed path (inversed or not). */
    private static class DirectedPath {
        /** the path. */
        private Path path;
        /** the flag saying that the path is inversed or not. */
        private boolean inverse;

        /**
         * Constructor.
         *
         * @param path    the path.
         * @param inverse the inverse.
         */
        public DirectedPath(Path path, boolean inverse) {
            this.path = path;
            this.inverse = inverse;
        }

        /** @return the path. */
        public Path getPath() {
            return path;
        }

        /** @return the inverse. */
        public boolean isInverse() {
            return inverse;
        }
    }

    /** Inner class describing a province. */
    private static class Province {
        /** The name of the province. */
        private String name;
        /** Terrain derived from the portions. */
        private String terrain;
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

    /** Inner class describing a portion of a province. */
    private static class SubProvince {
        /** The terrain of the province. */
        private String terrain;
        /** Flag saying that this sub province is secondary. */
        private boolean secondary;
        /** Flag saying that this SubProvince is located in the ROTW map. */
        private boolean rotw;
        /** The paths representing the frontiers of the province. */
        private List<DirectedPath> paths = new ArrayList<>();
        /** Flag saying that this SubProvince is a light one not to be considered (in zoom or between Europe and ROTW). */
        private boolean light;

        /**
         * Constructor.
         *
         * @param terrain of the province.
         */
        public SubProvince(String terrain, boolean secondary, boolean rotw) {
            if (terrain.startsWith("l") && !StringUtils.equals("lac", terrain)) {
                this.terrain = terrain.substring(1);
                light = true;
            } else {
                this.terrain = terrain;
            }
            if (terrain.startsWith("europe")) {
                light = true;
            }
            this.secondary = secondary;
            this.rotw = rotw;
        }

        /** @return the terrain. */
        public String getTerrain() {
            return terrain;
        }

        /** @return the secondary. */
        public boolean isSecondary() {
            return secondary;
        }

        /** @return the rotw. */
        public boolean isRotw() {
            return rotw;
        }

        /** @return the paths. */
        public List<DirectedPath> getPaths() {
            return paths;
        }

        /** @return the light. */
        public boolean isLight() {
            return light;
        }

        /**
         * Generates the restructurated coords of the province.
         *
         * @param province for logging purpose.
         * @param log      log writer.
         * @return the restructurated coords of the province.
         * @throws Exception exception.
         */
        public Pair<List<List<Pair<Integer, Integer>>>, Boolean> getStructuratedCoords(Province province, Writer log) throws Exception {
            List<List<Pair<Integer, Integer>>> coordsPortion = new ArrayList<>();
            coordsPortion.add(new ArrayList<>());
            boolean sawBeginPath = false;
            for (DirectedPath path : getPaths()) {
                List<Pair<Integer, Integer>> pathValues;
                if (path.isInverse()) {
                    pathValues = path.getPath().getInvertedCoords();
                } else {
                    pathValues = path.getPath().getCoords();
                }

                // Should not happen but too many !pathValues.isEmpty() in code.
                if (pathValues.isEmpty()) {
                    continue;
                }

                if (path.getPath().isBegin()) {
                    // if path is a begin path, we must check if it is en enclave or a continuation from last coords.
                    Pair<Integer, Integer> lastCoords;
                    if (!lastElement(coordsPortion).isEmpty()) {
                        lastCoords = lastElement(lastElement(coordsPortion));
                    } else {
                        // if this is the first path, we take the last path to check.
                        DirectedPath lastElement = lastElement(getPaths());
                        if (lastElement.isInverse()) {
                            lastCoords = lastElement(path.getPath().getInvertedCoords());
                        } else {
                            lastCoords = lastElement(path.getPath().getCoords());
                        }
                    }

                    double nextDistance = distance(lastCoords, firstElement(pathValues));
                    if (nextDistance > 0 && sawBeginPath) {
                        // if it is not a continuation and this is not the first begin path that we saw, we check if it is an error in the file or an enclave.
                        double distance;
                        if (!lastElement(coordsPortion).isEmpty()) {
                            distance = distanceToClosePolygone(lastElement(coordsPortion));
                        } else {
                            // if this is the first path of the SubProvince, we consider it is an error in the file.
                            distance = 2 * nextDistance;
                        }

                        if (distance > 0) {
                            if (distance > nextDistance) {
                                log.append(province.getName()).append("\t").append("Border not consistent (ignored)").append("\t").append(path.getPath().getName())
                                        .append("\t").append(firstElement(pathValues).toString()).append("\t").append(lastElement(lastElement(coordsPortion)).toString()).append("\t").append(firstElement(lastElement(coordsPortion)).toString()).append("\n");
                            } else {
                                log.append(province.getName()).append("\t").append("Border not consistent (enclave)").append("\t").append(path.getPath().getName())
                                        .append("\t").append(firstElement(pathValues).toString()).append("\t").append(lastElement(lastElement(coordsPortion)).toString()).append("\t").append(firstElement(lastElement(coordsPortion)).toString())
                                        .append("\t").append(distance + "").append("\t").append(nextDistance + "").append("\n");
                                coordsPortion.add(new ArrayList<>());
                                sawBeginPath = false;
                            }
                        } else {
                            coordsPortion.add(new ArrayList<>());
                            sawBeginPath = false;
                        }
                    } else if (nextDistance > 0) {
                        // if it is not a continuation and this is the first begin path that we saw, we flag it and keep the process.
                        if (path.getPath().getName().startsWith("carre")) {
                            coordsPortion.add(new ArrayList<>());
                            sawBeginPath = false;
                        } else {
                            sawBeginPath = true;
                        }
                    }
                }

                lastElement(coordsPortion).addAll(pathValues);
            }

            double distance = distanceToClosePolygone(lastElement(coordsPortion));


            if (distance > 0) {
                log.append(province.getName()).append("\t").append("Border not closed").append("\t").append(firstElement(getPaths()).getPath().getName())
                        .append("\t").append(lastElement(lastElement(coordsPortion)).toString()).append("\t").append(firstElement(lastElement(coordsPortion)).toString()).append("\n");
            }

            return new ImmutablePair<>(coordsPortion, rotw);
        }
    }

    /** Inner class describing a border between two provinces. */
    @XStreamAlias("border")
    private static class Border {
        /** First province (alphabetical order) of the border. */
        @XStreamAlias("first")
        private String first;
        /** Second province (alphabetical order) of the border. */
        @XStreamAlias("second")
        private String second;
        /** Type of border. */
        @XStreamAlias("type")
        private String type;

        /**
         * Constructor.
         *
         * @param province1 first province.
         * @param province2 second province.
         * @param type      type.
         */
        public Border(Province province1, Province province2, String type) {
            if (province1 == null || province2 == null || province1 == province2) {
                throw new IllegalStateException();
            }

            if (province1.getName().compareTo(province2.getName()) < 0) {
                first = province1.getName();
                second = province2.getName();
            } else {
                first = province2.getName();
                second = province1.getName();
            }

            this.type = type;
        }

        /** @return the first. */
        public String getFirst() {
            return first;
        }

        /** @return the second. */
        public String getSecond() {
            return second;
        }

        /** @return the type. */
        public String getType() {
            return type;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return 11 + 13 * first.hashCode() + 15 * second.hashCode();
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;

            boolean equals = false;

            if (obj instanceof Border) {
                Border border = (Border) obj;

                equals = StringUtils.equals(first, border.getFirst())
                        && StringUtils.equals(second, border.getSecond());
            }

            return equals;
        }
    }
}
