package com.mkl.tools.eu.map;

import com.mkl.tools.eu.util.ToolsUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description of the class.
 *
 * @author MKL.
 */
public class TablesGenerator {

    public static void main(String... args) throws Exception {
        computeTables();
    }

    public static void computeTables() throws IOException {
        Writer sqlWriter = ToolsUtil.createFileWriter("src/main/resources/output/tables.sql", false);

        sqlWriter.append("DELETE FROM T_UNIT;\n\n");

        BufferedReader reader = new BufferedReader(new InputStreamReader(DataExtractor.class.getClassLoader().getResourceAsStream("input/engCountryTables.tex")));
        String line;
        String country = null;
        String type = null;
        Integer id = 1;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            Matcher m = Pattern.compile("\\\\newcommand\\{\\\\(.*)Purchase.*").matcher(line);
            if (m.matches()) {
                country = m.group(1).toLowerCase();
                type = "purchase";
            }
            if (line.equals("}")) {
                country = null;
                type = null;
            }
            m = Pattern.compile("\\\\T([A-Z]*) +& (.*)").matcher(line);
            if (country != null && type != null && m.matches()) {
                Pair<String, Boolean> tech = toTech(m.group(1));
                if (tech != null) {
                    String value = m.group(2);
                    int i = 0;
                    for (String price : value.split("&")) {
                        price = price.trim();
                        if (price.endsWith("\\\\")) {
                            price = price.substring(0, price.length() - 2).trim();
                        }

                        if (i >= 2 && i <= 4 && tech.getRight()) {
                            String[] subPrices = price.split("/");
                            Integer subPrice = Integer.parseInt(subPrices[0].trim());
                            id = addUnitLine(sqlWriter, country, tech.getLeft(), subPrice, getType(i, tech.getRight()), getAction(i, tech.getRight()), false, id);
                            subPrice = Integer.parseInt(subPrices[1].trim());
                            id = addUnitLine(sqlWriter, country, tech.getLeft(), subPrice, getType(i, tech.getRight()), getAction(i, tech.getRight()), true, id);
                        } else {
                            Integer realPrice = null;
                            if (!price.equals("---")) {
                                realPrice = Integer.parseInt(price);
                            }
                            id = addUnitLine(sqlWriter, country, tech.getLeft(), realPrice, getType(i, tech.getRight()), getAction(i, tech.getRight()), false, id);
                        }

                        i++;
                    }


                }
            }
        }

        sqlWriter.flush();
        sqlWriter.close();
    }

    private static Pair<String, Boolean> toTech(String input) {
        Pair<String, Boolean> tech = null;

        switch (input) {
            case "MED":
                tech = new ImmutablePair<>("MEDIEVAL", true);
                break;
            case "REN":
                tech = new ImmutablePair<>("RENAISSANCE", true);
                break;
            case "ARQ":
                tech = new ImmutablePair<>("ARQUEBUS", true);
                break;
            case "MUS":
                tech = new ImmutablePair<>("MUSKET", true);
                break;
            case "BAR":
                tech = new ImmutablePair<>("BAROQUE", true);
                break;
            case "MAN":
                tech = new ImmutablePair<>("MANOEUVRE", true);
                break;
            case "L":
                tech = new ImmutablePair<>("LACE_WAR", true);
                break;
            case "CAR":
                tech = new ImmutablePair<>("CARRACK", false);
                break;
            case "GLN":
                tech = new ImmutablePair<>("NAE_GALEON", false);
                break;
            case "LS":
                tech = new ImmutablePair<>("GALLEON_FLUYT", false);
                break;
            case "BAT":
                tech = new ImmutablePair<>("BATTERY", false);
                break;
            case "VE":
                tech = new ImmutablePair<>("VESSEL", false);
                break;
            case "TD":
                tech = new ImmutablePair<>("THREE_DECKER", false);
                break;
            default:
                break;
        }

        return tech;
    }

    private static String getType(Integer index, boolean isTechLand) {
        String type = null;

        switch (index) {
            case 0:
                if (isTechLand) {
                    type = "LD";
                } else {
                    type = "NWD";
                }
                break;
            case 1:
                if (isTechLand) {
                    type = "ARMY_MINUS";
                } else {
                    type = "FLEET_MINUS";
                }
                break;
            case 2:
                if (isTechLand) {
                    type = "LD";
                } else {
                    type = "NGD";
                }
                break;
            case 3:
                if (isTechLand) {
                    type = "ARMY_MINUS";
                } else {
                    type = "FLEET_GALLEY_MINUS";
                }
                break;
            case 4:
                if (isTechLand) {
                    type = "ARMY_PLUS";
                } else {
                    type = "NTD";
                }
                break;
            case 5:
                if (isTechLand) {
                    type = "LD";
                } else {
                    type = "ND";
                }
                break;
            case 6:
                if (isTechLand) {
                    type = "ARMY_MINUS";
                } else {
                    type = "FLEET_MINUS";
                }
                break;
            case 7:
                if (isTechLand) {
                    type = "ARMY_PLUS";
                } else {
                    type = "FLEET_PLUS";
                }
                break;
            default:
                break;
        }

        return type;
    }

    private static String getAction(Integer index, boolean isTechLand) {
        String action = null;

        if (!isTechLand) {
            switch (index) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                    action = "PURCHASE";
                    break;
                case 5:
                case 6:
                case 7:
                    action = "MAINT";
                    break;
                default:
                    break;
            }
        } else {
            switch (index) {
                case 0:
                case 1:
                    action = "PURCHASE";
                    break;
                case 2:
                case 3:
                case 4:
                    action = "MAINT_WAR";
                    break;
                case 5:
                case 6:
                case 7:
                    action = "MAINT_PEACE";
                    break;
                default:
                    break;
            }
        }

        return action;
    }

    private static int addUnitLine(Writer sqlWriter, String country, String tech, Integer price, String type, String action, boolean special, Integer id) throws IOException {
        sqlWriter.append("INSERT INTO T_UNIT (ID, R_COUNTRY, ID_TECH, PRICE, TYPE, ACTION, SPECIAL)\n" +
                "    VALUES (")
                .append(integerToInteger(id)).append(", ")
                .append(stringToString(country)).append(", ")
                .append("(SELECT ID FROM T_TECH WHERE NAME = ").append(stringToString(tech)).append("), ")
                .append(integerToInteger(price)).append(", ")
                .append(stringToString(type)).append(", ")
                .append(stringToString(action)).append(", ")
                .append(booleanToBit(special)).append(");\n");

        return ++id;
    }

    /**
     * Convert a String to String (database).
     *
     * @param toConvert String to convert.
     * @return a String.
     */

    private static String stringToString(String toConvert) {
        String db = "null";

        if (!StringUtils.isEmpty(toConvert)) {
            db = "'" + toConvert + "'";
        }

        return db;
    }

    /**
     * Convert an Integer to Integer (database).
     *
     * @param toConvert Integer to convert.
     * @return a String.
     */
    private static String integerToInteger(Integer toConvert) {
        String db = "null";

        if (toConvert != null) {
            db = Integer.toString(toConvert);
        }

        return db;
    }

    /**
     * Convert a boolean to bit (database).
     *
     * @param toConvert boolean to convert.
     * @return a bit.
     */
    private static String booleanToBit(Boolean toConvert) {
        String bit = null;

        if (toConvert != null) {
            if (toConvert) {
                bit = "b'1'";
            } else {
                bit = "b'0'";
            }
        }

        return bit;
    }
}
