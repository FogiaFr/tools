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
public class MapGenerator {
    /**
     * Do all the stuff.
     *
     * @param args no args.
     * @throws Exception exception.
     */
    public static void main(String[] args) throws Exception {
        String ligne;

        BufferedReader reader = new BufferedReader(new InputStreamReader(MapGenerator.class.getClassLoader().getResourceAsStream("chemins.eps")));
        Map<String, Path> paths = new HashMap<>();
        Path currentPath = null;
        Map<String, Province> provinces = new HashMap<>();
        Province currentProv = null;
        int currentProvDeepth = -1;
        Pattern mer = Pattern.compile("/mer\\d+ beginpath.*");
        Pattern bord = Pattern.compile("/bord\\d+ beginpath.*");
        Pattern path = Pattern.compile("/path\\d+ beginpath.*");
        Pattern province = Pattern.compile("%#(=+) (.*)");
        while ((ligne = reader.readLine()) != null) {
            Matcher provMatch = province.matcher(ligne);
            if (mer.matcher(ligne).matches() || bord.matcher(ligne).matches() || path.matcher(ligne).matches()) {
                currentPath = new Path(ligne.split(" ")[0], !ligne.contains("contpath"));
            } else if (ligne.startsWith("endpath") && currentPath != null) {
                paths.put(currentPath.getName(), currentPath);
                currentPath = null;
            } else if (currentPath != null) {
                String[] coords = ligne.split(" ");
                for (int i = 0; i < ligne.length() - 1; i = i + 2) {
                    try {
                        Integer x = Integer.parseInt(coords[i]);
                        Integer y = Integer.parseInt(coords[i + 1]);
                        currentPath.getCoords().add(new ImmutablePair<>(x, y));
                    } catch (NumberFormatException e) {
                        break;
                    }
                }
            } else if (provMatch.matches()) {
                if (currentProv != null) {
                    provinces.put(currentProv.getName(), currentProv);
                }
                currentProvDeepth = provMatch.group(1).length();
                String fullName = provMatch.group(2);
                Matcher m = Pattern.compile("(.*) <(.*)>").matcher(fullName);
                if (m.matches()) {
                    currentProv = new Province(m.group(1), m.group(2));
                } else {
                    currentProv = new Province(fullName, null);
                }
            } else if (currentProv != null) {
                if (ligne.startsWith("/prov")) {
                    currentProv.setTerrain(ligne.split(" ")[1]);
                    Matcher m = Pattern.compile("(/path\\d+ AR?)|(/bord\\d+ AR?)|(/mer\\d+ AR?)").matcher(ligne);
                    while (m.find()) {
                        String chaine = m.group();
                        Path pathFound = paths.get(chaine.split(" ")[0]);
                        if (pathFound != null) {
                            currentProv.getPaths().add(new DirectedPath(pathFound, chaine.split(" ")[1].contains("R")));
                        } else {
                            System.out.println("Oops, le chemin " + pathFound + " n'a pas ete trouve pour " + currentProv.getName());
                        }
                    }
                } else if (ligne.startsWith("%#") && ligne.split(" ").length - 2 < currentProvDeepth) {
                    provinces.put(currentProv.getName(), currentProv);
                    currentProvDeepth = -1;
                    currentProv = null;
                }
            }
        }

        if (currentProv != null) {
            provinces.put(currentProv.getName(), currentProv);
        }

        reader.close();

        extractData(provinces);
    }

    private static void extractData(Map<String, Province> provinces) throws Exception {


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
//            if (polygones.getCoords().size() == 1) {
                writer.append("Polygon");
//            } else if (polygones.getCoords().size() > 1) {
//                writer.append("MultiPolygon");
//            } else {
//                System.out.println("Oops, la province " + polygones.getName() + " n'a pas de frontieres.");
//            }
            writer.append("\",\"coordinates\":[");

            boolean firstPolygon = true;
            for (List<Pair<Integer, Integer>> polygone : polygones.getCoords()) {
                if (!firstPolygon) {
                    writer.append(", ");
                } else {
                    firstPolygon = false;
                }
                writer.append("[");
//                if (polygones.getCoords().size() > 1) {
//                    writer.append("[");
//                }

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

//                if (polygones.getCoords().size() > 1) {
//                    writer.append("]");
//                }

                writer.append("]");
            }

            writer.append("]},\"id\":\"").append(polygones.getName()).append("\"}");
        }

        writer.append("\n]}");

        writer.flush();
        writer.close();
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
        /** The religion of the province. */
        private String religion;
        /** The terrain of the province. */
        private String terrain;
        /** The paths representing the frontiers of the province. */
        private List<DirectedPath> paths = new ArrayList<>();
        /** Restructuration of the coords for the geo.json export. */
        private List<List<Pair<Integer, Integer>>> coords;

        /**
         * Constructor.
         *
         * @param name     of the province.
         * @param religion of the province.
         */
        public Province(String name, String religion) {
            this.name = name;
            this.religion = religion;
        }

        /** @return the name. */
        public String getName() {
            return name;
        }

        /** @return the religion. */
        public String getReligion() {
            return religion;
        }

        /** @return the terrain. */
        public String getTerrain() {
            return terrain;
        }

        /** @param terrain the terrain to set. */
        public void setTerrain(String terrain) {
            this.terrain = terrain;
        }

        /** @return the paths. */
        public List<DirectedPath> getPaths() {
            return paths;
        }

        /** @return the coords. */
        public List<List<Pair<Integer, Integer>>> getCoords() {
            return coords;
        }

        /** Generates the restructurated coords of the province. */
        public void restructurate() {
            if (getName().equals("Norvege")) {
                int a = 1;
            }
            coords = new ArrayList<>();
            coords.add(new ArrayList<>());

            for (DirectedPath path : getPaths()) {
                List<Pair<Integer, Integer>> pathValues;
                if (path.isInverse()) {
                    pathValues = path.getPath().getInvertedCoords();
                } else {
                    pathValues = path.getPath().getCoords();
                }

                if (path.getPath().isBegin() && !lastElement(coords).isEmpty() && !pathValues.isEmpty()) {
                    double nextDistance = distance(lastElement(lastElement(coords)), firstElement(pathValues));
                    if (nextDistance > 0) {
                        double distance = distanceToClosePolygone(lastElement(coords));

                        if (distance > 0) {
                            if (distance > nextDistance) {
                                System.out.println("OK " + getName() + " n'est pas fermé à " + path.getPath().getName() + " ! " + firstElement(lastElement(coords)) + " vs " + lastElement(lastElement(coords)));
                            } else {
                                System.out.println( "KO " + getName() + " n'est pas fermé à " + path.getPath().getName() + " ! " + firstElement(lastElement(coords)) + " vs " + lastElement(lastElement(coords)));
                                coords.add(new ArrayList<>());
                            }
                        } else {
                            coords.add(new ArrayList<>());
                        }
                    }
                }

                lastElement(coords).addAll(pathValues);
            }

            double distance = distanceToClosePolygone(lastElement(coords));


            if (distance > 0) {
                System.out.println(getName() + " n'est pas fermé à " + firstElement(getPaths()).getPath().getName()
                        + " ! " + firstElement(lastElement(coords)) + " vs " + lastElement(lastElement(coords)));
            }

//            if (getPaths().get(0).getPath().isBegin() && ! coords.isEmpty() && ! coords.get(coords.size() - 1).equals(coords.get(0))) {
//                System.out.println(getName() + " a une discontinuite en " + getPaths().get(0).getPath().getName() + " : " + coords.get(coords.size() - 1) + " vs " + coords.get(0));
//            }
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

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
}
