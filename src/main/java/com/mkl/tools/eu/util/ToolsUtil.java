package com.mkl.tools.eu.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class.
 *
 * @author MKL
 */
public final class ToolsUtil {

    /** No instance. */
    private ToolsUtil() {
    }

    /**
     * Creates a FileWriter given a path. If parent directory does not exist, will attempt to create it and then retry.
     *
     * @param fileName the path
     * @param append   true to writes at the end of the file.
     * @return a FileWriter
     * @throws java.io.IOException erreur de lecture.
     */
    public static Writer createFileWriter(final String fileName, final boolean append) throws IOException {
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
     * <p>
     * Returns the index of the next separator sep of the input String avoiding all the couples sepInverse/sep.
     * </p>
     * <p>
     * <p>
     * For example, the string "toto(a), b)" will return the last character if sep is ')' and sepInverse '('.
     * </p>
     *
     * @param string     to test.
     * @param start      index to begin the search.
     * @param sep        wanted separator.
     * @param sepInverse inverse separator for avoid purpose.
     * @return index of the next separator.
     */
    public static int indexOf(String string, int start, char sep, char sepInverse) {
        int index = -1;
        int balance = 0;
        if (string != null) {
            for (int i = start; i < string.length(); i++) {
                char c = string.charAt(i);
                if (c == sep && balance == 0) {
                    index = i;
                    break;
                }
                if (c == sep && balance > 0) {
                    balance--;
                }
                if (c == sepInverse) {
                    balance++;
                }
            }
        }

        return index;
    }

    /**
     * Split a string with separator sep. The separators that are between a sepBeforeIgnore and a sepAfterIgnore are ignored.
     * <p>
     * For example, the String "toto, indexOf(3, 5), tutu" with sep ',' sepBeforeIgnore '(' and sepAfterIgnore ')' will return
     * <li>
     * <ul>toto</ul>
     * <ul> indexOf(3, 5)</ul>
     * <ul>tutu</ul>
     * </li>
     *
     * @param string          to split.
     * @param sep             separator.
     * @param sepBeforeIgnore before separator to ignore.
     * @param sepAfterIgnore  after separator to ignore.
     * @return split of the string.
     */
    public static String[] split(String string, char sep, char sepBeforeIgnore, char sepAfterIgnore) {
        List<String> split = new ArrayList<>();

        if (string != null) {
            int balance = 0;
            int lastBegin = 0;
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);

                if (c == sep && balance == 0) {
                    split.add(string.substring(lastBegin, i));
                    lastBegin = i + 1;
                }

                if (c == sepBeforeIgnore) {
                    balance++;
                }

                if (c == sepAfterIgnore) {
                    balance--;
                }
            }
            split.add(string.substring(lastBegin));
        }

        return split.toArray(new String[split.size()]);
    }

    /**
     * Retrieves pieces of String that begins with sepBefore and ends with sepAfter, ignoring inner sepBefore/sepAfter couple.
     * <p>
     * For example, the String "\minorcountry{mamelouks}{Sultanat of Cairo}{\AE{}gyptus}" with sepBefore '{' and sepAfter '}' will return
     * <li>
     * <ul>mamelouks</ul>
     * <ul>Sultanat of Cairo</ul>
     * <ul>\AE{}gyptus</ul>
     * </li>
     *
     * @param string    to split.
     * @param sepBefore before separator.
     * @param sepAfter  after separator.
     * @return split of the string.
     */
    public static String[] split(String string, char sepBefore, char sepAfter) {
        List<String> split = new ArrayList<>();

        if (string != null) {
            int balance = 0;
            int lastBegin = 0;
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);

                if (c == sepBefore) {
                    if (balance == 0) {
                        lastBegin = i + 1;
                    }
                    balance++;
                }

                if (c == sepAfter) {
                    if (balance == 1) {
                        split.add(string.substring(lastBegin, i));
                    }
                    balance--;
                }
            }
        }

        return split.toArray(new String[split.size()]);
    }
}