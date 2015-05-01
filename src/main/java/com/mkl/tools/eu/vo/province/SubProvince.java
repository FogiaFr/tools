package com.mkl.tools.eu.vo.province;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Inner class describing a portion of a province.
 *
 * @author MKL
 */
public class SubProvince {
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
     * @param terrain   of the province.
     * @param secondary flag saying that the subProvince is not the primal one.
     * @param rotw      flag saying that the object is in the rotw map.
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
                                    .append("\t").append(Double.toString(distance)).append("\t").append(Double.toString(nextDistance)).append("\n");
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
}