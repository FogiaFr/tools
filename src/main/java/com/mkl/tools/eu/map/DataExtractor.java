package com.mkl.tools.eu.map;

import com.mkl.tools.eu.util.ToolsUtil;
import com.mkl.tools.eu.vo.country.Country;
import com.mkl.tools.eu.vo.province.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class that gather all extraction data from external files.
 *
 * @author MKL.
 */
public class DataExtractor {
    /** Size of a square. */
    private static final int SQUARE_SIZE = 113;

    /**
     * Extract the paths data in order to have the provinces shapes and borders.
     *
     * @param provinces      List of provinces shapes.
     * @param specialBorders List of special borders.
     * @param aliases        the aliases.
     * @param inputFile      Name of the file to parse.
     * @param rotw           flag saying that the file is for the ROTW map.
     * @param log            log writer.
     * @throws IOException exception.
     */
    public static void extractPaths(Map<String, Province> provinces, Map<String, List<Path>> specialBorders,
                                    Map<String, Map<String, List<String>>> aliases, String inputFile, boolean rotw, Writer log) throws IOException {
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(DataExtractor.class.getClassLoader().getResourceAsStream(inputFile)));
        Map<String, Path> paths = new HashMap<>();
        Path currentPath = null;
        List<Path> pathsBorder = null;
        boolean specialBordersParsing = true;
        boolean zoomParsing = false;
        Pattern mer = Pattern.compile("/mer\\d+ beginpath.*");
        Pattern bord = Pattern.compile("/bord\\d+ beginpath.*");
        Pattern path = Pattern.compile("/path\\d+ beginpath.*");
        Pattern multiPath = Pattern.compile("\\s*(/mer|/path)\\d+ .*");
        Pattern square = Pattern.compile("\\s*(\\d{4}) (\\d{4}) \\d\\([^\\)]*\\)\\(([^\\)]*)\\)\\([^\\)]*\\) (true|false) carre\\s*");
        while ((line = reader.readLine()) != null) {
            if (mer.matcher(line).matches() || bord.matcher(line).matches() || path.matcher(line).matches()) {
                currentPath = new Path(line.split(" ")[0], !line.contains("contpath"), rotw);
            } else if (line.startsWith("endpath") && currentPath != null) {
                paths.put(currentPath.getName(), currentPath);
                currentPath = null;
            } else if (currentPath != null) {
                String[] coords = line.split(" ");
                for (int i = 0; i < line.length() - 1; i = i + 2) {
                    try {
                        currentPath.getCoords().add(new ImmutablePair<>(Integer.parseInt(coords[i]), Integer.parseInt(coords[i + 1])));
                    } catch (NumberFormatException e) {
                        break;
                    }
                }
            } else if (line.startsWith("/prov")) {
                addSubProvince(line, provinces, paths, aliases, rotw, zoomParsing, log);
            } else if (line.startsWith("%#%% Zoom")) {
                zoomParsing = true;
            } else if (specialBordersParsing && StringUtils.equals("[", line.trim())) {
                pathsBorder = new ArrayList<>();
            } else if (line.startsWith("} if % river / pass / strait")) {
                specialBordersParsing = false;
                pathsBorder = null;
            } else if (line.endsWith("change pathtype to river")) {
                if (!specialBorders.containsKey("RIVER")) {
                    specialBorders.put("RIVER", new ArrayList<>());
                }
                specialBorders.get("RIVER").addAll(pathsBorder);
                pathsBorder = new ArrayList<>();
            } else if (line.endsWith("change pathtype to pass")) {
                if (!specialBorders.containsKey("MOUNTAIN_PASS")) {
                    specialBorders.put("MOUNTAIN_PASS", new ArrayList<>());
                }
                specialBorders.get("MOUNTAIN_PASS").addAll(pathsBorder);
                pathsBorder = new ArrayList<>();
            } else if (line.endsWith("change pathtype to strait")) {
                if (!specialBorders.containsKey("STRAITS")) {
                    specialBorders.put("STRAITS", new ArrayList<>());
                }
                specialBorders.get("STRAITS").addAll(pathsBorder);
                pathsBorder = new ArrayList<>();
            } else if (specialBordersParsing && multiPath.matcher(line).matches()) {
                String[] specialsBorder = line.trim().split(" ");
                for (String specialBorder : specialsBorder) {
                    Path pathBorder = paths.get(specialBorder);
                    if (pathBorder == null) {
                        log.append(specialBorder).append("\tBorder not found\n");
                    } else {
                        pathsBorder.add(pathBorder);
                    }
                }
            } else if (square.matcher(line).matches()) {
                addSquare(line, provinces, aliases, rotw, zoomParsing, log);
            }
        }

        reader.close();
    }

    /**
     * Adds a portion to an existing province.
     *
     * @param line        to parse.
     * @param provinces   existing provinces.
     * @param paths       list of paths.
     * @param aliases     the aliases.
     * @param rotw        flag saying that the subProvince is for the ROTW map.
     * @param zoomParsing flag saying that the subProvince is in a zoom (for Europe map).
     * @param log         log writer.
     * @throws IOException exception.
     */
    private static void addSubProvince(String line, Map<String, Province> provinces, Map<String, Path> paths,
                                       Map<String, Map<String, List<String>>> aliases, boolean rotw, boolean zoomParsing, Writer log) throws IOException {
        Matcher m = Pattern.compile(".*\\((.*)\\) ?ppdef.*").matcher(line);
        if (!m.matches()) {
            return;
        }
        String provinceName = m.group(1);
        boolean secondary = false;
        if (provinceName.startsWith("*")) {
            provinceName = provinceName.substring(1);
            secondary = true;
        }
        if (StringUtils.equals("Caption", provinceName) || StringUtils.equals("Special", provinceName) || StringUtils.isEmpty(provinceName)
                || provinceName.startsWith("Zone") || StringUtils.equals("SaintEmpire", provinceName)
                || StringUtils.equals("zone", line.split(" ")[2])) {
            return;
        }
        provinceName = getRealProvinceName(provinceName, aliases, line.split(" ")[1], log);
        if (provinceName == null) {
            return;
        }
        Province province = provinces.get(provinceName);
        if (province == null) {
            province = new Province(provinceName, log);
            provinces.put(provinceName, province);
        }
        SubProvince portion = new SubProvince(line.split(" ")[1], secondary, rotw);
        m = Pattern.compile("(/path\\d+ AR?)|(/bord\\d+ AR?)|(/mer\\d+ AR?)|(carre[a-zA-Z]+ AR?)").matcher(line);
        while (m.find()) {
            String string = m.group();
            Path pathFound = paths.get(string.split(" ")[0]);
            if (pathFound != null) {
                portion.getPaths().add(new DirectedPath(pathFound, string.split(" ")[1].contains("R")));
            } else {
                log.append(province.getName()).append("\t").append("Path not found").append("\t").append(string.split(" ")[0]).append("\n");
            }
        }

        if ((zoomParsing && !portion.isLight() && !portion.isSecondary())
                || (!province.getPortions().isEmpty() && province.getPortions().get(0).isSecondary() && !portion.isSecondary())
                || (!province.getPortions().isEmpty() && province.getPortions().get(0).isLight() && !portion.isLight())) {
            province.getPortions().add(0, portion);
        } else {
            province.getPortions().add(portion);
        }
    }

    /**
     * Adds a portion to an existing province.
     *
     * @param line        to parse.
     * @param provinces   existing provinces.
     * @param aliases     the aliases.
     * @param rotw        flag saying that the subProvince is for the ROTW map.
     * @param zoomParsing flag saying that the subProvince is in a zoom (for Europe map).
     * @param log         log writer.
     * @throws IOException exception.
     */
    private static void addSquare(String line, Map<String, Province> provinces,
                                  Map<String, Map<String, List<String>>> aliases, boolean rotw, boolean zoomParsing, Writer log) throws IOException {
        Matcher m = Pattern.compile("\\s*(\\d{4}) (\\d{4}) \\d\\([^\\)]*\\)\\(([^\\)]*)\\)\\(([^\\)]*)\\) (true|false) carre\\s*").matcher(line);
        if (!m.matches()) {
            return;
        }
        int x = Integer.parseInt(m.group(1)) - SQUARE_SIZE / 2;
        int y = Integer.parseInt(m.group(2)) + 6 - SQUARE_SIZE / 2;
        String provinceName = m.group(3);
        String squareName = "carre" + m.group(4);
        boolean plain = StringUtils.equals("true", m.group(5));
        String terrain = plain ? "plaine" : "foret";
        boolean secondary = false;
        if (provinceName.startsWith("*")) {
            provinceName = provinceName.substring(1);
            secondary = true;
        }
        if (StringUtils.equals("Caption", provinceName)) {
            return;
        }
        provinceName = getRealProvinceName(provinceName, aliases, terrain, log);
        if (provinceName == null) {
            return;
        }
        createSquare(provinceName, squareName, x, y, terrain, zoomParsing, rotw, secondary, provinces, log);
    }

    /**
     * Add a square.
     *
     * @param provinceName name of the province related to this square.
     * @param squareName   name of the square.
     * @param x            coordinate x of the square.
     * @param y            coordinate y of the square.
     * @param terrain      terrain of the square.
     * @param zoomParsing  flag saying that the square is in a zoom.
     * @param rotw         flag saying that the square is in the rotw map.
     * @param secondary    flag saying that the square is secondary.
     * @param provinces    existing provinces.
     * @param log          log writer.
     */
    private static void createSquare(String provinceName, String squareName, int x, int y, String terrain, boolean zoomParsing,
                                     boolean rotw, boolean secondary, Map<String, Province> provinces, Writer log) {
        Province province = provinces.get(provinceName);
        if (province == null) {
            province = new Province(provinceName, log);
            provinces.put(provinceName, province);
        }
        SubProvince portion = new SubProvince(terrain, secondary, rotw);
        Path squarePath = new Path(squareName, true, rotw);
        squarePath.getCoords().add(new ImmutablePair<>(x, y));
        squarePath.getCoords().add(new ImmutablePair<>(x + SQUARE_SIZE, y));
        squarePath.getCoords().add(new ImmutablePair<>(x + SQUARE_SIZE, y + SQUARE_SIZE));
        squarePath.getCoords().add(new ImmutablePair<>(x, y + SQUARE_SIZE));
        squarePath.getCoords().add(new ImmutablePair<>(x, y));
        portion.getPaths().add(new DirectedPath(squarePath, false));


        if ((zoomParsing && !portion.isLight() && !portion.isSecondary())
                || (!province.getPortions().isEmpty() && province.getPortions().get(0).isSecondary() && !portion.isSecondary())
                || (!province.getPortions().isEmpty() && province.getPortions().get(0).isLight() && !portion.isLight())) {
            province.getPortions().add(0, portion);
        } else {
            province.getPortions().add(portion);
        }
    }

    /**
     * Retrieve the real province name given an input.
     * The format is 'T'name~suffix where
     * <ul>
     * <li>T is a character (p for europe, r for rotw and s for sea)</li>
     * <li>name is the name which has to exist in the aliases</li>
     * <li>~suffix is a rotw suffix (inexistant for province and sea)</li>
     * </ul>
     *
     * @param input   base for the province name.
     * @param aliases aliases that contain all the real names and their aliases.
     * @param terrain terrain of the province/sea zone.
     * @param log     log writer.
     * @return the real province name given an input.
     * @throws IOException exception.
     */
    private static String getRealProvinceName(String input,
                                              Map<String, Map<String, List<String>>> aliases,
                                              String terrain, Writer log) throws IOException {
        if (StringUtils.equals("noman", terrain)) {
            return null;
        }

        boolean seaZone = StringUtils.equals("lmer", terrain)
                || StringUtils.equals("mer", terrain)
                || StringUtils.equals("europemer", terrain);

        boolean rotw = !seaZone && input.contains("~");
        String realName = null;
        String provinceName = input;
        String aliasPrefix = "province";
        String prefix = "e";
        String suffix = "";
        if (seaZone) {
            aliasPrefix = "seazone";
            prefix = "s";
        } else if (rotw) {
            aliasPrefix = "granderegion";
            prefix = "r";
            suffix = input.substring(input.indexOf('~'));
            provinceName = input.substring(0, input.indexOf('~'));
        }

        if (aliases.get(aliasPrefix).containsKey(provinceName)) {
            realName = provinceName;
        } else if (aliases.get(aliasPrefix + "Inv").containsKey(provinceName)) {
            realName = aliases.get(aliasPrefix + "Inv").get(provinceName).get(0);
        } else {
            for (String key : aliases.get(aliasPrefix).keySet()) {
                for (String value : aliases.get(aliasPrefix).get(key)) {
                    if (!StringUtils.isEmpty(matchProvinceName(value, provinceName, StringUtils::equals))) {
                        if (realName != null) {
                            log.append(provinceName).append("\tCan't find root name: ambiguous values\t")
                                    .append(realName).append("\t").append(key).append("\n");
                        }
                        realName = key;
                    }
                }
            }
        }

        if (realName == null) {
            if (seaZone) {
                log.append(provinceName).append("\tSea zone not found in aliases\n");
            } else if (rotw) {
                log.append(provinceName).append("\tRegion not found in aliases\n");
            } else {
                log.append(provinceName).append("\tProvince not found in aliases\n");
            }
            return null;
        }

        return prefix + realName + suffix;
    }

    /**
     * Extract data about the provinces.
     *
     * @param provinces data gathered so far.
     * @param aliases   the aliases.
     * @param log       log writer.
     * @throws Exception exception.
     */
    public static void extractProvinceData(Map<String, Province> provinces, Map<String, Map<String, List<String>>> aliases, Writer log) throws Exception {
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(DataExtractor.class.getClassLoader().getResourceAsStream("input/europe.utf")));
        List<String> block = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("NOM ")) {
                if (!block.isEmpty()) {
                    log.append("europe.utf\tNOM already parsed for this block\t").append(line).append("\n");
                }
                block.add(line);
            } else if (line.startsWith("; ---") || line.startsWith("; %%%")) {
                if (!block.isEmpty()) {
                    processBlock(block, provinces, aliases, log);
                }
                block.clear();
            } else if (!block.isEmpty()) {
                block.add(line);
            }
        }
        if (!block.isEmpty()) {
            processBlock(block, provinces, aliases, log);
        }
    }

    /**
     * Process a block of the extractProvinceData segment.
     *
     * @param block     to parse.
     * @param provinces data gathered so far.
     * @param aliases   the aliases.
     * @param log       log writer.
     * @throws Exception exception.
     */
    private static void processBlock(List<String> block, Map<String, Province> provinces, Map<String, Map<String, List<String>>> aliases, Writer log) throws Exception {
        ProvinceInfo info = new ProvinceInfo();
        for (String line : block) {
            Matcher m = Pattern.compile("NOM [^\"]* \"(.*)\" .*").matcher(line);
            if (m.matches()) {
                if (!StringUtils.isEmpty(info.getNameCity())) {
                    info.getAltNameCity().add(purifyName(m.group(1)));
                } else {
                    info.setNameCity(purifyName(m.group(1)));
                }
                continue;
            }
            m = Pattern.compile("ALTNOM [^\"]* \"(.*)\" .*").matcher(line);
            if (m.matches()) {
                info.getAltNameCity().add(purifyName(m.group(1)));
                continue;
            }
            m = Pattern.compile("(PROV|PROVCURVE|PROVCURVEX) [^\"]* \"(.*)\" .*").matcher(line);
            if (m.matches()) {
                if (!StringUtils.isEmpty(info.getNameProvince())) {
                    info.getAltNameProvince().add(purifyName(m.group(2)));
                } else {
                    info.setNameProvince(purifyName(m.group(2)));
                }
                continue;
            }
            m = Pattern.compile("ALTPROV [^\"]* \"(.*)\" .*").matcher(line);
            if (m.matches()) {
                info.getAltNameProvince().add(purifyName(m.group(1)));
                continue;
            }
            m = Pattern.compile("CITE \\d{4} \\d{4} (.*)").matcher(line);
            if (m.matches()) {
                String string = m.group(1);
                string = string.replace("jaune", "");
                info.setCapital(string.contains("capitale"));
                info.setFortress(Integer.parseInt(string.substring(string.length() - 1)));
                continue;
            }
            m = Pattern.compile("IMG \\d{4} \\d{4} (.*)").matcher(line);
            if (m.matches()) {
                String string = m.group(1);
                info.setPort(StringUtils.equals("anchor", string) || StringUtils.equals("anchor4", string));
                info.setArsenal(StringUtils.equals("anchor2", string) || StringUtils.equals("anchor5", string));
                info.setPraesidiable(StringUtils.equals("anchor4", string) || StringUtils.equals("anchor5", string));
                continue;
            }
            if (line.startsWith("VALUE ")) {
                String[] segments = line.split(" ");
                info.setIncome(Integer.parseInt(segments[segments.length - 2]));
                continue;
            }
            if (line.startsWith("ECU ")) {
                String owner = toMajorName(line.substring(line.lastIndexOf(' ')).trim());
                if (!StringUtils.isEmpty(owner)) {
                    if (!StringUtils.isEmpty(info.getDefaultOwner())) {
                        log.append(info.getNameProvince()).append("\tProvince has various owners\t")
                                .append(info.getDefaultOwner()).append("\t")
                                .append(owner).append("\n");
                    }
                    info.setDefaultOwner(owner);
                }
                continue;
            }
        }
        Province found = isProvince(info.getNameProvince(), provinces);
        if (found == null) {
            for (String alt : info.getAltNameProvince()) {
                found = isProvince(alt, provinces);
                if (found != null) {
                    break;
                }
            }

            if (found == null) {
                if (info.getAltNameProvince().size() == 1) {
                    found = isProvince(info.getNameProvince() + info.getAltNameProvince().get(0), provinces);
                    if (found != null) {
                        info.setNameProvince(info.getNameProvince() + info.getAltNameProvince().get(0));
                        info.getAltNameProvince().clear();
                    } else {
                        found = isProvince(info.getNameProvince() + " " + info.getAltNameProvince().get(0), provinces);
                        if (found != null) {
                            info.setNameProvince(info.getNameProvince() + " " + info.getAltNameProvince().get(0));
                            info.getAltNameProvince().clear();
                        }
                    }
                }
            }

            if (found == null) {
                String realProvinceName = deepSearchProvince(info, aliases);
                found = isProvince(realProvinceName, provinces);
            }
        }

        if (found != null) {
            if (found.getInfo() != null) {
                log.append(found.getName()).append("\tProvince has already info\t").append(info.getNameProvince())
                        .append("\n");
            }
            found.setInfo(info);
        } else {
            log.append(info.getNameProvince()).append("\tCan't find province\n");
        }
    }

    /**
     * Remove special caracters from input name.
     *
     * @param input to process.
     * @return name purified.
     */
    private static String purifyName(String input) {
        String name = input.trim();

        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        if ((name.startsWith("(") && name.endsWith(")"))
                || (name.startsWith("[") && name.endsWith("]"))) {
            name = name.substring(1, name.length() - 1);
        }

        return name;
    }

    /**
     * Transform a blason to a Major trigramme.
     *
     * @param blason to check.
     * @return the major trigramme.
     */
    private static String toMajorName(String blason) {
        String major = null;

        switch (blason) {
//            case "brandebourg":
            case "lithuanie":
                major = "pologne";
                break;
            case "france":
            case "espagne":
            case "portugal":
            case "angleterre":
//            case "hollande":
//            case "suede":
//            case "habsbourg":
            case "venise":
            case "pologne":
            case "russie":
            case "turquie":
                major = blason;
                break;
            default:
                break;
        }

        return major;
    }

    /**
     * Retrieves the name of the province related to the info.
     *
     * @param info      of the province.
     * @param aliases   List of aliases.
     * @return the province related to the info.
     */
    private static String deepSearchProvince(ProvinceInfo info, Map<String, Map<String, List<String>>> aliases) {
        String province = null;

        for (String provinceName : aliases.get("province").keySet()) {
            List<String> alts = aliases.get("province").get(provinceName);
            alts.addAll(aliases.get("provinceInv").keySet().stream().filter(alias -> aliases.get("provinceInv").get(alias).contains(provinceName)).collect(Collectors.toList()));

            for (String alt : alts) {
                String name = matchProvinceName(alt, info.getNameProvince(), StringUtils::equals);
                if (StringUtils.isEmpty(name)) {
                    for (String altInfo : info.getAltNameProvince()) {
                        name = matchProvinceName(alt, altInfo, StringUtils::equals);
                        if (!StringUtils.isEmpty(name)) {
                            break;
                        }
                    }
                }

                if (StringUtils.isEmpty(name)) {
                    if (info.getAltNameProvince().size() == 1) {
                        String nameToTest = info.getNameProvince() + info.getAltNameProvince().get(0);
                        name = matchProvinceName(alt, nameToTest, StringUtils::equals);
                        if (StringUtils.isEmpty(name)) {
                            nameToTest = info.getNameProvince() + " " + info.getAltNameProvince().get(0);
                            name = matchProvinceName(alt, nameToTest, StringUtils::equals);
                        }
                    }
                }

                if (!StringUtils.isEmpty(name)) {
                    province = provinceName;
                    break;
                }
            }

            if (!StringUtils.isEmpty(province)) {
                break;
            }
        }

        return province;
    }

    /**
     * Check if the name is a province and returns it.
     * <p>
     * If province is 'PROV', name like 'PROV', 'toto (PROV)' or 'tutu [PROV]' will work.
     * </p>
     *
     * @param name      to check.
     * @param provinces all the provinces.
     * @return the province found.
     */
    private static Province isProvince(String name, Map<String, Province> provinces) {
        String realName = matchProvinceName(name, null, (s, s2) -> provinces.containsKey('e' + s));

        return provinces.get('e' + realName);
    }

    /**
     * Check if the names matches.
     * The first <code>true</code> test will return the match:
     * <ul>
     * <li>filter.test(name, otherName) and will return name</li>
     * <li>filter.test(subName, otherName) and will return subName where name = toto (subName)</li>
     * <li>filter.test(subName, subOtherName) and will return subName where name = toto (subName) and otherName = tutu (subOtherName)</li>
     * <li>filter.test(subName, otherName) and will return subName where name = toto [subName]</li>
     * <li>filter.test(subName, subOtherName) and will return subName where name = toto [subName] and otherName = tutu [subOtherName]</li>
     * </ul>
     *
     * @param name      first string to test.
     * @param otherName second string to test.
     * @param filter    function used to test the strings.
     * @return a matching name.
     */
    private static String matchProvinceName(String name, String otherName, BiPredicate<String, String> filter) {
        String realName = null;

        if (filter.test(name, otherName)) {
            realName = name;
        } else if (name != null && name.contains("(") && name.contains(")")) {
            String newName = name.substring(name.indexOf('(') + 1, name.lastIndexOf(')'));
            if (filter.test(newName, otherName)) {
                realName = newName;
            } else if (otherName != null && otherName.contains("(") && otherName.contains(")")) {
                String newOtherName = otherName.substring(otherName.indexOf('(') + 1, otherName.lastIndexOf(')'));
                if (filter.test(newName, newOtherName)) {
                    realName = newName;
                }
            }
        } else if (name != null && name.contains("[") && name.contains("]")) {
            String newName = name.substring(name.indexOf('[') + 1, name.lastIndexOf(']'));
            if (filter.test(newName, otherName)) {
                realName = newName;
            } else if (otherName != null && otherName.contains("[") && otherName.contains("]")) {
                String newOtherName = otherName.substring(otherName.indexOf('[') + 1, otherName.lastIndexOf(']'));
                if (filter.test(newName, newOtherName)) {
                    realName = newName;
                }
            }
        }

        return realName;
    }

    /**
     * Create the special boxes on the maps.
     *
     * @param log log writer.
     * @return the special boxes.
     */
    public static Map<String, Province> createSpecialBoxes(Writer log) {
        Map<String, Province> specialBoxes = new HashMap<>();
        // Military rounds tiles
        int roundXBegin = 1550;
        int roundYBegin = 32;
        for (int i = 0; i <= 5; i++) {
            createSquare("MR_W" + i, "MR_W" + i, roundXBegin + 2 * i * SQUARE_SIZE, roundYBegin,
                    "lac", false, false, false, specialBoxes, log);
            String name = "MR_S" + (i + 1);
            if (i == 5) {
                name = "MR_End";
            }
            createSquare(name, name, roundXBegin + 2 * i * SQUARE_SIZE, roundYBegin + 2 * SQUARE_SIZE,
                    "lac", false, false, false, specialBoxes, log);
        }

        return specialBoxes;
    }

    /**
     * Extract the aliases.
     *
     * @param log log writer.
     * @return the aliases.
     * @throws Exception exception.
     */
    public static Map<String, Map<String, List<String>>> extractAliases(Writer log) throws Exception {
        Map<String, Map<String, List<String>>> aliases = new HashMap<>();

        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(DataExtractor.class.getClassLoader().getResourceAsStream("input/translations.utf")));
        while ((line = reader.readLine()) != null) {
            if (line.trim().startsWith("%") || StringUtils.isEmpty(line)) {
                continue;
            }

            Matcher m = Pattern.compile("(.*)=\\[(.*)\\]=>(.*)").matcher(line);
            String type;
            String key;
            List<String> alias = new ArrayList<>(1);
            if (m.matches()) {
                type = m.group(2) + "Inv";
                key = m.group(1);
                String alt = m.group(3);
                if (alt.contains("%")) {
                    alt = alt.substring(0, alt.indexOf('%'));
                }
                alias.add(alt);
            } else {
                String[] split = line.split(":");
                type = split[0];
                key = split[1];
                for (int i = 2; i < split.length; i++) {
                    String alt = split[i];
                    if (alt.contains("%")) {
                        alt = alt.substring(0, alt.indexOf('%'));
                    }
                    alias.add(alt);
                }
            }

            if (!aliases.containsKey(type)) {
                aliases.put(type, new HashMap<>());
            }

            if (aliases.get(type).containsKey(key)) {
                log.append(key).append("\tAlias already exist\t").append(type).append("\n");
            }

            aliases.get(type).put(key, alias);
        }

        return aliases;
    }

    /**
     * Create the countries from the header file of countries.
     *
     * @param log log writer.
     * @throws Exception exception.
     */
    public static Map<String, Country> createCountries(Writer log) throws Exception {
        Map<String, Country> countries = new HashMap<>();
        Country major = new Country("MAJOR");
        major.setName("france");
        major.setLongLabel("Kingdom of France");
        major.setShortLabel("France");
        major.setArmyClass("IV");
        major.setCulture("Latin");
        major.setReligion("catholique");
        countries.put(major.getName(), major);
        major = new Country("MAJOR");
        major.setName("angleterre");
        major.setLongLabel("Kingdom of England");
        major.setShortLabel("England");
        major.setArmyClass("IVM");
        major.setCulture("Latin");
        major.setReligion("catholique");
        countries.put(major.getName(), major);
        major = new Country("MAJOR");
        major.setName("espagne");
        major.setLongLabel("Kingdom of Spain");
        major.setShortLabel("Spain");
        major.setArmyClass("III");
        major.setCulture("Latin");
        major.setReligion("catholique");
        countries.put(major.getName(), major);
        major = new Country("MAJOR");
        major.setName("russie");
        major.setLongLabel("Kingdom of Russia");
        major.setShortLabel("Russia");
        major.setArmyClass("IM");
        major.setCulture("Orthodoxe");
        major.setReligion("orthodoxe");
        countries.put(major.getName(), major);
        major = new Country("MAJOR");
        major.setName("turquie");
        major.setLongLabel("Ottoman Empire");
        major.setShortLabel("Turkey");
        major.setArmyClass("I");
        major.setCulture("Islam");
        major.setReligion("sunnite");
        countries.put(major.getName(), major);
        major = new Country("MAJOR");
        major.setName("prusse");
        major.setLongLabel("Kingdom of Prussia");
        major.setShortLabel("Prussia");
        major.setArmyClass("III");
        major.setCulture("Latin");
        major.setReligion("protestant");
        countries.put(major.getName(), major);

        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(DataExtractor.class.getClassLoader().getResourceAsStream("input/engEntetesMineurs.tex")));
        List<String> block = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            if (line.trim().startsWith("%") || StringUtils.isEmpty(line)) {
                continue;
            }

            block.add(line);

            if (wellBalanced(block, '{', '}')) {
                Country country = null;
                String minor = String.join("", block);
                if (minor.startsWith("\\minorcountry")) {
                    country = new Country("MINOR");
                }
                if (minor.startsWith("\\minorcountryminmaj")) {
                    country = new Country("MINORMAJOR");
                }
                if (minor.startsWith("\\minorcountryvirtual")) {
                    country = new Country("REVOLT");
                }
                if (minor.startsWith("\\minorcountryreallyvirtual")) {
                    country = new Country("VIRTUAL");
                }
                if (minor.startsWith("\\minorcountryhab")) {
                    country = new Country("HAB");
                }
                if (minor.startsWith("\\minorcountryrotw")) {
                    country = new Country("ROTW");
                }

                if (country != null) {
                    String[] split = ToolsUtil.split(minor, '{', '}');
                    if (split.length == 3 || split.length == 4) {
                        country.setName(transformSpecialChars(split[0]));
                        country.setShortLabel(transformSpecialChars(split[2]));
                        country.setLongLabel(transformSpecialChars(split[1]));
                    } else {
                        log.append("Minor country header\tUnparsable\t").append(minor).append("\n");
                    }

                    countries.put(country.getName(), country);
                }

                block.clear();
            }
        }

        return countries;
    }

    /**
     * Tell if a block of String is well balanced (same number oof open and close char).
     *
     * @param lines block of String.
     * @param open  char.
     * @param close char.
     * @return <code>true</code> if the block is well balanced.
     */
    private static boolean wellBalanced(List<String> lines, char open, char close) {
        int balance = 0;

        for (String line : lines) {
            for (char c : line.toCharArray()) {
                if (c == open) {
                    balance++;
                }
                if (c == close) {
                    balance--;
                }
            }
        }

        return balance == 0;
    }

    /**
     * Transform special chars into ascii chars.
     *
     * @param string to transform.
     * @return encoded string.
     */
    private static String transformSpecialChars(String string) {
        String name = string;

        name = name.replace("\\AE{}", "Æ");
        name = name.replace("\\ae{}", "æ");
        name = name.replace("\\ae", "æ");
        name = name.replace("\\\"o", "ö");
        name = name.replace("\\\"u", "ü");
        name = name.replace("\\`", "è");
        name = name.replace("\\bazar", "");
        name = name.replace("\\HRE", "HRE");

        return name;
    }

    /**
     * Fill the countries from the file of countries.
     *
     * @param countries to fill.
     * @param provinces gathered so far.
     * @param aliases   the aliases.
     * @param log       log writer.
     * @throws Exception exception.
     */
    public static void extractCountriesData(Map<String, Country> countries, Map<String, Province> provinces,
                                            Map<String, Map<String, List<String>>> aliases, Writer log) throws Exception {
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(DataExtractor.class.getClassLoader().getResourceAsStream("input/engCorpsMineurs.tex")));
        List<String> block = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            if (line.trim().startsWith("%") || StringUtils.isEmpty(line)) {
                continue;
            }

            block.add(line.trim());

            if (wellBalanced(block, '{', '}')) {
                String minor = String.join(" ", block);
                block.clear();
                String[] split = ToolsUtil.split(minor, '{', '}');
                Country country = countries.get(split[0]);
                if (country == null) {
                    log.append(split[0]).append("\tMinor country not found, cant fill it\n");
                    continue;
                }
                if (minor.startsWith("\\minorreligion")) {
                    country.setReligion(split[1]);
                } else if (minor.startsWith("\\minordiplo")) {
                    if (!minor.startsWith("\\minordiplospecial")) {
                        country.setRoyalMarriage(toDiplo(split[1]));
                        country.setSubsidies(toDiplo(split[2]));
                        country.setMilitaryAlliance(toDiplo(split[3]));
                        country.setExpCorps(toDiplo(split[4]));
                        country.setEntryInWar(toDiplo(split[5]));
                        country.setVassal(toDiplo(split[6]));
                        country.setAnnexion(toDiplo(split[7]));
                    }
                } else if (minor.startsWith("\\minorfid")) {
                    country.setFidelity(Integer.parseInt(split[1]));
                } else if (minor.startsWith("\\minorprovince")) {
                    String realProv = getRealProvince(country, split[1], provinces, aliases, log);
                    if (realProv != null) {
                        country.getProvinces().add(realProv);
                    }
                } else if (minor.startsWith("\\minorcapital")) {
                    String realProv = getRealProvince(country, split[1], provinces, aliases, log);
                    if (realProv != null) {
                        country.getCapitals().add(realProv);
                    }
                } else if (minor.startsWith("\\minorpref")) {
                    // TODO conception
                } else if (minor.startsWith("\\minorbasicforces")) {
                    List<Country.Limit> forces = createForces(country, split[1], log);
                    country.getBasicForces().addAll(forces);
                } else if (minor.startsWith("\\minorbasicrenforts")) {
                    List<Country.Limit> forces = createForces(country, split[1], log);
                    country.getReinforcements().addAll(forces);
                } else if (minor.startsWith("\\minorforces")) {
                    String[] forces = ToolsUtil.split(split[1], ',', '(', ')');
                    for (String force : forces) {
                        int number = 0;
                        String type = null;
                        String[] details = force.trim().split(" ");
                        if (details.length == 1) {
                            number = 1;
                            if (details[0].charAt(0) != '\\') {
                                log.append(country.getName()).append("\tCountry has error in minorforces\t")
                                        .append(minor);
                            }
                            type = details[0].substring(1);
                        } else if (details.length == 2) {
                            number = Integer.parseInt(details[0]);
                            type = details[1].substring(1);
                        } else if (force.contains("transport \\FLEET")) {
                            number = 1;
                            type = "FLEET_TRANSPORT";
                        } else {
                            continue;
                        }
                        country.addLimit(number, type);
                    }
                } else if (minor.startsWith("\\minorarmyclass")) {
                    country.setCulture(split[1]);
                    country.setArmyClass(split[2]);
                } else if (minor.startsWith("\\minorHRE[Elector]")) {
                    country.setHre(true);
                    country.setElector(true);
                } else if (minor.startsWith("\\minorHRE")) {
                    country.setHre(true);
                } else if (minor.startsWith("\\minorgeo")) {
                    String geo = split[1];
                    if (geo.length() <= 7) {
                        country.setPreference(geo.substring(1, 4));
                        country.setPreferenceBonus(Integer.parseInt(geo.substring(geo.length() - 1, geo.length())));
                    }
                } else if (minor.startsWith("\\minorspecial")
                        || minor.startsWith("\\minorrule")
                        || minor.startsWith("\\minorblason")
                        || minor.startsWith("\\eventref")
                        || minor.startsWith("\\minoractivation")
                        || minor.startsWith("\\minorevent")
                        || minor.startsWith("\\minorindeptwo")
                        || minor.startsWith("\\minorindep")
                        || minor.startsWith("\\minorfixedincome")
                        || minor.startsWith("\\minorbonusrenforts")) {
                } else {

                    throw new RuntimeException(minor);
                }
            }
        }

        // Default value for potential independent kingdoms
        for (Country country : countries.values()) {
            if (country.getFidelity() == 0 && StringUtils.equals("REVOLT", country.getType())
                    && country.getName().startsWith("V")) {
                country.setFidelity(10);

                country.setRoyalMarriage(4);
                country.setSubsidies(50);
                country.setMilitaryAlliance(2);
                country.setExpCorps(4);
                country.setEntryInWar(5);
                country.setVassal(8);

                country.addLimit(1, "ARMY");
                country.addLimit(2, "LD");

                country.addBasicForce(1, "\\ARMY\\facemoins");

                country.addReinforcements(1, "\\LD");
            }
        }

        Country major = countries.get("france");
        major.addLimit(6, "ARMY");
        major.addLimit(5, "FLEET");
        major.addLimit(4, "PIRATE");
        major.addLimit(15, "LDND");
        major.addLimit(5, "LD");
        major.addLimit(4, "NTD");
        major.addLimit(8, "LDENDE");
        major.addLimit(5, "FORT12");
        major.addLimit(5, "FORT23");
        major.addLimit(6, "FORT34");
        major.addLimit(4, "FORT45");
        major.addLimit(11, "FORT");
        major.addLimit(2, "ARS23");
        major.addLimit(2, "ARS34");
        major.addLimit(4, "MISSION");
        major.addLimit(5, "SEPOY");
        major.addLimit(3, "SEPOY_EXPLORATION");
        major.addLimit(2, "INDIAN");
        major.addLimit(4, "INDIAN_EXPLORATION");
        major.addLimit(14, "COL");
        major.addLimit(10, "TP");
        major.addLimit(18, "TF");
        major.addLimit(4, "ROTW_DIPLO");
        major.addLimit(3, "MNU_ART");
        major.addLimit(1, "MNU_WOOD");
        major.addLimit(1, "MNU_CEREALS");
        major.addLimit(2, "MNU_INSTRUMENTS");
        major.addLimit(2, "MNU_METAL");
        major.addLimit(1, "MNU_FISH");
        major.addLimit(1, "MNU_SALT");
        major.addLimit(1, "MNU_CLOTHES");
        major.addLimit(2, "MNU_WINE");

        major = countries.get("angleterre");
        major.addLimit(4, "ARMY");
        major.addLimit(6, "FLEET");
        major.addLimit(3, "PIRATE");
        major.addLimit(15, "LDND");
        major.addLimit(5, "LD");
        major.addLimit(4, "NTD");
        major.addLimit(10, "LDENDE");
        major.addLimit(2, "FORT12");
        major.addLimit(4, "FORT23");
        major.addLimit(4, "FORT34");
        major.addLimit(2, "FORT45");
        major.addLimit(11, "FORT");
        major.addLimit(2, "ARS23");
        major.addLimit(2, "ARS34");
        major.addLimit(1, "ARS23_GIBRALTAR");
        major.addLimit(2, "MISSION");
        major.addLimit(5, "SEPOY");
        major.addLimit(3, "SEPOY_EXPLORATION");
        major.addLimit(14, "COL");
        major.addLimit(10, "TP");
        major.addLimit(18, "TF");
        major.addLimit(4, "ROTW_DIPLO");
        major.addLimit(1, "MNU_ART");
        major.addLimit(1, "MNU_WOOD");
        major.addLimit(1, "MNU_CEREALS");
        major.addLimit(3, "MNU_INSTRUMENTS");
        major.addLimit(3, "MNU_METAL");
        major.addLimit(2, "MNU_FISH");
        major.addLimit(2, "MNU_CLOTHES");

        major = countries.get("espagne");
        major.addLimit(5, "ARMY");
        major.addLimit(4, "FLEET");
        major.addLimit(2, "PIRATE");
        major.addLimit(10, "LDND");
        major.addLimit(10, "LD");
        major.addLimit(4, "NTD");
        major.addLimit(10, "LDENDE");
        major.addLimit(6, "FORT12");
        major.addLimit(4, "FORT23");
        major.addLimit(4, "FORT34");
        major.addLimit(3, "FORT45");
        major.addLimit(10, "FORT");
        major.addLimit(2, "ARS23");
        major.addLimit(2, "ARS34");
        major.addLimit(15, "MISSION");
        major.addLimit(32, "COL");
        major.addLimit(7, "TP");
        major.addLimit(13, "TF");
        major.addLimit(2, "ROTW_DIPLO");
        major.addLimit(2, "MNU_ART");
        major.addLimit(1, "MNU_INSTRUMENTS");
        major.addLimit(2, "MNU_METAL");
        major.addLimit(1, "MNU_FISH");
        major.addLimit(1, "MNU_SALT");
        major.addLimit(1, "MNU_CLOTHES");
        major.addLimit(1, "MNU_WINE");

        major = countries.get("russie");
        major.addLimit(6, "ARMY");
        major.addLimit(3, "FLEET");
        major.addLimit(1, "PIRATE");
        major.addLimit(10, "LDND");
        major.addLimit(10, "LD");
        major.addLimit(2, "NTD");
        major.addLimit(8, "LDENDE");
        major.addLimit(4, "FORT12");
        major.addLimit(4, "FORT23");
        major.addLimit(3, "FORT34");
        major.addLimit(1, "FORT45");
        // TODO St Petersburg
        major.addLimit(10, "FORT");
        major.addLimit(1, "ARS23");
        major.addLimit(11, "COL");
        major.addLimit(5, "TP");
        major.addLimit(7, "TF");
        major.addLimit(2, "ROTW_DIPLO");
        major.addLimit(1, "MNU_ART");
        major.addLimit(1, "MNU_WOOD");
        major.addLimit(1, "MNU_CEREALS");
        major.addLimit(1, "MNU_INSTRUMENTS");
        major.addLimit(2, "MNU_METAL");
        major.addLimit(2, "MNU_CLOTHES");

        major = countries.get("turquie");
        major.addLimit(6, "ARMY");
        major.addLimit(4, "ARMY_TIMAR");
        major.addLimit(6, "FLEET");
        major.addLimit(2, "PIRATE");
        major.addLimit(5, "LDND");
        major.addLimit(5, "LDND_TIMAR");
        major.addLimit(5, "LD");
        major.addLimit(5, "LD_TIMAR");
//        major.addLimit(5, "PASHAS"); TODO conception pashas
        major.addLimit(4, "NTD");
        major.addLimit(6, "LDENDE");
        major.addLimit(5, "FORT12");
        major.addLimit(10, "FORT23");
        major.addLimit(2, "FORT34");
        major.addLimit(1, "FORT45");
        major.addLimit(5, "FORT");
        major.addLimit(5, "COL");
        major.addLimit(6, "TP");
        major.addLimit(9, "TF");
        major.addLimit(2, "ROTW_DIPLO");
        major.addLimit(1, "MNU_ART");
        major.addLimit(1, "MNU_WOOD");
        major.addLimit(1, "MNU_CEREALS");
        major.addLimit(1, "MNU_INSTRUMENTS");
        major.addLimit(2, "MNU_METAL");
        major.addLimit(1, "MNU_SALT");
        major.addLimit(2, "MNU_CLOTHES");

        major = countries.get("hollande");
        major.addLimit(2, "PIRATE");
        major.addLimit(1, "LD");
        major.addLimit(2, "FORT12");
        major.addLimit(4, "FORT23");
        major.addLimit(5, "FORT34");
        major.addLimit(2, "FORT45");
        major.addLimit(5, "FORT");
        major.addLimit(2, "ARS23");
        major.addLimit(2, "ARS34");
        major.addLimit(4, "SEPOY");
        major.addLimit(2, "SEPOY_EXPLORATION");
        major.addLimit(10, "COL");
        major.addLimit(12, "TP");
        major.addLimit(20, "TF");
        major.addLimit(4, "ROTW_DIPLO");
        major.addLimit(2, "MNU_ART");
        major.addLimit(2, "MNU_INSTRUMENTS");
        major.addLimit(1, "MNU_METAL");
        major.addLimit(1, "MNU_FISH");
        major.addLimit(2, "MNU_CLOTHES");

        major = countries.get("pologne");
        major.addLimit(2, "FORT12");
        major.addLimit(4, "FORT23");
        major.addLimit(4, "FORT34");
        major.addLimit(1, "FORT45");
        major.addLimit(2, "FORT");
        major.addLimit(5, "COL");
        major.addLimit(5, "TP");
        major.addLimit(6, "TF");
        major.addLimit(2, "ROTW_DIPLO");
        major.addLimit(1, "MNU_ART");
        major.addLimit(2, "MNU_WOOD");
        major.addLimit(2, "MNU_CEREALS");
        major.addLimit(1, "MNU_INSTRUMENTS");
        major.addLimit(1, "MNU_METAL");
        major.addLimit(1, "MNU_SALT");

        major = countries.get("venise");
        major.addLimit(1, "PIRATE");
        major.addLimit(2, "FORT12");
        major.addLimit(5, "FORT23");
        major.addLimit(3, "FORT34");
        major.addLimit(1, "FORT45");
        major.addLimit(2, "FORT");
        major.addLimit(1, "COL");
        major.addLimit(4, "TP");
        major.addLimit(5, "TF");
        major.addLimit(2, "ROTW_DIPLO");
        major.addLimit(2, "MNU_ART");
        major.addLimit(1, "MNU_INSTRUMENTS");
        major.addLimit(1, "MNU_METAL");
        major.addLimit(1, "MNU_SALT");
        major.addLimit(1, "MNU_WINE");

        major = countries.get("portugal");
        major.addLimit(1, "PIRATE");
        major.addLimit(3, "FORT12");
        major.addLimit(5, "FORT23");
        major.addLimit(2, "FORT34");
        major.addLimit(4, "FORT");
        major.addLimit(2, "ARS23");
        major.addLimit(2, "ARS34");
        major.addLimit(3, "MISSION");
        major.addLimit(12, "COL");
        major.addLimit(12, "TP");
        major.addLimit(8, "TF");
        major.addLimit(6, "ROTW_DIPLO");
        major.addLimit(1, "MNU_ART");
        major.addLimit(1, "MNU_INSTRUMENTS");
        major.addLimit(1, "MNU_METAL");
        major.addLimit(1, "MNU_FISH");
        major.addLimit(1, "MNU_SALT");
        major.addLimit(1, "MNU_WINE");

        major = countries.get("suede");
        major.addLimit(1, "FLEET");
        major.addLimit(1, "PIRATE");
        major.addLimit(2, "FORT12");
        major.addLimit(4, "FORT23");
        major.addLimit(4, "FORT34");
        major.addLimit(1, "FORT45");
        major.addLimit(4, "FORT");
        major.addLimit(2, "ARS23");
        // TODO army pugatchev ?
        major.addLimit(5, "COL");
        major.addLimit(5, "TP");
        major.addLimit(10, "TF");
        major.addLimit(2, "ROTW_DIPLO");
        major.addLimit(1, "MNU_ART");
        major.addLimit(2, "MNU_WOOD");
        major.addLimit(1, "MNU_INSTRUMENTS");
        major.addLimit(2, "MNU_METAL");
        major.addLimit(1, "MNU_FISH");
        major.addLimit(1, "MNU_CLOTHES");

        major = countries.get("prusse");
        major.addLimit(4, "ARMY");
        major.addLimit(2, "LDND");
        major.addLimit(8, "LD");
        major.addLimit(2, "NTD");
        major.addLimit(3, "LDENDE");
        major.addLimit(2, "FORT12");
        major.addLimit(4, "FORT23");
        major.addLimit(4, "FORT34");
        major.addLimit(1, "FORT45");
        major.addLimit(2, "FORT");
        major.addLimit(2, "COL");
        major.addLimit(2, "TP");
        major.addLimit(2, "TF");
        major.addLimit(1, "MNU_ART");
        major.addLimit(1, "MNU_WOOD");
        major.addLimit(1, "MNU_INSTRUMENTS");
        major.addLimit(1, "MNU_METAL");
        major.addLimit(1, "MNU_METAL_SCHLESIEN");
        major.addLimit(1, "MNU_CLOTHES");

        major = countries.get("habsbourg");
        major.addLimit(1, "FLEET");
        major.addLimit(2, "NTD");
        major.addLimit(3, "LDENDE");
        major.addLimit(2, "FORT12");
        major.addLimit(4, "FORT23");
        major.addLimit(4, "FORT34");
        major.addLimit(3, "FORT45");
        major.addLimit(2, "TF");
        major.addLimit(1, "MNU_ART");
        major.addLimit(1, "MNU_CEREALS");
        major.addLimit(1, "MNU_INSTRUMENTS");
        major.addLimit(2, "MNU_METAL");
        major.addLimit(1, "MNU_SALT");
        major.addLimit(1, "MNU_CLOTHES");
    }

    /**
     * Transform a string to a diplo track value.
     *
     * @param value to transform.
     * @return the diplo track value.
     */
    private static Integer toDiplo(String value) {
        if (StringUtils.equals("*", value)) {
            return null;
        }

        String modValue = value;
        if (modValue.endsWith("+")) {
            modValue = modValue.substring(0, modValue.length() - 1);
        }

        return Integer.parseInt(modValue);
    }

    /**
     * Retrieves the real province.
     *
     * @param country   owner of the province.
     * @param prov      name of the province.
     * @param provinces list of the provinces.
     * @param aliases   list of the aliases.
     * @param log       log writer.
     * @return the real province.
     * @throws Exception exception.
     */
    private static String getRealProvince(Country country, String prov, Map<String, Province> provinces,
                                          Map<String, Map<String, List<String>>> aliases, Writer log) throws Exception {
        String realProv = null;
        Province province = isProvince(prov, provinces);

        if (province == null) {
            List<String> inv = aliases.get("provinceInv").get(prov);
            if (inv != null && inv.size() == 1) {
                prov = inv.get(0);
                province = isProvince(prov, provinces);
            }
            if (province == null) {
                for (String alias : aliases.get("province").get(prov)) {
                    province = isProvince(alias, provinces);
                    if (province != null) {
                        break;
                    }
                }
            }
        }
        if (province == null) {
            log.append(country.getName()).append("\tProvince does not exist\t").append(prov).append("\n");
        } else {
            realProv = province.getName();
            if (StringUtils.equals("MINOR", country.getType())
                    || (StringUtils.equals("MINORMAJOR", country.getType()) && !StringUtils.equals("hollande", country.getName()))) {
                if (!StringUtils.isEmpty(province.getInfo().getDefaultOwner())) {
                    if (StringUtils.equals("ukraine", province.getInfo().getDefaultOwner())) {
                        province.getInfo().setDefaultOwner(country.getName());
                    }
                    log.append(prov).append("\tProvince has 2 owners\t")
                            .append(province.getInfo().getDefaultOwner()).append("\t").append(country.getName())
                            .append("\tgiven to\t").append(province.getInfo().getDefaultOwner()).append("\n");
                } else {
                    province.getInfo().setDefaultOwner(country.getName());
                }
            }
        }

        return realProv;
    }

    /**
     * Create forces (basic force as reinforcements).
     *
     * @param country       to give forces.
     * @param stringToParse string to parse.
     * @param log           log writer.
     * @return the forces.
     * @throws Exception exception.
     */
    private static List<Country.Limit> createForces(Country country, String stringToParse, Writer log) throws Exception {
        List<Country.Limit> returnValue = new ArrayList<>();
        String[] forces = ToolsUtil.split(stringToParse, ',', '{', '}');
        for (String force : forces) {
            int number;
            String type;
            String[] details = force.trim().split(" ");
            if (details.length == 1) {
                if (StringUtils.equals("nothing", details[0])
                        || StringUtils.equals("None", details[0])
                        || details[0].startsWith("See")) {
                    continue;
                }
                number = 1;
                if (details[0].charAt(0) != '\\') {
                    log.append(country.getName()).append("\tCountry has error in addForce\t")
                            .append(stringToParse).append("\n");
                }
                type = details[0];
            } else if (details.length == 2) {
                number = Integer.parseInt(details[0]);
                type = details[1];
            } else {
                if (StringUtils.equals("\\LD or \\ND", force.trim())) {
                    number = 1;
                    type = "\\LDND";
                } else if (StringUtils.equals("1 \\NGD\\ or 1 \\NDE or 1\\NTD", force.trim())) {
                    number = 1;
                    type = "\\NDE";
                } else if (StringUtils.equals("1 \\NDE\\ or 1 \\NGD", force.trim())) {
                    number = 1;
                    type = "\\NDE";
                } else if (StringUtils.equals("\\LD or 2\\NGD", force.trim())) {
                    number = 1;
                    type = "\\LDND";
                } else if (StringUtils.equals("1 \\NWD or 2 \\NGD", force.trim())) {
                    number = 1;
                    type = "\\ND";
                } else {
                    if (!force.contains("\\EUminorremark")
                            && !force.contains(" if ")
                            && !force.contains("event")) {
                        log.append(country.getName()).append("\tCan't parse country force\t")
                                .append(force).append("\n");
                    }
                    continue;
                }
            }
            returnValue.add(country.createForce(number, type));
        }
        return returnValue;
    }
}
