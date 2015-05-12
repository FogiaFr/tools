package com.mkl.tools.eu.map;

import com.mkl.tools.eu.vo.country.Country;
import com.mkl.tools.eu.vo.province.Border;
import com.mkl.tools.eu.vo.province.Province;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

/**
 * Utility class that gather all injection data for database.
 *
 * @author MKL.
 */
public class DBGenerator {

    /**
     * Create a SQL injection script for provinces, borders and countries.
     *
     * @param provinces list of provinces.
     * @param borders   list of borders.
     * @param sqlWriter where to write the db instructions.
     * @throws IOException exception.
     */
    public static void createDBInjection(Map<String, Province> provinces, List<Border> borders, Writer sqlWriter) throws IOException {
        sqlWriter.append("DELETE FROM R_COUNTRY_PROVINCE_EU_CAPITALS;\n")
                .append("DELETE FROM R_COUNTRY_PROVINCE_EU;\n")
                .append("DELETE FROM R_LIMIT;\n")
                .append("DELETE FROM R_BASIC_FORCE;\n")
                .append("DELETE FROM R_REINFORCEMENTS;\n")
                .append("DELETE FROM R_COUNTRY;\n")
                .append("DELETE FROM R_BORDER;\n")
                .append("DELETE FROM R_PROVINCE_EU;\n")
                .append("DELETE FROM R_PROVINCE_ROTW;\n")
                .append("DELETE FROM R_PROVINCE;\n\n");

        for (Province province : provinces.values()) {
            sqlWriter.append("INSERT INTO R_PROVINCE (NAME, TERRAIN)\n")
                    .append("    VALUES ('").append(province.getName())
                    .append("', '").append(province.getTerrain())
                    .append("');\n");

            if (province.getInfo() != null) {
                sqlWriter.append("INSERT INTO R_PROVINCE_EU (ID, INCOME, FORTRESS, CAPITAL, PORT, ARSENAL, PRAESIDIABLE, METADATA)\n")
                        .append("    VALUES (").append(" (SELECT ID FROM R_PROVINCE WHERE NAME = '").append(province.getName()).append("')")
                        .append(", '").append(Integer.toString(province.getInfo().getIncome()))
                        .append("', '").append(Integer.toString(province.getInfo().getFortress()))
                        .append("', b'").append(booleanToBit(province.getInfo().isCapital()))
                        .append("', b'").append(booleanToBit(province.getInfo().isPort()))
                        .append("', b'").append(booleanToBit(province.getInfo().isArsenal()))
                        .append("', b'").append(booleanToBit(province.getInfo().isPraesidiable()))
                        .append("', '").append(String.join(";;", province.getInfo().getMetadata(province.getName())))
                        .append("');\n");

            } else if (!province.getPortions().get(0).isRotw()) {
                sqlWriter.append("INSERT INTO R_PROVINCE_EU (ID)\n")
                        .append("    VALUES (").append(" (SELECT ID FROM R_PROVINCE WHERE NAME = '").append(province.getName()).append("')")
                        .append(");\n");
            } else {
                sqlWriter.append("INSERT INTO R_PROVINCE_ROTW (ID)\n")
                        .append("    VALUES (").append(" (SELECT ID FROM R_PROVINCE WHERE NAME = '").append(province.getName()).append("')")
                        .append(");\n");
            }
        }

        sqlWriter.append("\n");

        for (Border border : borders) {
            sqlWriter.append("INSERT INTO R_BORDER (TYPE, ID_R_PROVINCE_FROM, ID_R_PROVINCE_TO)\n")
                    .append("    VALUES (").append(stringToString(border.getType())).append(",\n")
                    .append("        (SELECT ID FROM R_PROVINCE WHERE NAME = '")
                    .append(border.getFirst()).append("'),\n")
                    .append("        (SELECT ID FROM R_PROVINCE WHERE NAME = '")
                    .append(border.getSecond()).append("'));\n");
            sqlWriter.append("INSERT INTO R_BORDER (TYPE, ID_R_PROVINCE_FROM, ID_R_PROVINCE_TO)\n")
                    .append("    VALUES (").append(stringToString(border.getType())).append(",\n")
                    .append("        (SELECT ID FROM R_PROVINCE WHERE NAME = '")
                    .append(border.getSecond()).append("'),\n")
                    .append("        (SELECT ID FROM R_PROVINCE WHERE NAME = '")
                    .append(border.getFirst()).append("'));\n");
        }
    }

    /**
     * Create a SQL injection script for countries.
     *
     * @param countries list of countries.
     * @param sqlWriter where to write the db instructions.
     * @throws IOException exception.
     */
    public static void createCountriesData(Map<String, Country> countries, Writer sqlWriter) throws IOException {
        sqlWriter.append("DELETE FROM R_COUNTRY_PROVINCE_EU_CAPITALS;\n")
                .append("DELETE FROM R_COUNTRY_PROVINCE_EU;\n")
                .append("DELETE FROM R_LIMIT;\n")
                .append("DELETE FROM R_BASIC_FORCE;\n")
                .append("DELETE FROM R_REINFORCEMENTS;\n")
                .append("DELETE FROM R_COUNTRY;\n\n");

        for (Country country : countries.values()) {
            sqlWriter.append("INSERT INTO R_COUNTRY (NAME, TYPE, RELIGION, CULTURE, GEOPOLITICS_COUNTRY, GEOPOLITICS_BONUS" +
                    ", RM, SUB, MA, EC, EW, VA, AN" +
                    ", FIDELITY, ARMY_CLASS, ELECTOR, HRE)\n")
                    .append("    VALUES ('").append(country.getName())
                    .append("', '").append(country.getType())
                    .append("', '").append(country.getReligion())
                    .append("', '").append(country.getCulture())
                    .append("', ").append(stringToString(country.getPreference()))
                    .append(", ").append(integerToString(country.getPreferenceBonus()))
                    .append(", ").append(integerToString(country.getRoyalMarriage()))
                    .append(", ").append(integerToString(country.getSubsidies()))
                    .append(", ").append(integerToString(country.getMilitaryAlliance()))
                    .append(", ").append(integerToString(country.getExpCorps()))
                    .append(", ").append(integerToString(country.getEntryInWar()))
                    .append(", ").append(integerToString(country.getVassal()))
                    .append(", ").append(integerToString(country.getAnnexion()))
                    .append(", ").append(integerToString(country.getFidelity()))
                    .append(", '").append(country.getArmyClass())
                    .append("', b'").append(booleanToBit(country.isElector()))
                    .append("', b'").append(booleanToBit(country.isHre()))
                    .append("');\n");

            for (String province : country.getCapitals()) {
                sqlWriter.append("INSERT INTO R_COUNTRY_PROVINCE_EU_CAPITALS (ID_R_COUNTRY, ID_R_PROVINCE_EU)\n")
                        .append("    VALUES (")
                        .append(" (SELECT ID FROM R_COUNTRY WHERE NAME = '").append(country.getName()).append("')")
                        .append(", (SELECT ID FROM R_PROVINCE WHERE NAME = '").append(province).append("')")
                        .append(");\n");
            }

            for (String province : country.getProvinces()) {
                sqlWriter.append("INSERT INTO R_COUNTRY_PROVINCE_EU (ID_R_COUNTRY, ID_R_PROVINCE_EU)\n")
                        .append("    VALUES (")
                        .append(" (SELECT ID FROM R_COUNTRY WHERE NAME = '").append(country.getName()).append("')")
                        .append(", (SELECT ID FROM R_PROVINCE WHERE NAME = '").append(province).append("')")
                        .append(");\n");
            }

            for (Country.Limit limit : country.getLimits()) {
                sqlWriter.append("INSERT INTO R_LIMIT (NUMBER, TYPE, ID_R_COUNTRY)\n")
                        .append("    VALUES (").append(limit.getNumber().toString())
                        .append(", '").append(limit.getType())
                        .append("', (SELECT ID FROM R_COUNTRY WHERE NAME = '").append(country.getName()).append("')")
                        .append(");\n");
            }

            for (Country.Limit limit : country.getBasicForces()) {
                sqlWriter.append("INSERT INTO R_BASIC_FORCE (NUMBER, TYPE, ID_R_COUNTRY)\n")
                        .append("    VALUES (").append(limit.getNumber().toString())
                        .append(", '").append(limit.getType())
                        .append("', (SELECT ID FROM R_COUNTRY WHERE NAME = '").append(country.getName()).append("')")
                        .append(");\n");
            }

            for (Country.Limit limit : country.getReinforcements()) {
                sqlWriter.append("INSERT INTO R_REINFORCEMENTS (NUMBER, TYPE, ID_R_COUNTRY)\n")
                        .append("    VALUES (").append(limit.getNumber().toString())
                        .append(", '").append(limit.getType())
                        .append("', (SELECT ID FROM R_COUNTRY WHERE NAME = '").append(country.getName()).append("')")
                        .append(");\n");
            }
        }
    }

    /**
     * Convert a boolean to bit (database).
     *
     * @param toConvert boolean to convert.
     * @return a bit.
     */
    private static String booleanToBit(Boolean toConvert) {
        String bit = "0";

        if (toConvert != null && toConvert) {
            bit = "1";
        }

        return bit;
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
     * Convert an Integer to String (database).
     *
     * @param toConvert Integer to convert.
     * @return a String.
     */
    private static String integerToString(Integer toConvert) {
        String db = "null";

        if (toConvert != null) {
            db = "'" + Integer.toString(toConvert) + "'";
        }

        return db;
    }
}
