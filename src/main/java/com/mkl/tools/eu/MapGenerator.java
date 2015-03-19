package com.mkl.tools.eu;


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
     * @param args no args.
     * @throws Exception exception.
     */
    public static void main(String[] args) throws Exception {
        String ligne;

        BufferedReader reader = new BufferedReader(new InputStreamReader(MapGenerator.class.getClassLoader().getResourceAsStream("chemins.eps")));
        Map<String, List<Pair<Integer, Integer>>> paths = new HashMap<>();
        List<Pair<Integer, Integer>> currentPath = null;
        Map<String, List<List<Pair<Integer, Integer>>>> provinces = new HashMap<>();
        List<List<Pair<Integer, Integer>>> currentProv = null;
        String currentName = null;
        boolean isParsingPath = false;
        boolean isParsingProvince = false;
        Pattern mer = Pattern.compile("/mer\\d+ beginpath.*");
        Pattern bord = Pattern.compile("/bord\\d+ beginpath.*");
        Pattern path = Pattern.compile("/path\\d+ beginpath.*");
        Pattern province = Pattern.compile("%#=+ (.*)");
        while ((ligne = reader.readLine()) != null) {
            Matcher provMatch = province.matcher(ligne);
            if (mer.matcher(ligne).matches() || bord.matcher(ligne).matches() || path.matcher(ligne).matches()) {
                isParsingPath = true;
                currentPath = new ArrayList<>();
                currentName = ligne.split(" ")[0];
            } else if (ligne.startsWith("endpath") && isParsingPath) {
                paths.put(currentName, currentPath);
                currentPath = null;
                currentName = null;
                isParsingPath = false;
            } else if (isParsingPath) {
                String[] coords = ligne.split(" ");
                for (int i = 0; i < ligne.length() - 1; i = i + 2) {
                    try {
                        Integer x = Integer.parseInt(coords[i]);
                        Integer y = Integer.parseInt(coords[i + 1]);
                        currentPath.add(new ImmutablePair<>(x, y));
                    } catch (NumberFormatException e) {
                        break;
                    }
                }
            } else if (provMatch.matches()) {
                if (isParsingProvince) {
                    provinces.put(currentName, currentProv);
                }
                isParsingProvince = true;
                currentName = provMatch.group(1);
                currentProv = new ArrayList<>();
            } else if (isParsingProvince) {
                if (ligne.startsWith("/prov")) {
                    List<Pair<Integer, Integer>> provPiece = new ArrayList<>();
                    Matcher m = Pattern.compile("(/path\\d+)|(/bord\\d+)|(/mer\\d+)").matcher(ligne);
                    while (m.find()) {
                        String pathFound = m.group();
                        List<Pair<Integer, Integer>> coordsFound = paths.get(pathFound);
                        if (coordsFound != null) {
                            provPiece.addAll(coordsFound);
                        } else {
                            System.out.println("Oops, le chemin " + pathFound + " n'a pas ete trouve pour " + currentName);
                        }
                    }
                    currentProv.add(provPiece);
                }
            }
            // Bad parsing of provinces, does not detect some cases.
        }

        if (isParsingProvince) {
            provinces.put(currentName, currentProv);
        }

        reader.close();

        Writer writer = createFileWriter("src/main/resources/countries.geo.json", false);
        writer.append("{\"type\":\"FeatureCollection\",\"features\":[\n");
        boolean first = true;

        for (String prov: provinces.keySet()) {
            if (! first) {
                writer.append(",\n");
            } else {
                first = false;
            }
            writer.append("    {\"type\":\"Feature\",\"geometry\":{\"type\":\"");
            List<List<Pair<Integer, Integer>>> polygones = provinces.get(prov);
            if (polygones.size() == 1) {
                writer.append("Polygon");
            } else if (polygones.size() > 1) {
                writer.append("MultiPolygon");
            } else {
                System.out.println("Oops, la province " + prov + " n'a pas de frontieres.");
            }
            writer.append("\",\"coordinates\":[");

            boolean firstPolygon = true;
            for (List<Pair<Integer, Integer>> polygone: polygones) {
                if (! firstPolygon) {
                    writer.append(", ");
                } else {
                    firstPolygon = false;
                }
                writer.append("[");
                if (polygones.size() > 1) {
                    writer.append("[");
                }

                boolean firstCoord = true;
                for (Pair<Integer, Integer> coord: polygone) {
                    if (! firstCoord) {
                        writer.append(", ");
                    } else {
                        firstCoord = false;
                    }
                    double x = 1.204 + coord.getLeft() * 12.859 / 8425;
                    double y = 1.204 + coord.getRight() * 8.862 / 5840;
                    writer.append("[").append(Double.toString(x)).append(", ").append(Double.toString(y)).append("]");
                }

                if (polygones.size() > 1) {
                    writer.append("]");
                }

                writer.append("]");
            }

            writer.append("]},\"id\":\"").append(prov).append("\"}");
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
}
