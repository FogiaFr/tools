package com.mkl.tools.eu;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to generate the EU Map to the format .geo.json.
 *
 * @author MKL
 */
public final class MapGenerator {
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
        String ligne;

        Writer log = createFileWriter("src/main/resources/log.txt", false);

        BufferedReader reader = new BufferedReader(new InputStreamReader(MapGenerator.class.getClassLoader().getResourceAsStream("chemins.eps")));
        Map<String, Path> paths = new HashMap<>();
        Path currentPath = null;
        Map<String, Province> provinces = new HashMap<>();
        Pattern mer = Pattern.compile("/mer\\d+ beginpath.*");
        Pattern bord = Pattern.compile("/bord\\d+ beginpath.*");
        Pattern path = Pattern.compile("/path\\d+ beginpath.*");
        while ((ligne = reader.readLine()) != null) {
            if (mer.matcher(ligne).matches() || bord.matcher(ligne).matches() || path.matcher(ligne).matches()) {
                currentPath = new Path(ligne.split(" ")[0], !ligne.contains("contpath"));
            } else if (ligne.startsWith("endpath") && currentPath != null) {
                paths.put(currentPath.getName(), currentPath);
                currentPath = null;
            } else if (currentPath != null) {
                String[] coords = ligne.split(" ");
                for (int i = 0; i < ligne.length() - 1; i = i + 2) {
                    try {
                        currentPath.getCoords().add(new ImmutablePair<>(Integer.parseInt(coords[i]), Integer.parseInt(coords[i + 1])));
                    } catch (NumberFormatException e) {
                        break;
                    }
                }
            } else if (ligne.startsWith("/prov")) {
                addSubProvince(ligne, provinces, paths, log);
            }
        }

        reader.close();

        extractData(provinces, log);

        log.flush();
        log.close();
    }

    /**
     * Adds a portion to an existing province.
     *
     * @param ligne     to parse.
     * @param provinces existing provinces.
     * @param paths     list of paths.
     * @param log       log writer.
     * @throws IOException exception.
     */
    private static void addSubProvince(String ligne, Map<String, Province> provinces, Map<String, Path> paths, Writer log) throws IOException {
        Matcher m = Pattern.compile(".*\\((.*)\\) ?ppdef.*").matcher(ligne);
        if (! m.matches()) {
            return;
        }
        String provinceName = m.group(1);
        if (provinceName.startsWith("*")) {
            provinceName = provinceName.substring(1);
        }
        if (StringUtils.equals("Caption", provinceName) || StringUtils.equals("Special", provinceName)) {
            return;
        }
        Province province = provinces.get(provinceName);
        if (province == null) {
            province = new Province(provinceName, log);
            provinces.put(provinceName, province);
        }
        SubProvince portion = new SubProvince(ligne.split(" ")[1]);
        province.getPortions().add(portion);
        m = Pattern.compile("(/path\\d+ AR?)|(/bord\\d+ AR?)|(/mer\\d+ AR?)").matcher(ligne);
        while (m.find()) {
            String chaine = m.group();
            Path pathFound = paths.get(chaine.split(" ")[0]);
            if (pathFound != null) {
                portion.getPaths().add(new DirectedPath(pathFound, chaine.split(" ")[1].contains("R")));
            } else {
                log.append(province.getName()).append("\t").append("Path not found").append("\t").append(chaine.split(" ")[0]).append("\n");
            }
        }
    }

    /**
     * Create files used by the application.
     *
     * @param provinces data gathered by the input.
     * @param log       log writer.
     * @throws Exception exception.
     */
    private static void extractData(Map<String, Province> provinces, Writer log) throws Exception {
        Writer writer = createFileWriter("src/main/resources/countries.geo.json", false);
        writer.append("{\"type\":\"FeatureCollection\",\"features\":[\n");
        boolean first = true;

        for (String prov : provinces.keySet()) {
            if (!first) {
                writer.append(",\n");
            } else {
                first = false;
            }
            writer.append("    {\"type\":\"Feature\",\"geometry\":{\"type\":\"");
            Province polygones = provinces.get(prov);
            polygones.restructurate();
            if (polygones.getCoords().size() == 1) {
                writer.append("Polygon");
            } else if (polygones.getCoords().size() > 1) {
                writer.append("MultiPolygon");
            } else {
                log.append(polygones.getName()).append("\t").append("No border.").append("\n");
            }
            writer.append("\",\"coordinates\":[");

            if (polygones.getCoords().size() == 1) {
                writePolygone(polygones.getCoords().get(0), writer);
            } else {
                boolean firstPolygon = true;
                for (List<List<Pair<Integer, Integer>>> polygone : polygones.getCoords()) {
                    if (!firstPolygon) {
                        writer.append(", ");
                    } else {
                        firstPolygon = false;
                    }
                    writer.append("[");

                    writePolygone(polygone, writer);

                    writer.append("]");
                }
            }

            writer.append("]},\"id\":\"").append(polygones.getName()).append("\"}");
        }
        // Military rounds tiles
        int roundXBegin = 1550;
        int roundYBegin = 32;
        int roundSize = 113;
        for (int i = 0; i <= 5; i++) {
            writeSquare(writer, roundXBegin + 2 * i * roundSize, roundYBegin, roundSize, "MR_W" + i);
            String name = "MR_S" + (i + 1);
            if (i == 5) {
                name = "MR_End";
            }
            writeSquare(writer, roundXBegin + 2 * i * roundSize, roundYBegin + 2 * roundSize, roundSize, name);
        }


        writer.append("\n]}");

        writer.flush();
        writer.close();
    }

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
     * Write a polygone in a geo.json format.
     *
     * @param polygones List of coordinates of the polygones.
     * @param writer    File Writer.
     * @throws Exception exception.
     */
    private static void writePolygone(List<List<Pair<Integer, Integer>>> polygones, Writer writer) throws Exception {
        boolean firstPolygon = true;
        for (List<Pair<Integer, Integer>> polygone : polygones) {
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
                double x = 1.204 + coord.getLeft() * 12.859 / 8425;
                double y = 1.204 + coord.getRight() * 8.862 / 5840;
                writer.append("[").append(Double.toString(x)).append(", ").append(Double.toString(y)).append("]");
            }

            writer.append("]");
        }
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
        /** Coordinates of the path. */
        private List<Pair<Integer, Integer>> coords = new ArrayList<>();

        /**
         * Constructor.
         *
         * @param name  of the path.
         * @param begin of the path.
         */
        public Path(String name, boolean begin) {
            this.name = name;
            this.begin = begin;
        }

        /** @return the name. */
        public String getName() {
            return name;
        }

        /** @return the begin. */
        public boolean isBegin() {
            return begin;
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
                equals = StringUtils.equals(name, ((Path) obj).getName());
            }

            return equals;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return name.hashCode();
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
        /** Portions of the province. */
        private List<SubProvince> portions = new ArrayList<>();
        /** Restructuration of the coords for the geo.json export. */
        private List<List<List<Pair<Integer, Integer>>>> coords;
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

        /** @return the portions. */
        public List<SubProvince> getPortions() {
            return portions;
        }

        /** @return the coords. */
        public List<List<List<Pair<Integer, Integer>>>> getCoords() {
            return coords;
        }

        /**
         * Generates the restructurated coords of the province.
         *
         * @throws Exception exception.
         */
        public void restructurate() throws Exception {
            coords = new ArrayList<>();
            String terrain = null;
            for (SubProvince portion : portions) {
                coords.add(portion.getStructuratedCoords(this, log));
                if (terrain == null && !StringUtils.equals("lac", portion.getTerrain())) {
                    terrain = portion.getTerrain();
                } else if (!StringUtils.equals(terrain, portion.getTerrain()) && !StringUtils.equals("lac", portion.getTerrain())) {
                    log.append(getName()).append("\t").append("Terrain not consistent").append("\t").append(terrain).append("\t").append(portion.getTerrain()).append("\n");
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
        /** The paths representing the frontiers of the province. */
        private List<DirectedPath> paths = new ArrayList<>();

        /**
         * Constructor.
         *
         * @param terrain of the province.
         */
        public SubProvince(String terrain) {
            this.terrain = terrain;
        }

        /** @return the terrain. */
        public String getTerrain() {
            return terrain;
        }

        /** @return the paths. */
        public List<DirectedPath> getPaths() {
            return paths;
        }

        /**
         * Generates the restructurated coords of the province.
         *
         * @param province for logging purpose.
         * @param log      log writer.
         * @return the restructurated coords of the province.
         * @throws Exception exception.
         */
        public List<List<Pair<Integer, Integer>>> getStructuratedCoords(Province province, Writer log) throws Exception {
            List<List<Pair<Integer, Integer>>> coordsPortion = new ArrayList<>();
            coordsPortion.add(new ArrayList<>());
            for (DirectedPath path : getPaths()) {
                List<Pair<Integer, Integer>> pathValues;
                if (path.isInverse()) {
                    pathValues = path.getPath().getInvertedCoords();
                } else {
                    pathValues = path.getPath().getCoords();
                }

                if (path.getPath().isBegin() && !lastElement(coordsPortion).isEmpty() && !pathValues.isEmpty()) {
                    double nextDistance = distance(lastElement(lastElement(coordsPortion)), firstElement(pathValues));
                    if (nextDistance > 0) {
                        double distance = distanceToClosePolygone(lastElement(coordsPortion));

                        if (distance > 0) {
                            if (distance > nextDistance) {
                                log.append(province.getName()).append("\t").append("Border not consistent (ignored)").append("\t").append(path.getPath().getName())
                                        .append("\t").append(firstElement(pathValues).toString()).append("\t").append(lastElement(lastElement(coordsPortion)).toString()).append("\t").append(firstElement(lastElement(coordsPortion)).toString()).append("\n");
                            } else {
                                log.append(province.getName()).append("\t").append("Border not consistent (enclave)").append("\t").append(path.getPath().getName())
                                        .append("\t").append(firstElement(pathValues).toString()).append("\t").append(lastElement(lastElement(coordsPortion)).toString()).append("\t").append(firstElement(lastElement(coordsPortion)).toString()).append("\n");
                                coordsPortion.add(new ArrayList<>());
                            }
                        } else {
                            coordsPortion.add(new ArrayList<>());
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

            return coordsPortion;
        }
    }
}
