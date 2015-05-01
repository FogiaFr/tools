package com.mkl.tools.eu.vo.province;

/**
 * Inner class describing a directed path (inverse or not).
 *
 * @author MKL
 */
public class DirectedPath {
    /** the path. */
    private Path path;
    /** the flag saying that the path is inverse or not. */
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