package com.mkl.tools.eu.tables;

import com.mkl.tools.eu.map.DataExtractor;
import com.mkl.tools.eu.util.ToolsUtil;
import com.mkl.tools.eu.vo.Leader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates sql for the tables.
 *
 * @author MKL.
 */
public class TablesGenerator {
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TablesGenerator.class);
    /** Leaders without image. */
    private static final List<String> leadersWithoutImage;

    static {
        // TODO TG-152 cannot import leaders that have no image
        leadersWithoutImage = new ArrayList<>();
        leadersWithoutImage.add("van Bylandt");
        leadersWithoutImage.add("Prince Waldek");
        leadersWithoutImage.add("van Zuylen van Nijevelt");
        leadersWithoutImage.add("Kutuzov");
        leadersWithoutImage.add("Oruc Reis");
        leadersWithoutImage.add("Morosini");
        leadersWithoutImage.add("Juel");
        leadersWithoutImage.add("Malahayati");
        leadersWithoutImage.add("Selman Reis");
        leadersWithoutImage.add("J. Cabot");
        leadersWithoutImage.add("S. Cabot");
        leadersWithoutImage.add("Bart");
        leadersWithoutImage.add("Dumouriez");
        leadersWithoutImage.add("Estrees");
        leadersWithoutImage.add("Forbin");
        leadersWithoutImage.add("Jourdan");
        leadersWithoutImage.add("Marceau");
        leadersWithoutImage.add("Duguay-Trouin");
        leadersWithoutImage.add("Cassard");
        leadersWithoutImage.add("Kleber");
        leadersWithoutImage.add("Kellermann");
        leadersWithoutImage.add("Hoche");
        leadersWithoutImage.add("Massena");
        leadersWithoutImage.add("Joubert");
        leadersWithoutImage.add("Pichegru");
        leadersWithoutImage.add("Moreau");
        leadersWithoutImage.add("Brueys");
        leadersWithoutImage.add("Villeneuve");
        leadersWithoutImage.add("Johann Georg I");
        leadersWithoutImage.add("J De la Gardie");
        leadersWithoutImage.add("Printz");
        leadersWithoutImage.add("Dobeln");
        leadersWithoutImage.add("Coeuvres");
        leadersWithoutImage.add("Esnambuc");
        leadersWithoutImage.add("Suffren");
        leadersWithoutImage.add("Tremoille");
        leadersWithoutImage.add("Selim");
        leadersWithoutImage.add("Ozdemir");
        leadersWithoutImage.add("Ali Pasha");
        leadersWithoutImage.add("Husain Pasha");
        leadersWithoutImage.add("Borovinic");
        leadersWithoutImage.add("Ibrahim");
        leadersWithoutImage.add("Kurtoglu M");
        leadersWithoutImage.add("Salih Reis");
        leadersWithoutImage.add("Uluj Ali");
        leadersWithoutImage.add("Siroco");
        leadersWithoutImage.add("Mezzomorto");
        leadersWithoutImage.add("Montiano");
        leadersWithoutImage.add("Bertendona");
        leadersWithoutImage.add("Oquendo");
        leadersWithoutImage.add("Blas de Lezo");
        leadersWithoutImage.add("Kirke");
        leadersWithoutImage.add("Hughes");
        leadersWithoutImage.add("La Bourdonnais");
        leadersWithoutImage.add("La Galissonniere");
        leadersWithoutImage.add("Osman");
        leadersWithoutImage.add("Yusuf Sinan");
        leadersWithoutImage.add("Brouwer");
        leadersWithoutImage.add("Johan Maurits");
        leadersWithoutImage.add("Rupertroy");
        leadersWithoutImage.add("Burji1");
        leadersWithoutImage.add("Estaingpriv");
        leadersWithoutImage.add("Sinan");
        leadersWithoutImage.add("K Braunschweig");
    }

    /**
     * Main.
     *
     * @param args none.
     * @throws Exception Exception.
     */
    public static void main(String... args) throws Exception {
        Writer sqlWriter = ToolsUtil.createFileWriter("src/main/resources/output/tables-auto.sql", false);

        sqlWriter.append("DELETE FROM T_UNIT;\n")
                .append("ALTER TABLE T_UNIT AUTO_INCREMENT = 1;\n")
                .append("DELETE FROM T_BASIC_FORCE;\n")
                .append("ALTER TABLE T_BASIC_FORCE AUTO_INCREMENT = 1;\n")
                .append("DELETE FROM T_LIMIT;\n")
                .append("ALTER TABLE T_LIMIT AUTO_INCREMENT = 1;\n")
                .append("DELETE FROM T_TRADE;\n")
                .append("ALTER TABLE T_TRADE AUTO_INCREMENT = 1;\n")
                .append("DELETE FROM T_RESULT;\n")
                .append("ALTER TABLE T_RESULT AUTO_INCREMENT = 1;\n")
                .append("DELETE FROM T_BATTLE_TECH;\n")
                .append("ALTER TABLE T_BATTLE_TECH AUTO_INCREMENT = 1;\n")
                .append("DELETE FROM T_COMBAT_RESULT;\n")
                .append("ALTER TABLE T_COMBAT_RESULT AUTO_INCREMENT = 1;\n")
                .append("DELETE FROM T_ARMY_CLASS;\n")
                .append("ALTER TABLE T_ARMY_CLASS AUTO_INCREMENT = 1;\n")
                .append("DELETE FROM T_ARMY_ARTILLERY;\n")
                .append("ALTER TABLE T_ARMY_ARTILLERY AUTO_INCREMENT = 1;\n")
                .append("DELETE FROM T_ARTILLERY_SIEGE;\n")
                .append("ALTER TABLE T_ARTILLERY_SIEGE AUTO_INCREMENT = 1;\n")
                .append("DELETE FROM T_FORTRESS_RESISTANCE;\n")
                .append("ALTER TABLE T_FORTRESS_RESISTANCE AUTO_INCREMENT = 1;\n")
                .append("DELETE FROM T_ASSAULT_RESULT;\n")
                .append("ALTER TABLE T_ASSAULT_RESULT AUTO_INCREMENT = 1;\n")
                .append("DELETE FROM T_EXCHEQUER;\n")
                .append("ALTER TABLE T_EXCHEQUER AUTO_INCREMENT = 1;\n")
                .append("DELETE FROM T_LEADER;\n")
                .append("ALTER TABLE T_LEADER AUTO_INCREMENT = 1;\n")
                .append("\n");

        computeCountryTables(sqlWriter);
        computeGeneralTables(sqlWriter);
        computeLeaders(sqlWriter);

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
                            LOGGER.error("Can't parse " + country + " unit limits: " + unitLimits);
                        }
                        String[] leaderLimits = limits[leaderNumber].trim().split("/");
                        for (String leaderLimit : leaderLimits) {
                            if (leaderLimit.startsWith("(") || leaderLimit.startsWith("[")) {
                                continue;
                            }
                            String number = leaderLimit.substring(0, leaderLimit.indexOf("\\"));
                            String leader = getLeaderTypeLimit(leaderLimit.substring(leaderLimit.indexOf("\\")));
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
            LOGGER.error(input + " -> " + toNumber);
        }

        return number;
    }

    /**
     * Transforms the input into a leader type for a limit.
     *
     * @param input input
     * @return the leader type.
     */
    private static String getLeaderTypeLimit(String input) {
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
                LOGGER.error("Can't parse " + input + " -> " + toType);
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
     * @throws IOException if the writer fails.
     */
    public static void computeGeneralTables(Writer sqlWriter) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(DataExtractor.class.getClassLoader().getResourceAsStream("input/tables/engGeneralTables.tex")));
        String line;
        String type = null;
        String pendingLine = null;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            type = getGeneralTableType(line, type);

            if (StringUtils.equals("foreigntrade", type) || StringUtils.equals("domestictrade", type)) {
                computeTrade(line, type, sqlWriter);
            } else if (StringUtils.equals("adminresults", type)) {
                computeAdminResult(line, sqlWriter);
            } else if (StringUtils.equals("navaltech", type) || StringUtils.equals("landtech", type)) {
                computeBattleTech(line, type, sqlWriter);
            } else if (StringUtils.equals("combatresults", type)) {
                pendingLine = computeCombatResult(line, pendingLine, sqlWriter);
            } else if (StringUtils.equals("armyclasses", type)) {
                computeArmyClass(line, sqlWriter);
            } else if (StringUtils.equals("artilleryvalue", type)) {
                computeArmyArtillery(line, sqlWriter);
            } else if (StringUtils.equals("artillerybonus", type)) {
                computeArtilleryBonus(line, sqlWriter);
            } else if (StringUtils.equals("fortressResistance", type)) {
                computeFortressResistance(line, sqlWriter);
            } else if (StringUtils.equals("assault", type)) {
                computeAssaultResult(line, sqlWriter);
            } else if (StringUtils.equals("etatsauvrai", type)) {
                computeExchequer(line, sqlWriter);
            } else if (StringUtils.equals("leader", type)) {
                computeReplacementLeader(line, sqlWriter);
            }
        }
    }

    /**
     * @param line         to check.
     * @param previousType previous type before parsing this line.
     * @return the type of table to compute given the line.
     */
    private static String getGeneralTableType(String line, String previousType) {
        String type = previousType;
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
        m = Pattern.compile("\\\\newcommand\\{\\\\navaltech\\}\\{").matcher(line);
        if (m.matches()) {
            type = "navaltech";
        }
        m = Pattern.compile("\\\\newcommand\\{\\\\landtech\\}\\{").matcher(line);
        if (m.matches()) {
            type = "landtech";
        }
        m = Pattern.compile("\\\\newcommand\\{\\\\combatresults\\}\\{").matcher(line);
        if (m.matches()) {
            type = "combatresults";
        }
        m = Pattern.compile("\\\\newcommand\\{\\\\armyclasses\\}\\{").matcher(line);
        if (m.matches()) {
            type = "armyclasses";
        }
        m = Pattern.compile("\\\\newcommand\\{\\\\artilleryvalue\\}\\{").matcher(line);
        if (m.matches()) {
            type = "artilleryvalue";
        }
        m = Pattern.compile("\\\\newcommand\\{\\\\artillerybonus\\}\\{").matcher(line);
        if (m.matches()) {
            type = "artillerybonus";
        }
        m = Pattern.compile("\\\\GTmorecontent\\{fortresses\\}\\{.*").matcher(line);
        if (m.matches()) {
            type = "fortressResistance";
        }
        m = Pattern.compile("\\\\newcommand\\{\\\\assault\\}\\{").matcher(line);
        if (m.matches()) {
            type = "assault";
        }
        m = Pattern.compile("\\\\newcommand\\{\\\\etatsauvrai\\}\\{").matcher(line);
        if (m.matches()) {
            type = "etatsauvrai";
        }
        m = Pattern.compile("\\\\newcommand\\{\\\\replacement\\}\\{").matcher(line);
        if (m.matches()) {
            type = "leader";
        }
        if (line.equals("}")) {
            type = null;
        }
        return type;
    }

    /**
     * Creates the trade tables insertion for this line.
     *
     * @param line      the line to compute.
     * @param type      type of block.
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException if the writer fails.
     */
    private static void computeTrade(String line, String type, Writer sqlWriter) throws IOException {
        boolean foreign = StringUtils.equals("foreigntrade", type);
        Matcher m = Pattern.compile("(\\\\leq|\\d+|\\\\geq|\\\\textgreatequal)-?-?(\\d+)\\s*&\\s*(\\d*)\\s*&\\s*(\\d*)\\s*&\\s*(\\d*)\\s*&\\s*(\\d*)\\s*&\\s*(\\d*).*").matcher(line);
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
     * Creates the admin result tables insertion for this line.
     *
     * @param line      the line to compute.
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException if the writer fails.
     */
    private static void computeAdminResult(String line, Writer sqlWriter) throws IOException {
        String possibilites = "F\\\\textetoile|F|\\\\undemi|\\\\undemi\\s*\\\\textetoile|S|S\\\\textetoile";
        Matcher m = Pattern.compile("[\\\\a-z]*(\\d+)&(" + possibilites + ")&(" + possibilites + ")&(" + possibilites + ")&(" + possibilites + ")" +
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
     * Creates the battle tech tables insertion for this line.
     *
     * @param line      the line to compute.
     * @param type      type of block.
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException if the writer fails.
     */
    private static void computeBattleTech(String line, String type, Writer sqlWriter) throws IOException {
        boolean land = StringUtils.equals("landtech", type);
        Matcher m = Pattern.compile("\\\\technologie\\{([^\\}]*)\\}.*").matcher(line);
        if (m.matches()) {
            String tech = m.group(1);
            String[] split = line.split("&");
            if (land && split.length != 9) {
                LOGGER.error("Land tech battle line should have 9 columns. " + line);
            }
            if (!land && split.length != 10) {
                LOGGER.error("Naval tech battle line should have 10 columns. " + line);
            }
            int moral = 0;
            m = Pattern.compile("^(\\d).*").matcher(split[split.length - 1].trim());
            if (m.matches()) {
                moral = Integer.parseInt(m.group(1));
            } else {
                LOGGER.error("Can't retrieve moral for battle tech. " + line);
            }
            m = Pattern.compile(".*\\\\textdag.*").matcher(split[split.length - 1]);
            boolean bonus = land || m.matches();
            for (int i = 1; i < split.length - 1; i++) {
                String s = split[i];
                m = Pattern.compile(".*([ABCDE-])/.*([ABCDE]).*").matcher(s);
                if (m.matches()) {
                    addBattleTechLine(sqlWriter, transformTech(tech), getTech(land, i), land, m.group(1), m.group(2), moral, bonus);
                } else {
                    LOGGER.error("Can't parse battle tech. " + s);
                }
            }
        }
    }

    /**
     * Creates an insert for a result.
     *
     * @param sqlWriter   where to write the db instructions.
     * @param techFor     the technology of the stack.
     * @param techAgainst the technology facing the stack.
     * @param land        if it is land battle.
     * @param columnFire  fire column of the stack.
     * @param columnShock shock column of the stack.
     * @param moral       moral of the stack.
     * @param bonus       bonus moral if the stack is veteran.
     * @throws IOException if the writer fails.
     */
    private static void addBattleTechLine(Writer sqlWriter, String techFor, String techAgainst, boolean land,
                                          String columnFire, String columnShock, int moral, boolean bonus) throws IOException {
        if (StringUtils.equals("-", columnFire)) {
            columnFire = null;
        }
        if (StringUtils.equals("-", columnShock)) {
            columnShock = null;
        }
        sqlWriter.append("INSERT INTO T_BATTLE_TECH (TECH_FOR, `TECH_AGAINST`, LAND, COLUMN_FIRE, COLUMN_SHOCK, MORAL, MORAL_BONUS_VETERAN)\n" +
                "    VALUES (")
                .append(stringToString(techFor)).append(", ")
                .append(stringToString(techAgainst)).append(", ")
                .append(booleanToBit(land)).append(", ")
                .append(stringToString(columnFire)).append(", ")
                .append(stringToString(columnShock)).append(", ")
                .append(integerToInteger(moral)).append(", ")
                .append(booleanToBit(bonus)).append(");\n");
    }

    /**
     * @param text the tech name in another format.
     * @return the tech name given the text.
     */
    private static String transformTech(String text) {
        String tech = null;

        switch (text) {
            case "Galley":
                tech = "GALLEY";
                break;
            case "Carrack":
                tech = "CARRACK";
                break;
            case "Nao-Galeon":
                tech = "NAE_GALEON";
                break;
            case "Galleon-Fluyt":
                tech = "GALLEON_FLUYT";
                break;
            case "Battery":
                tech = "BATTERY";
                break;
            case "Vessel":
                tech = "VESSEL";
                break;
            case "Three-Decker":
                tech = "THREE_DECKER";
                break;
            case "74s":
                tech = "SEVENTY_FOUR";
                break;
            case "Medieval":
                tech = "MEDIEVAL";
                break;
            case "Renaissance":
                tech = "RENAISSANCE";
                break;
            case "Arquebus":
                tech = "ARQUEBUS";
                break;
            case "Muskets":
                tech = "MUSKET";
                break;
            case "Baroque":
                tech = "BAROQUE";
                break;
            case "Manoeuvre":
                tech = "MANOEUVRE";
                break;
            case "Lace":
                tech = "LACE_WAR";
                break;
        }

        return tech;
    }

    /**
     * @param land  if the tech is land.
     * @param index index in the line.
     * @return the tech name given its index.
     */
    private static String getTech(boolean land, int index) {
        String tech = null;
        if (land) {
            index += 10;
        }

        switch (index) {
            case 1:
                tech = "GALLEY";
                break;
            case 2:
                tech = "CARRACK";
                break;
            case 3:
                tech = "NAE_GALEON";
                break;
            case 4:
                tech = "GALLEON_FLUYT";
                break;
            case 5:
                tech = "BATTERY";
                break;
            case 6:
                tech = "VESSEL";
                break;
            case 7:
                tech = "THREE_DECKER";
                break;
            case 8:
                tech = "SEVENTY_FOUR";
                break;
            case 11:
                tech = "MEDIEVAL";
                break;
            case 12:
                tech = "RENAISSANCE";
                break;
            case 13:
                tech = "ARQUEBUS";
                break;
            case 14:
                tech = "MUSKET";
                break;
            case 15:
                tech = "BAROQUE";
                break;
            case 16:
                tech = "MANOEUVRE";
                break;
            case 17:
                tech = "LACE_WAR";
                break;
        }

        return tech;
    }

    /**
     * Creates the combat result table insertions for this line.
     *
     * @param line      the line to compute.
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException if the writer fails.
     */
    private static String computeCombatResult(String line, String pendingLine, Writer sqlWriter) throws IOException {
        if (line.endsWith("ghline")) {
            String[] split = line.split("&");
            if (split.length == 11) {
                int dice = Integer.parseInt(split[0].replace("\\leq", "").replace("\\geq", "").trim());

                addBattleResultLine("A", dice, split[1], split[2], sqlWriter);
                addBattleResultLine("B", dice, split[3], split[4], sqlWriter);
                addBattleResultLine("C", dice, split[5], split[6], sqlWriter);
                addBattleResultLine("D", dice, split[7], split[8], sqlWriter);
                addBattleResultLine("E", dice, split[9], split[10], sqlWriter);

                return null;
            } else {

                return computeCombatResult((pendingLine != null ? pendingLine : "") + line, null, sqlWriter);
            }
        }

        return (pendingLine != null ? pendingLine : "") + line;
    }

    /**
     * Creates the combat result table insertions for this line.
     *
     * @param column    the technology column.
     * @param dice      the modified dice.
     * @param army      the part of the line concerning round and third losses.
     * @param moral     the part of the line concerning moral losses.
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException if the writer fails.
     */
    private static void addBattleResultLine(String column, int dice, String army, String moral, Writer sqlWriter) throws IOException {
        Integer roundLoss = 0;
        Matcher m = Pattern.compile(".*(\\d).*").matcher(army);
        if (m.matches()) {
            roundLoss = Integer.parseInt(m.group(1));
        }
        Integer thirdLoss = 0;
        if (army.contains("\\td")) {
            thirdLoss = 2;
        } else if (army.contains("\\tu")) {
            thirdLoss = 1;
        }
        Integer moralLoss = StringUtils.countMatches(moral, "\\textetoilex");
        addBattleResultLine(sqlWriter, column, dice, roundLoss, thirdLoss, moralLoss);
    }

    /**
     * Creates an insert for a result.
     *
     * @param sqlWriter where to write the db instructions.
     * @param column    the column resulting from the technology.
     * @param dice      the result of the modified dice.
     * @param roundLoss the number of round losses.
     * @param thirdLoss the number of third losses.
     * @param moralLoss the number of moral losses.
     * @throws IOException if the writer fails.
     */
    private static void addBattleResultLine(Writer sqlWriter, String column, int dice, Integer roundLoss,
                                            Integer thirdLoss, Integer moralLoss) throws IOException {
        sqlWriter.append("INSERT INTO T_COMBAT_RESULT (`COLUMN`, DICE, ROUNDLOSS, THIRDLOSS, MORALELOSS)\n" +
                "    VALUES (")
                .append(stringToString(column)).append(", ")
                .append(integerToInteger(dice)).append(", ")
                .append(integerToInteger(roundLoss)).append(", ")
                .append(integerToInteger(thirdLoss)).append(", ")
                .append(integerToInteger(moralLoss)).append(");\n");
    }

    /**
     * Creates the combat result table insertions for this line.
     *
     * @param line      the line to compute.
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException if the writer fails.
     */
    private static void computeArmyClass(String line, Writer sqlWriter) throws IOException {
        Matcher m = Pattern.compile("\\\\CA([^&]*)&[^&]*&(\\d)&(\\d)&(\\d)&(\\d)&(\\d)&(\\d)&(\\d)&.*").matcher(line);
        if (m.matches()) {
            String armyClass = m.group(1);

            addArmyClassLine(sqlWriter, armyClass, "I", Integer.parseInt(m.group(2)));
            addArmyClassLine(sqlWriter, armyClass, "II", Integer.parseInt(m.group(3)));
            addArmyClassLine(sqlWriter, armyClass, "III", Integer.parseInt(m.group(4)));
            addArmyClassLine(sqlWriter, armyClass, "IV", Integer.parseInt(m.group(5)));
            addArmyClassLine(sqlWriter, armyClass, "V", Integer.parseInt(m.group(6)));
            addArmyClassLine(sqlWriter, armyClass, "VI", Integer.parseInt(m.group(7)));
            addArmyClassLine(sqlWriter, armyClass, "VII", Integer.parseInt(m.group(8)));
        }
    }

    /**
     * Creates an insert for a result.
     *
     * @param sqlWriter where to write the db instructions.
     * @param armyClass the class of the army.
     * @param period    the period.
     * @param size      the size of the army.
     * @throws IOException if the writer fails.
     */
    private static void addArmyClassLine(Writer sqlWriter, String armyClass, String period, int size) throws IOException {
        sqlWriter.append("INSERT INTO T_ARMY_CLASS (CLASS, PERIOD, SIZE)\n" +
                "    VALUES (")
                .append(stringToString(armyClass)).append(", ")
                .append(stringToString(period)).append(", ")
                .append(integerToInteger(size)).append(");\n");
    }

    /**
     * Creates the combat result table insertions for this line.
     *
     * @param line      the line to compute.
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException if the writer fails.
     */
    private static void computeArmyArtillery(String line, Writer sqlWriter) throws IOException {
        Matcher m = Pattern.compile("([^&]*)&([^&]*)&([^&]*)&([^&]*)&([^&]*)&([^&]*)&([^&]*)&([^&]*)\\\\\\\\\\\\ghline.*").matcher(line);
        if (m.matches()) {
            String header = m.group(1);
            List<String> classes = extractClassesFromArmyArtilleryHeader(header);

            if (classes.isEmpty()) {
                String country = extractCountryFromArmyArtilleryHeader(header);
                if (StringUtils.isNotEmpty(country)) {
                    addArmyArtilleryLine(sqlWriter, country, null, "I", NumberUtils.toInt(m.group(2)));
                    addArmyArtilleryLine(sqlWriter, country, null, "II", NumberUtils.toInt(m.group(3)));
                    addArmyArtilleryLine(sqlWriter, country, null, "III", NumberUtils.toInt(m.group(4)));
                    addArmyArtilleryLine(sqlWriter, country, null, "IV", NumberUtils.toInt(m.group(5)));
                    addArmyArtilleryLine(sqlWriter, country, null, "V", NumberUtils.toInt(m.group(6)));
                    addArmyArtilleryLine(sqlWriter, country, null, "VI", NumberUtils.toInt(m.group(7)));
                    addArmyArtilleryLine(sqlWriter, country, null, "VII", NumberUtils.toInt(m.group(8)));
                }
            } else {
                for (String armyClass : classes) {
                    addArmyArtilleryLine(sqlWriter, null, armyClass, "I", NumberUtils.toInt(m.group(2)));
                    addArmyArtilleryLine(sqlWriter, null, armyClass, "II", NumberUtils.toInt(m.group(3)));
                    addArmyArtilleryLine(sqlWriter, null, armyClass, "III", NumberUtils.toInt(m.group(4)));
                    addArmyArtilleryLine(sqlWriter, null, armyClass, "IV", NumberUtils.toInt(m.group(5)));
                    addArmyArtilleryLine(sqlWriter, null, armyClass, "V", NumberUtils.toInt(m.group(6)));
                    addArmyArtilleryLine(sqlWriter, null, armyClass, "VI", NumberUtils.toInt(m.group(7)));
                    addArmyArtilleryLine(sqlWriter, null, armyClass, "VII", NumberUtils.toInt(m.group(8)));
                }
            }
        }
    }

    /**
     * Creates the artillery bonus table insertions for this line.
     *
     * @param line      the line to compute.
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException if the writer fails.
     */
    private static void computeArtilleryBonus(String line, Writer sqlWriter) throws IOException {
        Matcher m = Pattern.compile("&([^&]*)&([^&]*)&([^&]*)&([^&]*)&([^&]*)&([^&]*)&.*\\+([^&]*)\\\\\\\\.*").matcher(line);
        if (m.matches()) {
            int bonus = NumberUtils.toInt(m.group(7).trim());
            for (int i = 0; i < 6; i++) {
                addArtilleryBonusLine(sqlWriter, i, NumberUtils.toInt(m.group(i + 1).trim()), bonus);
            }
        }
    }

    /**
     * Creates the fortress resistance table insertions for this line.
     *
     * @param line      the line to compute.
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException if the writer fails.
     */
    private static void computeFortressResistance(String line, Writer sqlWriter) throws IOException {
        Matcher m = Pattern.compile("([^&]*)&([^&]*)&([^&]*)&([^&]*)&([^&]*)&([^&]*)&([^&]*).*").matcher(line);
        if (m.matches()) {
            String title = m.group(1).trim();
            if (StringUtils.equals("Level", title)) {
                return;
            }
            boolean breach = !StringUtils.equals("Resistance", title);
            for (int i = 0; i < 6; i++) {
                String res = m.group(i + 2).trim();
                int third = 0;
                int round = 0;
                Matcher m2 = Pattern.compile(".*(\\d).*").matcher(res);
                if (m2.matches()) {
                    round = Integer.parseInt(m2.group(1));
                }
                if (res.contains("\\td")) {
                    third = 2;
                } else if (res.contains("\\tu")) {
                    third = 1;
                }

                addFortressResistanceLine(sqlWriter, i, round, third, breach);
            }
        }
    }

    /**
     * Creates the fortress resistance table insertions for this line.
     *
     * @param line      the line to compute.
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException if the writer fails.
     */
    private static void computeAssaultResult(String line, Writer sqlWriter) throws IOException {
        Matcher m = Pattern.compile("([^&]*)&([^&]*)&([^&]*)&([^&]*)&([^&]*)&([^&]*)&([^&]*)&([^&]*)&([^&]*)&([^&]*)&([^&]*)\\\\\\\\\\\\ghline.*").matcher(line);
        if (m.matches()) {
            String dieString = m.group(1);
            dieString = dieString.replace("\\leq", "").replace("\\geq", "").trim();
            int die = NumberUtils.toInt(dieString);
            computeAssaultResult(sqlWriter, die, true, m.group(2), m.group(3), false, false);
            computeAssaultResult(sqlWriter, die, true, m.group(4), m.group(3), true, false);
            computeAssaultResult(sqlWriter, die, false, m.group(5), m.group(6), false, false);
            computeAssaultResult(sqlWriter, die, false, m.group(7), m.group(6), true, false);
            computeAssaultResult(sqlWriter, die, true, m.group(8), m.group(9), false, true);
            computeAssaultResult(sqlWriter, die, false, m.group(10), m.group(11), false, true);
        }
    }

    private static void computeAssaultResult(Writer sqlWriter, int die, boolean fire, String result, String moral, boolean breach, boolean besieger) throws IOException {
        Integer roundLoss = 0;
        Matcher m = Pattern.compile(".*(\\d).*").matcher(result);
        if (m.matches()) {
            roundLoss = Integer.parseInt(m.group(1));
        }
        Integer thirdLoss = 0;
        if (result.contains("\\td")) {
            thirdLoss = 2;
        } else if (result.contains("\\tu")) {
            thirdLoss = 1;
        }
        Integer moralLoss = StringUtils.countMatches(moral, "\\textetoilex");
        addAssaultResultLine(sqlWriter, die, fire, roundLoss, thirdLoss, moralLoss, breach, besieger);
    }

    private static String extractCountryFromArmyArtilleryHeader(String header) {
        String country = null;

        header = header.trim();

        switch (header) {
            case "\\VEN":
                country = "venise";
                break;
            case "\\HOL":
                country = "hollande";
                break;
            case "\\HAB":
                country = "habsbourg";
                break;
            case "\\POR":
                country = "portugal";
                break;
            case "\\SUE":
                country = "suede";
                break;
            case "\\SPA":
                country = "espagne";
                break;
            case "\\FRA":
                country = "france";
                break;
            case "\\ENG":
                country = "angleterre";
                break;
            case "\\TUR":
                country = "turquie";
                break;
            case "\\RUS":
                country = "russie";
                break;
            case "\\POL":
                country = "pologne";
                break;
            case "\\PRU":
                country = "prusse";
                break;
            case "Minor":
                country = "minor";
                break;
            case "Vizier":
                country = "vizier";
                break;
            case "Natives":
                country = "natives";
                break;
            default:
                if (!header.startsWith("\\\\quad")) {
                    LOGGER.error("Can't parse country for army artillery : " + header);
                }
                break;
        }

        return country;
    }

    private static List<String> extractClassesFromArmyArtilleryHeader(String header) {
        List<String> armyClasses = new ArrayList<>();

        Matcher m = Pattern.compile(".*\\\\quad.*\\\\CA([^\\s/\\\\]*).*\\\\CA([^\\s/\\\\]*).").matcher(header);
        if (m.matches()) {
            String armyClass = m.group(1);
            armyClasses.add(armyClass);
            armyClass = m.group(2);
            armyClasses.add(armyClass);
        } else {
            m = Pattern.compile(".*\\\\quad.*\\\\CA([^\\s/\\\\]*).*").matcher(header);
            if (m.matches()) {
                String armyClass = m.group(1);
                armyClasses.add(armyClass);
            }
        }

        return armyClasses;
    }

    /**
     * Creates an insert for a result.
     *
     * @param sqlWriter where to write the db instructions.
     * @param country   the name of the owner of the army.
     * @param armyClass the class of the army.
     * @param period    the period.
     * @param artillery the number of artillery of the army.
     * @throws IOException if the writer fails.
     */
    private static void addArmyArtilleryLine(Writer sqlWriter, String country, String armyClass, String period, int artillery) throws IOException {
        sqlWriter.append("INSERT INTO T_ARMY_ARTILLERY (R_COUNTRY, CLASS, PERIOD, ARTILLERY)\n" +
                "    VALUES (")
                .append(stringToString(country)).append(", ")
                .append(stringToString(armyClass)).append(", ")
                .append(stringToString(period)).append(", ")
                .append(integerToInteger(artillery)).append(");\n");
    }

    /**
     * Creates an insert for a result.
     *
     * @param sqlWriter where to write the db instructions.
     * @param fortress  the level of the fortress.
     * @param artillery the number of artillery of the army.
     * @param bonus     the bonus given by the artllery on the fortress.
     * @throws IOException if the writer fails.
     */
    private static void addArtilleryBonusLine(Writer sqlWriter, int fortress, int artillery, int bonus) throws IOException {
        sqlWriter.append("INSERT INTO T_ARTILLERY_SIEGE (FORTRESS, ARTILLERY, BONUS)\n" +
                "    VALUES (")
                .append(integerToInteger(fortress)).append(", ")
                .append(integerToInteger(artillery)).append(", ")
                .append(integerToInteger(bonus)).append(");\n");
    }

    /**
     * Creates an insert for a result.
     *
     * @param sqlWriter where to write the db instructions.
     * @param fortress  the level of the fortress.
     * @param round     the resistance of the fortress (round number).
     * @param third     the resistance of the fortress (third number).
     * @param breach    if the fortress is breached.
     * @throws IOException if the writer fails.
     */
    private static void addFortressResistanceLine(Writer sqlWriter, int fortress, int round, int third, boolean breach) throws IOException {
        sqlWriter.append("INSERT INTO T_FORTRESS_RESISTANCE (FORTRESS, ROUND, THIRD, BREACH)\n" +
                "    VALUES (")
                .append(integerToInteger(fortress)).append(", ")
                .append(integerToInteger(round)).append(", ")
                .append(integerToInteger(third)).append(", ")
                .append(booleanToBit(breach)).append(");\n");
    }

    /**
     * Creates an insert for a result.
     *
     * @param sqlWriter where to write the db instructions.
     * @param die       the die roll.
     * @param fire      fire or shock phase.
     * @param roundLoss the round losses.
     * @param thirdLoss the third losses.
     * @param moral     the moral loss.
     * @param breach    if the fortress is breached.
     * @param besieger  if it si the besieger.
     * @throws IOException if the writer fails.
     */
    private static void addAssaultResultLine(Writer sqlWriter, int die, boolean fire, int roundLoss, int thirdLoss, int moral, boolean breach, boolean besieger) throws IOException {
        sqlWriter.append("INSERT INTO T_ASSAULT_RESULT (DICE, FIRE, ROUNDLOSS, THIRDLOSS, MORALELOSS, BREACH, BESIEGER)\n" +
                "    VALUES (")
                .append(integerToInteger(die)).append(", ")
                .append(booleanToBit(fire)).append(", ")
                .append(integerToInteger(roundLoss)).append(", ")
                .append(integerToInteger(thirdLoss)).append(", ")
                .append(integerToInteger(moral)).append(", ")
                .append(booleanToBit(breach)).append(", ")
                .append(booleanToBit(besieger)).append(");\n");
    }

    /**
     * Creates the exchequer table insertion for this line.
     *
     * @param line      the line to compute.
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException if the writer fails.
     */
    private static void computeExchequer(String line, Writer sqlWriter) throws IOException {
        Matcher m = Pattern.compile("([^&]*)&\\s*(\\d*)\\s*&\\s*(\\d*)\\s*&\\s*(\\d*)\\s*&\\s*(\\d*)\\s*\\\\%.*").matcher(line);
        if (m.matches()) {
            addExchequerLine(sqlWriter, possibilityToResult(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)));
        }
    }

    /**
     * Creates an insert for a exchequer.
     *
     * @param sqlWriter         where to write the db instructions.
     * @param result            the result of the exchequer test.
     * @param regular           the regular income.
     * @param prestige          the prestige income.
     * @param nationalLoan      the maximum national loan.
     * @param internationalLoan the maximum international loan.
     * @throws IOException if the writer fails.
     */
    private static void addExchequerLine(Writer sqlWriter, String result, int regular, int prestige, int nationalLoan, int internationalLoan) throws IOException {
        sqlWriter.append("INSERT INTO T_EXCHEQUER (RESULT, REGULAR, PRESTIGE, NATLOAN, INTERLOAN)\n" +
                "    VALUES (")
                .append(stringToString(result)).append(", ")
                .append(integerToInteger(regular)).append(", ")
                .append(integerToInteger(prestige)).append(", ")
                .append(integerToInteger(nationalLoan)).append(", ")
                .append(integerToInteger(internationalLoan)).append(");\n");
    }

    /**
     * Creates the replacement leader table insertion for this line.
     *
     * @param line      the line to compute.
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException if the writer fails.
     */
    private static void computeReplacementLeader(String line, Writer sqlWriter) throws IOException {
        Matcher m = Pattern.compile("([^&]*)&\\s*(\\d{3})\\s*&\\s*(\\d{3})\\s*&\\s*(\\d{3})\\s*&\\s*(\\d{3})\\s*&\\s*(\\d{3})\\s*&\\s*(\\d{3})\\s*&\\s*(\\d{3})\\s*&\\s*(\\d{3})\\s*&\\s*(\\d{3})\\s*&\\s*(\\d{3}).*").matcher(line);
        if (m.matches()) {
            String country = m.group(1).trim();
            boolean badAdmiralManoeuvre = country.contains("\\xxa");
            boolean badAdmiralFire = country.contains("\\xxb");
            boolean goodArtiller = country.contains("\\xxc");
            if (country.contains("\\x")) {
                country = country.substring(0, country.indexOf("\\x"));
            }
            country = extractCountryFromArmyArtilleryHeader(country);
            for (int i = 1; i <= 10; i++) {
                Matcher mStats = Pattern.compile("(\\d)(\\d)(\\d)").matcher(m.group(i + 1));
                if (mStats.matches()) {
                    int manoeuvre = Integer.parseInt(mStats.group(1));
                    int fire = Integer.parseInt(mStats.group(2));
                    int shock = Integer.parseInt(mStats.group(3));
                    int siege = 0;
                    if (goodArtiller && i % 2 == 1) {
                        siege++;
                    }
                    String code = country + "-general-" + i;
                    addLeaderLine(sqlWriter, code, null, code, country, null, null, null, null, manoeuvre, fire, shock, siege, "GENERAL", true, false, false, false, false, false, true, null);
                    code = country + "-admiral-" + i;
                    if (badAdmiralManoeuvre) {
                        manoeuvre--;
                    }
                    if (badAdmiralFire) {
                        fire--;
                    }
                    addLeaderLine(sqlWriter, code, null, code, country, null, null, null, null, manoeuvre, fire, shock, 0, "ADMIRAL", true, false, false, false, false, false, true, null);
                } else {
                    System.out.println("Bad Stats : " + m.group(i + 1));
                }
            }
        }
    }

    /**
     * Compute the leader.
     *
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException if the writer fails.
     */
    public static List<Leader> computeLeaders(Writer sqlWriter) throws IOException {
        List<Leader> leaders = new ArrayList<>();
        leaders.addAll(computeNamedLeaders(sqlWriter));
        leaders.addAll(computeAnonymousLeaders(sqlWriter));
        return leaders;
    }

    /**
     * Compute the named leaders.
     *
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException if the writer fails.
     */
    private static List<Leader> computeNamedLeaders(Writer sqlWriter) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(DataExtractor.class.getClassLoader().getResourceAsStream("input/tables/leaders.txt")));
        String line;
        List<Leader> leaders = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            Matcher m = Pattern.compile("LEADER(|DOUBLE);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);(.*)").matcher(line);
            if (m.matches()) {
                String value = m.group(11);
                Matcher mVal = Pattern.compile("([A-Z]) (\\d).(\\d).(\\d) ?\\-?(\\d)?").matcher(value);
                if (mVal.matches()) {
                    String code = m.group(2);
                    if (leadersWithoutImage.contains(code)) {
                        continue;
                    }
                    String country = m.group(9);
                    String beginGroup = m.group(7);
                    String endGroup = m.group(8);
                    Integer begin = null;
                    Integer end = null;
                    String event = null;
                    if (StringUtils.isNumeric(beginGroup) && StringUtils.isNumeric(endGroup)) {
                        begin = Integer.parseInt(beginGroup);
                        end = Integer.parseInt(endGroup);
                    } else {
                        event = beginGroup + "-" + endGroup;
                    }
                    String rank = mVal.group(1);
                    int manoeuvre = Integer.parseInt(mVal.group(2));
                    int fire = Integer.parseInt(mVal.group(3));
                    int shock = Integer.parseInt(mVal.group(4));
                    String possibleSiege = mVal.group(5);
                    int siege = 0;
                    if (StringUtils.isNotEmpty(possibleSiege)) {
                        siege = Integer.parseInt(possibleSiege);
                    }
                    String typeValue = m.group(10);
                    String type = getLeaderType(typeValue);
                    boolean rotw = typeValue.contains("R");
                    boolean asia = typeValue.contains("@");
                    boolean america = typeValue.contains("$");
                    boolean mediterranee = typeValue.contains("m");
                    boolean privateer = typeValue.contains("P");
                    boolean main = typeValue.contains("*");

                    boolean doubleLeader = StringUtils.isNotEmpty(m.group(1));
                    String otherCode = doubleLeader ? code + "-2" : null;

                    if (sqlWriter != null) {
                        addLeaderLine(sqlWriter, code, otherCode, code, country, event, begin, end, rank,
                                manoeuvre, fire, shock, siege, type, rotw, asia, america, mediterranee, privateer, main, false, null);
                    }
                    Leader leader = new Leader();
                    leader.setType(doubleLeader ? Leader.LeaderType.LEADERDOUBLE : Leader.LeaderType.LEADER);
                    leader.setCode(code);
                    leader.setCountry(country);
                    leaders.add(leader);

                    if (doubleLeader) {
                        String value2 = m.group(13);
                        Matcher mVal2 = Pattern.compile("([A-Z]) (\\d).(\\d).(\\d) ?\\-?(\\d)?").matcher(value2);
                        if (mVal2.matches()) {
                            rank = mVal2.group(1);
                            manoeuvre = Integer.parseInt(mVal2.group(2));
                            fire = Integer.parseInt(mVal2.group(3));
                            shock = Integer.parseInt(mVal2.group(4));
                            possibleSiege = mVal2.group(5);
                            siege = 0;
                            if (StringUtils.isNotEmpty(possibleSiege)) {
                                siege = Integer.parseInt(possibleSiege);
                            }
                            String typeValue2 = m.group(12);
                            type = getLeaderType(typeValue2);
                            rotw = typeValue2.contains("R");
                            asia = typeValue2.contains("@");
                            america = typeValue2.contains("%");
                            mediterranee = typeValue2.contains("m");
                            privateer = typeValue2.contains("P");
                            main = typeValue2.contains("*");

                            if (sqlWriter != null) {
                                addLeaderLine(sqlWriter, otherCode, code, code, country, event, begin, end, rank,
                                        manoeuvre, fire, shock, siege, type, rotw, asia, america, mediterranee, privateer, main, false, null);
                            }
                        }
                    }
                }
            }
            m = Pattern.compile("LEADER(PAIRE);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);(.*)").matcher(line);
            if (m.matches()) {
                String value = m.group(11);
                Matcher mVal = Pattern.compile("([A-Z]) (\\d).(\\d).(\\d) ?\\-?(\\d)?").matcher(value);
                if (mVal.matches()) {
                    String code = m.group(2);
                    if (leadersWithoutImage.contains(code)) {
                        continue;
                    }
                    String country = m.group(9);
                    String beginGroup = m.group(7);
                    String endGroup = m.group(8);
                    Integer begin = null;
                    Integer end = null;
                    String event = null;
                    if (StringUtils.isNumeric(beginGroup) && StringUtils.isNumeric(endGroup)) {
                        begin = Integer.parseInt(beginGroup);
                        end = Integer.parseInt(endGroup);
                    } else {
                        event = beginGroup + "-" + endGroup;
                    }
                    String rank = mVal.group(1);
                    int manoeuvre = Integer.parseInt(mVal.group(2));
                    int fire = Integer.parseInt(mVal.group(3));
                    int shock = Integer.parseInt(mVal.group(4));
                    String possibleSiege = mVal.group(5);
                    int siege = 0;
                    if (StringUtils.isNotEmpty(possibleSiege)) {
                        siege = Integer.parseInt(possibleSiege);
                    }
                    String typeValue = m.group(10);
                    String type = getLeaderType(typeValue);
                    boolean rotw = typeValue.contains("R");
                    boolean asia = typeValue.contains("@");
                    boolean america = typeValue.contains("%");
                    boolean mediterranee = typeValue.contains("m");
                    boolean privateer = typeValue.contains("P");
                    boolean main = typeValue.contains("*");

                    String otherCode = code + "-2";

                    if (sqlWriter != null) {
                        addLeaderLine(sqlWriter, code, otherCode, code, country, event, begin, end, rank,
                                manoeuvre, fire, shock, siege, type, rotw, asia, america, mediterranee, privateer, main, false, null);
                    }
                    Leader leader = new Leader();
                    leader.setType(Leader.LeaderType.LEADERPAIRE);
                    leader.setCode(code);
                    leader.setCountry(country);
                    leader.setCode2(m.group(15));
                    leader.setCountry2(m.group(22));
                    leaders.add(leader);

                    String value2 = m.group(24);
                    Matcher mVal2 = Pattern.compile("([A-Z]) (\\d).(\\d).(\\d) ?\\-?(\\d)?").matcher(value2);
                    if (mVal2.matches()) {
                        country = m.group(22);
                        beginGroup = m.group(20);
                        endGroup = m.group(21);
                        begin = null;
                        end = null;
                        event = null;
                        if (StringUtils.isNumeric(beginGroup) && StringUtils.isNumeric(endGroup)) {
                            begin = Integer.parseInt(beginGroup);
                            end = Integer.parseInt(endGroup);
                        } else {
                            event = beginGroup + "-" + endGroup;
                        }
                        rank = mVal2.group(1);
                        manoeuvre = Integer.parseInt(mVal2.group(2));
                        fire = Integer.parseInt(mVal2.group(3));
                        shock = Integer.parseInt(mVal2.group(4));
                        possibleSiege = mVal2.group(5);
                        siege = 0;
                        if (StringUtils.isNotEmpty(possibleSiege)) {
                            siege = Integer.parseInt(possibleSiege);
                        }
                        String typeValue2 = m.group(23);
                        type = getLeaderType(typeValue2);
                        rotw = typeValue2.contains("R");
                        asia = typeValue2.contains("@");
                        america = typeValue2.contains("%");
                        mediterranee = typeValue2.contains("m");
                        privateer = typeValue2.contains("P");
                        main = typeValue2.contains("*");

                        if (sqlWriter != null) {
                            addLeaderLine(sqlWriter, otherCode, code, code, country, event, begin, end, rank,
                                    manoeuvre, fire, shock, siege, type, rotw, asia, america, mediterranee, privateer, main, false, null);
                        }
                    } else {
                        System.out.println("Double leader second side : " + value2);
                    }
                }
            }
        }
        return leaders;
    }

    /**
     * Compute the anonymous leaders.
     *
     * @param sqlWriter the writer with all database instructions.
     * @throws IOException if the writer fails.
     */
    private static List<Leader> computeAnonymousLeaders(Writer sqlWriter) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(DataExtractor.class.getClassLoader().getResourceAsStream("input/tables/anonymes.txt")));
        String line;
        List<Leader> leaders = new ArrayList<>();
        List<String> countriesWithAdmiralZero = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            Matcher m = Pattern.compile("LEADERANONYMOUS(|DOUBLE);\\?([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);?(.*)").matcher(line);
            if (m.matches()) {
                String value = m.group(7);
                Matcher mVal = Pattern.compile("([A-Z]) (\\d).(\\d).(\\d) ?\\-?(\\d)?").matcher(value);
                if (mVal.matches()) {
                    String country = m.group(3);
                    String beginGroup = m.group(4);
                    String endGroup = m.group(5);
                    Integer begin = null;
                    Integer end = null;
                    String event = null;
                    if (StringUtils.isNumeric(beginGroup) && StringUtils.isNumeric(endGroup)) {
                        begin = Integer.parseInt(beginGroup);
                        end = Integer.parseInt(endGroup);
                    } else {
                        event = beginGroup + "-" + endGroup;
                    }
                    String rank = mVal.group(1);
                    int manoeuvre = Integer.parseInt(mVal.group(2));
                    int fire = Integer.parseInt(mVal.group(3));
                    int shock = Integer.parseInt(mVal.group(4));
                    String possibleSiege = mVal.group(5);
                    int siege = 0;
                    if (StringUtils.isNotEmpty(possibleSiege)) {
                        siege = Integer.parseInt(possibleSiege);
                    }
                    String typeValue = m.group(6);
                    String type = getLeaderType(typeValue);
                    boolean rotw = typeValue.contains("R");
                    boolean asia = typeValue.contains("@");
                    boolean america = typeValue.contains("%");
                    boolean mediterranee = typeValue.contains("m");
                    boolean privateer = typeValue.contains("P");
                    boolean main = typeValue.contains("*");
                    String suffix = m.group(2);
                    String anonymType = getAnonymousLeaderType(type);
                    if (StringUtils.equals("A", anonymType)) {
                        Integer number = Integer.parseInt(suffix);
                        if (number == 0) {
                            countriesWithAdmiralZero.add(country);
                        }
                        if (countriesWithAdmiralZero.contains(country)) {
                            suffix = number + 1 + "";
                        }
                    }
                    String code = "Anonymous-" + anonymType + suffix;

                    boolean doubleLeader = StringUtils.isNotEmpty(m.group(1));
                    String otherCode = doubleLeader ? code + "-2" : null;

                    if (sqlWriter != null) {
                        addLeaderLine(sqlWriter, code, otherCode, code, country, event, begin, end, rank,
                                manoeuvre, fire, shock, siege, type, rotw, asia, america, mediterranee, privateer, main, true, null);
                    }
                    Leader leader = new Leader();
                    leader.setType(doubleLeader ? Leader.LeaderType.LEADERPAIRE : Leader.LeaderType.LEADER);
                    leader.setCode(code);
                    leader.setCountry(country);
                    leader.setCountry2(doubleLeader ? m.group(9) : null);
                    leaders.add(leader);

                    if (doubleLeader) {
                        country = m.group(9);

                        if (sqlWriter != null) {
                            addLeaderLine(sqlWriter, otherCode, code, code, country, event, begin, end, rank,
                                    manoeuvre, fire, shock, siege, type, rotw, asia, america, mediterranee, privateer, main, true, null);
                        }
                    }
                }
            } else {
                m = Pattern.compile("PACHA;([^;]*);([^;]*);([^;]*);(\\d);(.*)").matcher(line);
                if (m.matches()) {
                    String value = m.group(5);
                    Matcher mVal = Pattern.compile("([A-Z]) (\\d).(\\d).(\\d) ?\\-?(\\d)?").matcher(value);
                    if (mVal.matches()) {
                        String country = "turquie";
                        String rank = mVal.group(1);
                        int manoeuvre = Integer.parseInt(mVal.group(2));
                        int fire = Integer.parseInt(mVal.group(3));
                        int shock = Integer.parseInt(mVal.group(4));
                        String possibleSiege = mVal.group(5);
                        int siege = 0;
                        if (StringUtils.isNotEmpty(possibleSiege)) {
                            siege = Integer.parseInt(possibleSiege);
                        }
                        String type = "PACHA";
                        String code = m.group(1) + "_" + m.group(2);
                        String otherCode = code + "-2";
                        Integer size = Integer.parseInt(m.group(4));

                        if (sqlWriter != null) {
                            addLeaderLine(sqlWriter, code, otherCode, code, country, null, null, null, rank,
                                    manoeuvre, fire, shock, siege, type, false, false, false, false, false, true, true, size);
                            addLeaderLine(sqlWriter, otherCode, code, code, country, null, null, null, null,
                                    0, 0, 0, 0, type, false, false, false, false, false, false, true, null);
                        }
                        Leader leader = new Leader();
                        leader.setType(Leader.LeaderType.PACHA);
                        leader.setCode(code);
                        leader.setCountry(country);
                        leaders.add(leader);
                    }
                }
            }
        }
        return leaders;
    }

    /**
     * @param value string to parse.
     * @return the leader type.
     */
    private static String getLeaderType(String value) {
        if (value.startsWith("G")) {
            return "GENERAL";
        } else if (value.startsWith("A") || value.startsWith("d")) {
            return "ADMIRAL";
        } else if (value.startsWith("C")) {
            return "CONQUISTADOR";
        } else if (value.startsWith("E")) {
            return "EXPLORER";
        } else if (value.startsWith("I")) {
            return "ENGINEER";
        } else if (value.startsWith("K")) {
            return "KING";
        } else if (value.startsWith("g")) {
            return "GOVERNOR";
        } else if (value.startsWith("P")) {
            return "PRIVATEER";
        }
        LOGGER.error("Leader type unknown : " + value);
        return null;
    }

    /**
     * @param type the leader type.
     * @return the initial of the leader type most of the time.
     */
    private static String getAnonymousLeaderType(String type) {
        switch (type) {
            case "GENERAL":
                return "G";
            case "ADMIRAL":
                return "A";
            case "CONQUISTADOR":
                return "C";
            case "EXPLORER":
                return "E";
            case "GOVERNOR":
                return "G";
            case "PRIVATEER":
                return "P";
        }
        LOGGER.error("Anonymous leader type unknown : " + type);
        return null;
    }

    /**
     * Insert a leader database insert line.
     *
     * @param sqlWriter    where to write the db instructions.
     * @param code         of the leader.
     * @param otherCode    code of the other side of the leader.
     * @param name         of the leader.
     * @param country      of the leader.
     * @param event        of the leader.
     * @param begin        of the leader.
     * @param end          of the leader.
     * @param rank         of the leader.
     * @param manoeuvre    of the leader.
     * @param fire         of the leader.
     * @param shock        of the leader.
     * @param siege        of the leader.
     * @param type         of the leader.
     * @param rotw         of the leader.
     * @param asia         of the leader.
     * @param america      of the leader.
     * @param mediterranee of the leader.
     * @param privateer    of the leader.
     * @param main         of the leader.
     * @param anonymous    of the leader.
     * @param size         of the leader.
     * @throws IOException if the writer fails.
     */
    private static void addLeaderLine(Writer sqlWriter, String code, String otherCode, String name, String country, String event,
                                      Integer begin, Integer end, String rank, int manoeuvre, int fire, int shock, int siege,
                                      String type, boolean rotw, boolean asia, boolean america, boolean mediterranee, boolean privateer, boolean main, boolean anonymous, Integer size) throws IOException {
        sqlWriter.append("INSERT INTO T_LEADER (CODE, T_LEADER, NAME, R_COUNTRY, EVENT, BEGIN, END, " +
                "RANK, MANOEUVRE, FIRE, SHOCK, SIEGE, TYPE, ROTW, ASIA, AMERICA, MEDITERRANEE, PRIVATEER, MAIN, ANONYMOUS, SIZE)\n" +
                "    VALUES (")
                .append(stringToString(code)).append(", ")
                .append(stringToString(otherCode)).append(", ")
                .append(stringToString(name)).append(", ")
                .append(stringToString(country)).append(", ")
                .append(stringToString(event)).append(", ")
                .append(integerToInteger(begin)).append(", ")
                .append(integerToInteger(end)).append(", ")
                .append(stringToString(rank)).append(", ")
                .append(integerToInteger(manoeuvre)).append(", ")
                .append(integerToInteger(fire)).append(", ")
                .append(integerToInteger(shock)).append(", ")
                .append(integerToInteger(siege)).append(", ")
                .append(stringToString(type)).append(", ")
                .append(booleanToBit(rotw)).append(", ")
                .append(booleanToBit(asia)).append(", ")
                .append(booleanToBit(america)).append(", ")
                .append(booleanToBit(mediterranee)).append(", ")
                .append(booleanToBit(privateer)).append(", ")
                .append(booleanToBit(main)).append(", ")
                .append(booleanToBit(anonymous)).append(", ")
                .append(integerToInteger(size)).append(");\n");
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
