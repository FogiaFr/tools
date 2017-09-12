package com.mkl.tools.eu.tables;

import com.mkl.tools.eu.map.DataExtractor;
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
 * Generates sql for the tables.
 *
 * @author MKL.
 */
public class TablesGenerator {

    /**
     * Main.
     *
     * @param args none.
     * @throws Exception Exception.
     */
    public static void main(String... args) throws Exception {
        Writer sqlWriter = ToolsUtil.createFileWriter("src/main/resources/output/tables-auto.sql", false);

        sqlWriter.append("DELETE FROM T_UNIT;\n")
                .append("DELETE FROM T_BASIC_FORCE;\n")
                .append("DELETE FROM T_LIMIT;\n")
                .append("DELETE FROM T_TRADE;\n")
                .append("DELETE FROM T_RESULT;\n")
                .append("\n");

        computeCountryTables(sqlWriter);
        computeGeneralTables(sqlWriter);

        sqlWriter.flush();
        sqlWriter.close();
    }

    /**
     * Compute the country tables.
     *
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException
     */
    public static void computeCountryTables(Writer sqlWriter) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(DataExtractor.class.getClassLoader().getResourceAsStream("input/tables/engCountryTables.tex")));
        String line;
        String country = null;
        String type = null;
        Integer id = 1;
        String previousLine = null;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            Matcher m = Pattern.compile("\\\\newcommand\\{\\\\(.*)Purchase.*").matcher(line);
            if (m.matches()) {
                country = m.group(1).toLowerCase();
                type = "purchase";
            }
            m = Pattern.compile("\\\\newcommand\\{\\\\(.*)Turn.*").matcher(line);
            if (m.matches()) {
                country = m.group(1).toLowerCase();
                type = "limits";
            }
            m = Pattern.compile("\\\\newcommand\\{\\\\(.*)Period.*").matcher(line);
            if (m.matches()) {
                country = m.group(1).toLowerCase();
                type = "maxima";
            }
            if (line.equals("}")) {
                country = null;
                type = null;
            }
            if (country != null) {
                if (StringUtils.equals("purchase", type)) {
                    m = Pattern.compile("\\\\T([A-Z]*) +& (.*)").matcher(line);
                    if (m.matches()) {
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
                } else if (StringUtils.equals("limits", type)) {
                    if (previousLine != null) {
                        line = previousLine + line;
                        previousLine = null;
                    }

                    if (line.endsWith("&")) {
                        previousLine = line;
                        continue;
                    }

                    m = Pattern.compile("\\d{4}\\-+\\d{4} ([IV]+) *&(.*)").matcher(line);
                    if (m.matches()) {
                        String period = m.group(1);
                        String[] limits = m.group(2).split("&");

                        addLimitLine(sqlWriter, country, period, getLimitNumber(limits[0]), "ACTION_DIPLO");
                        addLimitLine(sqlWriter, country, period, getLimitNumber(limits[1]), "ACTION_TFI");
                        addLimitLine(sqlWriter, country, period, getLimitNumber(limits[2]), "ACTION_COL");
                        addLimitLine(sqlWriter, country, period, getLimitNumber(limits[3]), "ACTION_TP");
                        addLimitLine(sqlWriter, country, period, getLimitNumber(limits[4]), "ACTION_CONCURRENCY");

                        String[] basicForces = limits[5].trim().split(" ");
                        if (StringUtils.equals("hollande", country)) {
                            basicForces = limits[6].trim().split(" ");
                        }
                        for (String basicForce : basicForces) {
                            if (basicForce.startsWith("(")) {
                                continue;
                            }
                            int number = 1;
                            String typeInput = basicForce;
                            m = Pattern.compile("(\\d)+(.*)").matcher(basicForce);
                            if (m.matches()) {
                                number = Integer.parseInt(m.group(1));
                                typeInput = m.group(2);
                            }
                            if (StringUtils.equals("\\LeaderG;", typeInput) && StringUtils.equals("pologne", country)) {
                                addLimitLine(sqlWriter, country, period, number, "LEADER_GENERAL");
                            } else {
                                String force = getBasicForceType(typeInput);
                                addBasicForceLine(sqlWriter, country, period, number, force);
                            }
                        }

                        int unitLimitNumber = 6;
                        int leaderNumber = 7;
                        if (StringUtils.equals("turquie", country) || StringUtils.equals("pologne", country) || StringUtils.equals("hollande", country)) {
                            unitLimitNumber = 8;
                            leaderNumber = 9;
                        }
                        String unitLimits = limits[unitLimitNumber].trim();
                        m = Pattern.compile("(\\d)\\\\ND[\\\\xadb]* ?/ ?(\\d)\\\\LD[\\\\xadc]*").matcher(unitLimits);
                        if (m.matches()) {
                            addLimitLine(sqlWriter, country, period, Integer.parseInt(m.group(1)), "PURCHASE_NAVAL_TROOPS");
                            addLimitLine(sqlWriter, country, period, Integer.parseInt(m.group(2)), "PURCHASE_LAND_TROOPS");
                        } else {
                            System.out.println("Can't parse " + country + " unit limits: " + unitLimits);
                        }
                        String[] leaderLimits = limits[leaderNumber].trim().split("/");
                        for (String leaderLimit : leaderLimits) {
                            if (leaderLimit.startsWith("(") || leaderLimit.startsWith("[")) {
                                continue;
                            }
                            String number = leaderLimit.substring(0, leaderLimit.indexOf("\\"));
                            String leader = getLeaderType(leaderLimit.substring(leaderLimit.indexOf("\\")));
                            if (!StringUtils.isEmpty(leader)) {
                                if (StringUtils.isEmpty(number)) {
                                    number = "1";
                                }
                                addLimitLine(sqlWriter, country, period, Integer.parseInt(number), leader);
                            }
                        }
                    }
                } else if (StringUtils.equals("maxima", type)) {
                    if (previousLine != null) {
                        line = previousLine + line;
                        previousLine = null;
                    }

                    if (line.endsWith("&")) {
                        previousLine = line;
                        continue;
                    }

                    m = Pattern.compile("\\d{4}\\-+\\d{4} ([IV]+) *&(.*)").matcher(line);
                    if (m.matches()) {
                        String period = m.group(1);
                        String[] limits = m.group(2).split("&");

                        addLimitLine(sqlWriter, country, period, getLimitNumber(limits[0]), "MAX_DTI");
                        if (limits[1].contains("/")) {
                            addLimitLine(sqlWriter, country, period, getLimitNumber(limits[1].split("/")[0]), "MAX_FTI");
                            addLimitLine(sqlWriter, country, period, getLimitNumber(limits[1].split("/")[1]), "MAX_FTI_ROTW");
                        } else {
                            addLimitLine(sqlWriter, country, period, getLimitNumber(limits[1]), "MAX_FTI");
                        }
                        addLimitLine(sqlWriter, country, period, getLimitNumber(limits[2]), "MAX_MNU");
                        addLimitLine(sqlWriter, country, period, getLimitNumber(limits[3]), "MAX_COL");
                        addLimitLine(sqlWriter, country, period, getLimitNumber(limits[4]), "MAX_TP");
                        addLimitLine(sqlWriter, country, period, getLimitNumber(limits[5]), "MAX_ND");
                        int fleetMinus = 6;
                        int fleetPlus = 7;
                        int artillery = 8;
                        if (StringUtils.equals("suede", country) || StringUtils.equals("pologne", country)) {
                            fleetMinus = 7;
                            fleetPlus = 8;
                            artillery = 9;
                        }
                        if (!StringUtils.equals("---", limits[fleetMinus].trim())) {
                            addLimitLine(sqlWriter, country, period, getLimitNumber(limits[fleetMinus].split("/")[0]), "MAX_ND_F_MOINS");
                            addLimitLine(sqlWriter, country, period, getLimitNumber(limits[fleetMinus].split("/")[1]), "MAX_NTR_F_MOINS");
                        }
                        if (!StringUtils.equals("---", limits[fleetPlus].trim())) {
                            addLimitLine(sqlWriter, country, period, getLimitNumber(limits[fleetPlus].split("/")[0]), "MAX_ND_F_PLUS");
                            addLimitLine(sqlWriter, country, period, getLimitNumber(limits[fleetPlus].split("/")[1]), "MAX_NTR_F_PLUS");
                        }
                        addLimitLine(sqlWriter, country, period, getLimitNumber(limits[artillery]), "ARTILLERY_A_PLUS");
                    }
                }
            }
        }
    }

    /**
     * Transform an input tech into a Pair of Tech/flag for land tech.
     *
     * @param input the tech in the input format.
     * @return a Pair of Tech/flag for land tech.
     */
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

    /**
     * Return the unit type given an index and a tech flag.
     *
     * @param index      of the input file.
     * @param isTechLand flag that says if it is a tech land.
     * @return the unit type.
     */
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

    /**
     * Return the unit action given an index and a tech flag.
     *
     * @param index      of the input file.
     * @param isTechLand flag that says if it is a tech land.
     * @return the unit action.
     */
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

    /**
     * Transforms the input into a number for a limit.
     *
     * @param input input.
     * @return the number.
     */
    private static Integer getLimitNumber(String input) {
        Integer number = 0;

        String toNumber = input.trim();

        if (toNumber.contains("\\xx")) {
            toNumber = toNumber.substring(0, toNumber.indexOf("\\xx"));
        }

        if (toNumber.contains("{")) {
            toNumber = toNumber.substring(0, toNumber.indexOf('{'));
        }

        if (toNumber.contains("(")) {
            toNumber = toNumber.substring(0, toNumber.indexOf('('));
        }

        if (toNumber.contains("[")) {
            toNumber = toNumber.substring(0, toNumber.indexOf('['));
        }

        if (toNumber.contains("+")) {
            toNumber = toNumber.substring(0, toNumber.indexOf('+'));
        }

        if (StringUtils.equals("\\f", toNumber)) {
            // TODO manage 1/2
            return 0;
        }

        toNumber = toNumber.trim();

        // special case holland
        if (StringUtils.equals("1/2", toNumber)) {
            toNumber = "1";
        }

        try {
            number = Integer.parseInt(toNumber);
        } catch (Exception e) {
            System.out.println(input + " -> " + toNumber);
        }

        return number;
    }

    /**
     * Transforms the input into a leader type for a limit.
     *
     * @param input input
     * @return the leader type.
     */
    private static String getLeaderType(String input) {
        String type = null;

        String toType = input.trim().substring(1).trim();

        toType = toType.replaceAll("\\$\\\\!\\$\\\\?", "");

        if (toType.contains("\\")) {
            toType = toType.substring(0, toType.indexOf("\\")).trim();
        }

        switch (toType) {
            case "LeaderG":
                type = "LEADER_GENERAL";
                break;
            case "LeaderG$":
                type = "LEADER_GENERAL_AMERICA";
                break;
            case "LeaderA":
                type = "LEADER_ADMIRAL";
                break;
            case "LeaderC":
                type = "LEADER_CONQUISTADOR";
                break;
            case "LeaderC@":
                type = "LEADER_CONQUISTADOR_INDIA";
                break;
            case "LeaderE":
                type = "LEADER_EXPLORER";
                break;
            case "LeaderGov":
                type = "LEADER_GOVERNOR";
                break;
            default:
                System.out.println("Can't parse " + input + " -> " + toType);
                break;
        }

        return type;
    }

    /**
     * Transforms the input into a type for a basic force.
     *
     * @param input input
     * @return the type.
     */
    private static String getBasicForceType(String input) {
        String type;

        String toType = input.trim();

        if (toType.contains("\\xx")) {
            toType = toType.substring(0, toType.indexOf("\\xx")).trim();
        }

        switch (toType) {
            case "\\ARMY\\faceplus":
                type = "ARMY_PLUS";
                break;
            case "\\ARMY\\facemoins":
                type = "ARMY_MINUS";
                break;
            case "\\FLEET\\faceplus":
                type = "FLEET_PLUS";
                break;
            case "\\FLEET\\facemoins":
                type = "FLEET_MINUS";
                break;
            case "Tr\\FLEET\\faceplus":
                type = "FLEET_TRANSPORT_PLUS";
                break;
            case "\\GD":
                type = "LDND";
                break;
            case "\\LD":
                type = "LD";
                break;
            case "\\corsaire":
                type = "P_PLUS";
                break;
//            case "\\ND":
//            case "\\NTD":
//            case "\\VGD":
//                type = "ND";
//                break;
//            case "\\corsaire\\faceplus":
//                type = "P_PLUS";
//                break;
//            case "\\corsaire\\facemoins":
//                type = "P_MINUS";
//                break;
//            case "\\LDND":
//                type = "LDND";
//                break;
//            case "\\NDE":
//                type = "DE";
//                break;
            default:
                throw new RuntimeException(input);
        }

        return type;
    }

    /**
     * Creates an insert for a unit.
     *
     * @param sqlWriter where to write the db instructions.
     * @param country   owning the unit.
     * @param tech      of the unit.
     * @param price     of the unit.
     * @param type      of the unit.
     * @param action    of the unit.
     * @param special   if the unit is special.
     * @param id        of the unit.
     * @return the next id of the unit.
     * @throws IOException if the writer fails.
     */
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
     * Creates an insert for a basic force.
     *
     * @param sqlWriter where to write the db instructions.
     * @param country   owning the basic force.
     * @param period    of the basic force.
     * @param number    of the basic force.
     * @param type      of the basic force.
     * @throws IOException if the writer fails.
     */
    private static void addBasicForceLine(Writer sqlWriter, String country, String period, Integer number, String type) throws IOException {
        sqlWriter.append("INSERT INTO T_BASIC_FORCE (R_COUNTRY, ID_PERIOD, NUMBER, TYPE)\n" +
                "    VALUES (")
                .append(stringToString(country)).append(", ")
                .append("(SELECT ID FROM T_PERIOD WHERE NAME = ").append(stringToString(period)).append("), ")
                .append(integerToInteger(number)).append(", ")
                .append(stringToString(type)).append(");\n");
    }

    /**
     * Creates an insert for a limit.
     *
     * @param sqlWriter where to write the db instructions.
     * @param country   owning the limit.
     * @param period    of the limit.
     * @param number    of the limit.
     * @param type      of the limit.
     * @throws IOException if the writer fails.
     */
    private static void addLimitLine(Writer sqlWriter, String country, String period, Integer number, String type) throws IOException {
        sqlWriter.append("INSERT INTO T_LIMIT (R_COUNTRY, ID_PERIOD, NUMBER, TYPE)\n" +
                "    VALUES (")
                .append(stringToString(country)).append(", ")
                .append("(SELECT ID FROM T_PERIOD WHERE NAME = ").append(stringToString(period)).append("), ")
                .append(integerToInteger(number)).append(", ")
                .append(stringToString(type)).append(");\n");
    }

    /**
     * Compute the general tables.
     *
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException
     */
    public static void computeGeneralTables(Writer sqlWriter) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(DataExtractor.class.getClassLoader().getResourceAsStream("input/tables/engGeneralTables.tex")));
        String line;
        String type = null;
        Integer id = 1;
        String previousLine = null;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            Matcher m = Pattern.compile("\\\\newcommand\\{\\\\foreigntrade\\}\\{").matcher(line);
            if (m.matches()) {
                type = "foreigntrade";
            }
            m = Pattern.compile("\\\\newcommand\\{\\\\domestictrade\\}\\{").matcher(line);
            if (m.matches()) {
                type = "domestictrade";
            }
            m = Pattern.compile("\\\\newcommand\\{\\\\admintbl\\}\\{").matcher(line);
            if (m.matches()) {
                type = "adminresults";
            }
            if (line.equals("}")) {
                type = null;
            }

            if (StringUtils.equals("foreigntrade", type) || StringUtils.equals("domestictrade", type)) {
                boolean foreign = StringUtils.equals("foreigntrade", type);
                m = Pattern.compile("(\\\\leq|\\d+|\\\\geq|\\\\textgreatequal)-?-?(\\d+)\\s*&\\s*(\\d*)\\s*&\\s*(\\d*)\\s*&\\s*(\\d*)\\s*&\\s*(\\d*)\\s*&\\s*(\\d*).*").matcher(line);
                if (m.matches()) {
                    Integer startTrade;
                    String startTradeString = m.group(1);
                    Integer endTrade = Integer.parseInt(m.group(2));
                    Integer valueTrade1 = Integer.parseInt(m.group(3));
                    Integer valueTrade2 = Integer.parseInt(m.group(4));
                    Integer valueTrade3 = Integer.parseInt(m.group(5));
                    Integer valueTrade4 = Integer.parseInt(m.group(6));
                    Integer valueTrade5 = Integer.parseInt(m.group(7));

                    if (StringUtils.equals("\\leq", startTradeString)) {
                        startTrade = null;
                    } else if (StringUtils.equals("\\geq", startTradeString) || StringUtils.equals("\\textgreatequal", startTradeString)) {
                        startTrade = endTrade;
                        endTrade = null;
                    } else {
                        startTrade = Integer.parseInt(startTradeString);
                    }

                    addTradeLine(sqlWriter, 1, startTrade, endTrade, valueTrade1, foreign);
                    addTradeLine(sqlWriter, 2, startTrade, endTrade, valueTrade2, foreign);
                    addTradeLine(sqlWriter, 3, startTrade, endTrade, valueTrade3, foreign);
                    addTradeLine(sqlWriter, 4, startTrade, endTrade, valueTrade4, foreign);
                    addTradeLine(sqlWriter, 5, startTrade, endTrade, valueTrade5, foreign);
                }
            } else if (StringUtils.equals("adminresults", type)) {
                String possibilites = "F\\\\textetoile|F|\\\\undemi|\\\\undemi\\s*\\\\textetoile|S|S\\\\textetoile";
                m = Pattern.compile("[\\\\a-z]*(\\d+)&(" + possibilites + ")&(" + possibilites + ")&(" + possibilites + ")&(" + possibilites + ")" +
                        "&(" + possibilites + ")&(" + possibilites + ")&(" + possibilites + ")&(" + possibilites + ")&(" + possibilites + ")" +
                        "(\\\\\\\\\\\\ghline)?").matcher(line);
                if (m.matches()) {
                    Integer die = Integer.parseInt(m.group(1));
                    addAdminResultLine(sqlWriter, die, -4, possibilityToResult(m.group(2)));
                    addAdminResultLine(sqlWriter, die, -3, possibilityToResult(m.group(3)));
                    addAdminResultLine(sqlWriter, die, -2, possibilityToResult(m.group(4)));
                    addAdminResultLine(sqlWriter, die, -1, possibilityToResult(m.group(5)));
                    addAdminResultLine(sqlWriter, die, 0, possibilityToResult(m.group(6)));
                    addAdminResultLine(sqlWriter, die, 1, possibilityToResult(m.group(7)));
                    addAdminResultLine(sqlWriter, die, 2, possibilityToResult(m.group(8)));
                    addAdminResultLine(sqlWriter, die, 3, possibilityToResult(m.group(9)));
                    addAdminResultLine(sqlWriter, die, 4, possibilityToResult(m.group(10)));
                }
            }
        }
    }

    /**
     * @param possibility in the table input file.
     * @return the AdminActionResultEnum given a possibility.
     */
    private static String possibilityToResult(String possibility) {
        String result = null;

        if (StringUtils.isNotEmpty(possibility)) {
            String tmp = possibility.replaceAll("\\s", "");

            if (StringUtils.equals("F\\textetoile", tmp)) {
                result = "FUMBLE";
            } else if (StringUtils.equals("F", tmp)) {
                result = "FAILED";
            } else if (StringUtils.equals("\\undemi", tmp)) {
                result = "AVERAGE";
            } else if (StringUtils.equals("\\undemi\\textetoile", tmp)) {
                result = "AVERAGE_PLUS";
            } else if (StringUtils.equals("S", tmp)) {
                result = "SUCCESS";
            } else if (StringUtils.equals("S\\textetoile", tmp)) {
                result = "CRITICAL_HIT";
            }
        }

        return result;
    }

    /**
     * Creates an insert for a foreign trade.
     *
     * @param sqlWriter    where to write the db instructions.
     * @param countryValue DTI/FTI of the country.
     * @param min          minimum value of foreign/domestic trade.
     * @param max          maximum value of the foreign/domestic trade.
     * @param value        income computed.
     * @param foreign      flag saying if it is foreign or domestic trade.
     * @throws IOException if the writer fails.
     */
    private static void addTradeLine(Writer sqlWriter, Integer countryValue, Integer min, Integer max, Integer value, boolean foreign) throws IOException {
        sqlWriter.append("INSERT INTO T_TRADE (COUNTRY_VALUE, MIN_VALUE, MAX_VALUE, VALUE, FOREIGN_TRADE)\n" +
                "    VALUES (")
                .append(integerToInteger(countryValue)).append(", ")
                .append(integerToInteger(min)).append(", ")
                .append(integerToInteger(max)).append(", ")
                .append(integerToInteger(value)).append(", ")
                .append(booleanToBit(foreign)).append(");\n");
    }

    /**
     * Creates an insert for a result.
     *
     * @param sqlWriter where to write the db instructions.
     * @param die       modified die roll.
     * @param column    column used.
     * @param result    result of the action.
     * @throws IOException if the writer fails.
     */
    private static void addAdminResultLine(Writer sqlWriter, Integer die, Integer column, String result) throws IOException {
        sqlWriter.append("INSERT INTO T_RESULT (DIE, `COLUMN`, RESULT)\n" +
                "    VALUES (")
                .append(integerToInteger(die)).append(", ")
                .append(integerToInteger(column)).append(", ")
                .append(stringToString(result)).append(");\n");
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
