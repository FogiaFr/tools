package com.mkl.tools.eu.map;

import com.google.common.collect.Lists;
import com.mkl.tools.eu.vo.Leader;
import com.mkl.tools.eu.vo.country.Country;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.*;

/**
 * Sort the counter in order to have a proper hierarchy.
 *
 * @author MKL.
 */
public class CounterGenerator {
    /** Sometimes, some countries use the counters of other countries. */
    private static final Map<String, String> aliasCountry;
    /** List of countries without own counter. */
    private static final List<String> countriesWithoutOwn;
    /** List of countries without control counter. */
    private static final List<String> countriesWithoutControl;
    /** List of countries that can use Galley fleets. */
    private static final List<String> countriesWithGalleyFleet;
    /** List of minor countries with special diplo submission. */
    private static final Map<String, String> countriesSubmissive;

    static {
        aliasCountry = new HashMap<>();
        aliasCountry.put("parliament", "angleterre");
        aliasCountry.put("brandebourg", "prusse");
        aliasCountry.put("ormus", "perse");
        aliasCountry.put("parliament", "angleterre");
        aliasCountry.put("german-empire", "saint-empire");
        aliasCountry.put("provincesne", "hollande");
        aliasCountry.put("teutoniques1", "teutoniques");
        aliasCountry.put("teutoniques2", "teutoniques");
//        aliasCountry.put("turcorsaire", "turquie");
//        aliasCountry.put("turvizir", "turquie");
//        aliasCountry.put("fralicense", "france");


        countriesWithoutOwn = new ArrayList<>();
        countriesWithoutOwn.add("saint-empire");
        countriesWithoutOwn.add("pirates");
        countriesWithoutOwn.add("mazovie");
        countriesWithoutOwn.add("rebelles");
        countriesWithoutOwn.add("natives");

        countriesWithoutControl = new ArrayList<>();
        countriesWithoutControl.add("saint-empire");
        countriesWithoutControl.add("pirates");
        countriesWithoutControl.add("mazovie");
        countriesWithoutControl.add("rebelles");

        countriesWithGalleyFleet = new ArrayList<>();
        countriesWithGalleyFleet.add("algerie");
        countriesWithGalleyFleet.add("danemark");
        countriesWithGalleyFleet.add("espagne");
        countriesWithGalleyFleet.add("france");
        countriesWithGalleyFleet.add("genes");
        countriesWithGalleyFleet.add("mamelouks");
        countriesWithGalleyFleet.add("naples");
        countriesWithGalleyFleet.add("pologne");
        countriesWithGalleyFleet.add("russie");
        countriesWithGalleyFleet.add("suede");
        countriesWithGalleyFleet.add("turquie");
        countriesWithGalleyFleet.add("venise");

        countriesSubmissive = new HashMap<>();
        countriesSubmissive.put("mazovie", "pologne");
    }

    /**
     * Move from the existing counters (in flat directory) to a more hierarchic way.
     *
     * @param countries list of countries.
     * @param leaders   list of leaders.
     * @param log       log writer.
     * @throws IOException exception.
     */
    public static void moveExistingCounter(Map<String, Country> countries, List<Leader> leaders, String inputDirectory, String outputDirectory, Writer log) throws IOException {
        deleteTree(FileSystems.getDefault().getPath(outputDirectory));

        Map<String, List<Pair<String, String>>> typesByCountry = new HashMap<>();
        for (Country country : countries.values()) {
            List<Pair<String, String>> types = listCounterTypes(country, log);
            typesByCountry.put(country.getName(), types);
        }

        for (Leader leader : leaders) {
            String country = leader.getCountry();
            if (aliasCountry.containsKey(country)) {
                country = aliasCountry.get(country);
            }
            if (!typesByCountry.containsKey(country)) {
                typesByCountry.put(country, new ArrayList<>());
            }
            String code = leader.getCode();
            code = code.replaceAll(" ", "-");
            switch (leader.getType()) {
                case LEADER:
                    typesByCountry.get(country).add(new ImmutablePair<>("{0}" + File.separator + "Leader_{0}_" + code + ".png", "Leader_{0}_" + code + ".png"));
                    break;
                case LEADERDOUBLE:
                    typesByCountry.get(country).add(new ImmutablePair<>("{0}" + File.separator + "Leader_{0}_" + code + ".png", "LeaderDouble_{0}_" + code + "_recto.png"));
                    typesByCountry.get(country).add(new ImmutablePair<>("{0}" + File.separator + "Leader_{0}_" + code + "-2.png", "LeaderDouble_{0}_" + code + "_verso.png"));
                    break;
                case LEADERPAIRE:
                    String country2 = leader.getCountry2();
                    if (aliasCountry.containsKey(country2)) {
                        country2 = aliasCountry.get(country2);
                    }
                    if (!typesByCountry.containsKey(country2)) {
                        typesByCountry.put(country2, new ArrayList<>());
                    }
                    String code2 = leader.getCode2();
                    String sourceCode;
                    if (code2 == null) {
                        sourceCode = code;
                    } else {
                        code2 = code2.replaceAll(" ", "-");
                        sourceCode = code + "_" + code2;
                    }
                    typesByCountry.get(country).add(new ImmutablePair<>("{0}" + File.separator + "Leader_{0}_" + code + ".png", "LeaderPair_{0}_" + country2 + "_" + sourceCode + "_recto.png"));
                    typesByCountry.get(country2).add(new ImmutablePair<>("{0}" + File.separator + "Leader_{0}_" + code + "-2.png", "LeaderPair_" + country + "_{0}_" + sourceCode + "_verso.png"));
                    break;
                case PACHA:
                    typesByCountry.get(country).add(new ImmutablePair<>("{0}" + File.separator + "Pacha_{0}_" + code + ".png", "Pacha_" + code + "_recto.png"));
                    typesByCountry.get(country).add(new ImmutablePair<>("{0}" + File.separator + "Pacha_{0}_" + code + "-2.png", "Pacha_" + code + "_verso.png"));
                    break;
            }
        }
        typesByCountry.put(null, listNeutralCounters(log));

        collectCounter(typesByCountry, inputDirectory, outputDirectory, log);
    }

    /**
     * Collect the counters from a directory to another.
     *
     * @param typesByCountry  list of counters to copy.
     * @param inputDirectory  directory where the counters are.
     * @param outputDirectory directory where the counters will be.
     * @param log             log writer.
     * @throws IOException exception.
     */
    private static void collectCounter(Map<String, List<Pair<String, String>>> typesByCountry, String inputDirectory, String outputDirectory, Writer log) throws IOException {
        Path root = FileSystems.getDefault().getPath(inputDirectory);
        DirectoryStream<Path> children = Files.newDirectoryStream(root);
        List<Path> subDirs = Lists.newArrayList(children);
        for (String key : typesByCountry.keySet()) {
            List<Pair<String, String>> types = typesByCountry.get(key);
            String countryName = key;
            if (countryName != null && (countryName.startsWith("H") || countryName.startsWith("V"))) {
                countryName = countryName.substring(1);
            }
            if (aliasCountry.containsKey(countryName)) {
                countryName = aliasCountry.get(countryName);
            }
            if (StringUtils.equals(countryName, "teutoniques")) {
                countryName = countryName + "?";
            }
            for (Pair<String, String> type : types) {
                List<String> dirInError = new ArrayList<>();
                String searchQuery = type.getRight();
                if (countryName != null) {
                    searchQuery = MessageFormat.format(type.getRight(), countryName);
                }
                searchQuery = searchQuery.replaceAll("\\(", "{");
                searchQuery = searchQuery.replaceAll("\\)", "}");
                if (type.getRight().startsWith("Diplomacy_") && StringUtils.equals("brandebourg", key)) {
                    searchQuery = MessageFormat.format(type.getRight(), key);
                }
                for (Path inputPath : subDirs) {
                    String subDir = inputPath.getName(inputPath.getNameCount() - 1).toString();
                    DirectoryStream<Path> results = Files.newDirectoryStream(inputPath, searchQuery);
                    Iterator<Path> result = results.iterator();
                    if (result.hasNext()) {
                        Path inputFile = result.next();
                        String outputSuffix = type.getLeft();
                        if (key != null) {
                            outputSuffix = MessageFormat.format(type.getLeft(), key);
                        }
                        String outputFilename = outputDirectory + subDir + File.separator + outputSuffix;
                        Path outputFile = FileSystems.getDefault().getPath(outputFilename);
                        createTree(outputFile.getParent());
                        if (Files.exists(outputFile)) {
                            Files.copy(inputFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            Files.copy(inputFile, outputFile);
                        }
                    } else {
                        dirInError.add(subDir);
                    }
                }
                if (!dirInError.isEmpty()) {
                    if (key == null) {
                        log.append("Neutral");
                    } else {
                        log.append(key);
                    }
                    log.append("\tCountry counter not found.\t").append(searchQuery).append("\t").append(dirInError.toString()).append("\n");
                }
            }
        }
    }

    /**
     * List the neutral counters.
     *
     * @param log log writer.
     * @return List of counters of the country.
     * @throws IOException exception.
     */
    private static List<Pair<String, String>> listNeutralCounters(Writer log) throws IOException {
        List<Pair<String, String>> types = new ArrayList<>();

        // convoys
        types.add(new ImmutablePair<>("FLOTA_DE_ORO.png", "Convoy_Oro.png"));
        types.add(new ImmutablePair<>("FLOTA_DEL_PERU.png", "Convoy_Peru.png"));
        types.add(new ImmutablePair<>("EAST_INDIES.png", "Convoy_EastIndies.png"));
        types.add(new ImmutablePair<>("LEVANT.png", "Convoy_Levant.png"));

        // Generic counter
        types.add(new ImmutablePair<>("REVOLT_PLUS.png", "Generic_Revolt_recto.png"));
        types.add(new ImmutablePair<>("REVOLT_MINUS.png", "Generic_Revolt_verso.png"));
        types.add(new ImmutablePair<>("REBEL_PLUS.png", "Generic_Rebellion_recto.png"));
        types.add(new ImmutablePair<>("REBEL_MINUS.png", "Generic_Rebellion_verso.png"));
        types.add(new ImmutablePair<>("GOLD_MINE.png", "Generic_GoldMine_recto.png"));
        types.add(new ImmutablePair<>("GOLD_DEPLETED.png", "Generic_GoldMine_verso.png"));
        types.add(new ImmutablePair<>("PILLAGE_PLUS.png", "Generic_Pillage_recto.png"));
        types.add(new ImmutablePair<>("PILLAGE_MINUS.png", "Generic_Pillage_verso.png"));
        types.add(new ImmutablePair<>("SIEGEWORK_PLUS.png", "Generic_Siegeworks_recto.png"));
        types.add(new ImmutablePair<>("SIEGEWORK_MINUS.png", "Generic_siegeworks_verso.png"));
        types.add(new ImmutablePair<>("FLOOD_PLUS.png", "Generic_DutchFlood_recto.png"));
        types.add(new ImmutablePair<>("FLOOD_MINUS.png", "Generic_DutchFlood_verso.png"));
        types.add(new ImmutablePair<>("TURN.png", "Generic_Turn.png"));
        types.add(new ImmutablePair<>("GOOD_WEATHER.png", "Generic_Round_recto.png"));
        types.add(new ImmutablePair<>("BAD_WEATHER.png", "Generic_Round_verso.png"));
        types.add(new ImmutablePair<>("INFLATION.png", "Generic_Inflation_recto.png"));
        types.add(new ImmutablePair<>("INFLATION_GOLD.png", "Generic_Inflation_verso.png"));

        // Trade centers
        types.add(new ImmutablePair<>("TRADE_CENTER_GREAT_ORIENT.png", "TradeCentre_GreatOrient.png"));
        types.add(new ImmutablePair<>("TRADE_CENTER_MEDITERRANEAN.png", "TradeCentre_Mediterranean.png"));
        types.add(new ImmutablePair<>("TRADE_CENTER_ATLANTIC.png", "TradeCentre_Atlantic.png"));
        types.add(new ImmutablePair<>("TRADE_CENTER_INDIAN.png", "TradeCentre_Indian.png"));

        // Exotic resources
        types.add(new ImmutablePair<>("SP_PRICE.png", "Price_Spices.png"));
        types.add(new ImmutablePair<>("SP_PRODUCTION.png", "Production_Spices.png"));
        types.add(new ImmutablePair<>("SU_PRICE.png", "Price_Sugar.png"));
        types.add(new ImmutablePair<>("SU_PRODUCTION.png", "Production_Sugar.png"));
        types.add(new ImmutablePair<>("FISH_PRICE.png", "Price_Fish.png"));
        types.add(new ImmutablePair<>("FISH_PRODUCTION.png", "Production_Fish.png"));
        types.add(new ImmutablePair<>("PA_PRICE.png", "Price_PA.png"));
        types.add(new ImmutablePair<>("PA_PRODUCTION.png", "Production_PA.png"));
        types.add(new ImmutablePair<>("SILK_PRICE.png", "Price_Silk.png"));
        types.add(new ImmutablePair<>("SILK_PRODUCTION.png", "Production_Silk.png"));
        types.add(new ImmutablePair<>("SALT_PRICE.png", "Price_Salt.png"));
        types.add(new ImmutablePair<>("SALT_PRODUCTION.png", "Production_Salt.png"));
        types.add(new ImmutablePair<>("CO_PRICE.png", "Price_Cotton.png"));
        types.add(new ImmutablePair<>("CO_PRODUCTION.png", "Production_Cotton.png"));
        types.add(new ImmutablePair<>("FUR_PRICE.png", "Price_Furs.png"));
        types.add(new ImmutablePair<>("FUR_PRODUCTION.png", "Production_Furs.png"));
        types.add(new ImmutablePair<>("PO_PRICE.png", "Price_PO.png"));
        types.add(new ImmutablePair<>("PO_PRODUCTION.png", "Production_PO.png"));
        types.add(new ImmutablePair<>("SL_PRICE.png", "Price_Slaves.png"));
        types.add(new ImmutablePair<>("SL_PRODUCTION.png", "Production_Slaves.png"));

        // Technology
        types.add(new ImmutablePair<>("TECH_RENAISSANCE.png", "TechnologyGoal_Renaissance.png"));
        types.add(new ImmutablePair<>("TECH_TERCIO.png", "TechnologyGoal_Tercio.png"));
        types.add(new ImmutablePair<>("TECH_ARQUEBUS.png", "TechnologyGoal_Arquebus.png"));
        types.add(new ImmutablePair<>("TECH_MUSKET.png", "TechnologyGoal_Musket.png"));
        types.add(new ImmutablePair<>("TECH_BAROQUE.png", "TechnologyGoal_Baroque.png"));
        types.add(new ImmutablePair<>("TECH_MANOEUVRE.png", "TechnologyGoal_Manoeuvre.png"));
        types.add(new ImmutablePair<>("TECH_LACE_WAR.png", "TechnologyGoal_LaceWar.png"));
        types.add(new ImmutablePair<>("TECH_NAE_GALEON.png", "TechnologyGoal_NaoGaleon.png"));
        types.add(new ImmutablePair<>("TECH_GALLEON_FLUYT.png", "TechnologyGoal_GalleonFluyt.png"));
        types.add(new ImmutablePair<>("TECH_GALLEASS.png", "TechnologyGoal_Galleass.png"));
        types.add(new ImmutablePair<>("TECH_BATTERY.png", "TechnologyGoal_Battery.png"));
        types.add(new ImmutablePair<>("TECH_VESSEL.png", "TechnologyGoal_Vessel.png"));
        types.add(new ImmutablePair<>("TECH_THREE_DECKER.png", "TechnologyGoal_ThreeDecker.png"));
        types.add(new ImmutablePair<>("TECH_SEVENTY_FOUR.png", "TechnologyGoal_SeventyFour.png"));
        // TODO culture techs

        // Fortress
        types.add(new ImmutablePair<>("FORTRESS_1.png", "Fortress_blanc_1.png"));
        types.add(new ImmutablePair<>("FORTRESS_2.png", "Fortress_neutre_3-2_verso.png"));
        types.add(new ImmutablePair<>("FORTRESS_3.png", "Fortress_neutre_3-2_recto.png"));
        types.add(new ImmutablePair<>("FORTRESS_4.png", "Fortress_neutre_5-4_verso.png"));
        types.add(new ImmutablePair<>("FORTRESS_5.png", "Fortress_neutre_5-4_recto.png"));

        return types;
    }

    /**
     * List the counters of a country using its limit forces.
     *
     * @param country whose counters we seek.
     * @param log     log writer.
     * @return List of counters of the country.
     * @throws IOException exception.
     */
    private static List<Pair<String, String>> listCounterTypes(Country country, Writer log) throws IOException {
        List<Pair<String, String>> types = new ArrayList<>();
        if (!countriesWithoutOwn.contains(country.getName())) {
            types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_OWN.png", "Ownership_{0}.png"));
        }
        if (!countriesWithoutControl.contains(country.getName())) {
            types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_CONTROL.png", "Control_{0}.png"));
        }
        if (StringUtils.equals("MINORMAJOR", country.getType())
                || StringUtils.equals("MAJOR", country.getType())) {
            types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_STABILITY.png", "Stability_{0}.png"));
            types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_STABILITY.png", "Stability_{0}.png"));
            types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_TECH_LAND.png", "Tech_Land_{0}.png"));
            if (!StringUtils.equals("prusse", country.getName()) && !StringUtils.equals("habsbourg", country.getName())) {
                types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_TECH_NAVAL.png", "Tech_Naval_{0}.png"));
            }
        }
        if (country.getFidelity() > 0 && !StringUtils.equals("ROTW", country.getType())) {
            if (countriesSubmissive.containsKey(country.getName())) {
                String owner = countriesSubmissive.get(country.getName());
                types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_DIPLOMACY.png", "Submission_*{0}_" + owner + "_recto.png"));
                types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_DIPLOMACY_WAR.png", "Submission_*{0}_" + owner + "_recto.png"));
            } else {
                types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_DIPLOMACY.png", "Diplomacy_*{0}_F*_recto.png"));
                types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_DIPLOMACY_WAR.png", "Diplomacy_*{0}_F*_verso.png"));
            }
        }
        for (Country.Limit limit : country.getLimits()) {
            switch (limit.getType()) {
                case "ARMY":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARMY_PLUS.png", "Army_{0}_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARMY_MINUS.png", "Army_{0}_*_verso.png"));
                    break;
                case "ARMY_TIMAR":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARMY_TIMAR_PLUS.png", "Army_timar_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARMY_TIMAR_MINUS.png", "Army_timar_*_verso.png"));
                    break;
                case "FLEET":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_FLEET_PLUS.png", "Fleet_Warships_{0}_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_FLEET_MINUS.png", "Fleet_Warships_{0}_*_verso.png"));
                    if (countriesWithGalleyFleet.contains(country.getName())) {
                        types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_FLEET_GALLEY_PLUS.png", "Fleet_Galleys_{0}_*_recto.png"));
                        types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_FLEET_GALLEY_MINUS.png", "Fleet_Galleys_{0}_*_verso.png"));
                    }
                    break;
                case "FLEET_TRANSPORT":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_FLEET_TRANSPORT_PLUS.png", "Fleet_Warships_{0}_Transport_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_FLEET_TRANSPORT_MINUS.png", "Fleet_Warships_{0}_Transport_verso.png"));
                    break;
                case "LDND":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_LAND_DETACHMENT.png", "LDND_{0}_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_NAVAL_DETACHMENT.png", "LDND_{0}_*_verso.png"));
                    break;
                case "LDND_TIMAR":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_LAND_DETACHMENT_TIMAR.png", "LDND_timar_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_NAVAL_DETACHMENT.png", "LDND_timar_*_verso.png"));
                    break;
                case "LD":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_LAND_DETACHMENT.png", "LD_{0}_*.png"));
                    break;
                case "LD_TIMAR":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_LAND_DETACHMENT_TIMAR.png", "LD_timar_*.png"));
                    break;
                case "LD_KOZAK":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_LAND_DETACHMENT_KOZAK.png", "LD_{0}_*-Kozaki.png"));
                    break;
                case "LDENDE":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_LAND_DETACHMENT_EXPLORATION.png", "LDENDE_{0}_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_NAVAL_DETACHMENT_EXPLORATION.png", "LDENDE_{0}_*_verso.png"));
                    break;
                case "LDE":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_LAND_DETACHMENT_EXPLORATION.png", "LDE_{0}_*.png"));
                    break;
                case "LDE_KOZAK":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_LAND_DETACHMENT_EXPLORATION_KOZAK.png", "LDE_{0}_*-Kozaki.png"));
                    break;
                case "NDE":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_NAVAL_DETACHMENT_EXPLORATION.png", "NDE_{0}_*.png"));
                    break;
                case "NTD":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_NAVAL_TRANSPORT.png", "ND_{0}_*-transport.png"));
                    break;
                case "PIRATE":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_PIRATE_PLUS.png", "Privateer_{0}_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_PIRATE_MINUS.png", "Privateer_{0}_verso.png"));
                    break;
                case "TP":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_TRADING_POST_PLUS.png", "TradingPost_{0}_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_TRADING_POST_MINUS.png", "TradingPost_{0}_*_verso.png"));
                    break;
                case "COL":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_COLONY_PLUS.png", "Colony_{0}_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_COLONY_MINUS.png", "Colony_{0}_*_verso.png"));
                    break;
                case "TF":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_TRADING_FLEET_PLUS.png", "TradeFleet_{0}_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_TRADING_FLEET_MINUS.png", "TradeFleet_{0}_*_verso.png"));
                    break;
                case "FORT12":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_FORTRESS_2.png", "Fortress_{0}_2-1_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_FORTRESS_1.png", "Fortress_{0}_2-1_verso.png"));
                    break;
                case "FORT23":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_FORTRESS_3.png", "Fortress_{0}_3-2_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_FORTRESS_2.png", "Fortress_{0}_3-2_verso.png"));
                    break;
                case "FORT34":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_FORTRESS_4.png", "Fortress_{0}_4-3_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_FORTRESS_3.png", "Fortress_{0}_4-3_verso.png"));
                    break;
                case "FORT45":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_FORTRESS_5.png", "Fortress_{0}_5-4_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_FORTRESS_4.png", "Fortress_{0}_5-4_verso.png"));
                    break;
                case "FORT":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_FORT.png", "Fort_{0}_0_recto.png"));
                    break;
                case "ARS23":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARSENAL_3.png", "Arsenal_{0}_3-2_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARSENAL_2.png", "Arsenal_{0}_3-2_verso.png"));
                    break;
                case "ARS23_GIBRALTAR":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARSENAL_3_GIBRALTAR.png", "Arsenal_{0}_3-2-Gibraltar_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARSENAL_2_GIBRALTAR.png", "Arsenal_{0}_3-2-Gibraltar_verso.png"));
                    break;
                case "ARS23_SEBASTOPOL":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARSENAL_3_SEBASTOPOL.png", "Arsenal_{0}_3-2-Sebastopol_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARSENAL_2_SEBASTOPOL.png", "Arsenal_{0}_3-2-Sebastopol_verso.png"));
                    break;
                case "ARS01_ST_PETER":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARSENAL_1_ST_PETER.png", "Arsenal_{0}_1-0-Saint-Petersburg_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARSENAL_0_ST_PETER.png", "Arsenal_{0}_1-0-Saint-Petersburg_verso.png"));
                    break;
                case "ARS23_ST_PETER":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARSENAL_3_ST_PETER.png", "Arsenal_{0}_3-2-Saint-Petersburg_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARSENAL_2_ST_PETER.png", "Arsenal_{0}_3-2-Saint-Petersburg_verso.png"));
                    break;
                case "ARS45_ST_PETER":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARSENAL_5_ST_PETER.png", "Arsenal_{0}_5-4-Saint-Petersburg_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARSENAL_4_ST_PETER.png", "Arsenal_{0}_5-4-Saint-Petersburg_verso.png"));
                    break;
                case "ARS34":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARSENAL_4.png", "Arsenal_{0}_4-3_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ARSENAL_3.png", "Arsenal_{0}_4-3_verso.png"));
                    break;
                case "MISSION":
                    // TODO TG-5 missionary can be level 1, 2 or 3. Maybe collusion with leaders. Wait leaders conception.
                    break;
                case "SEPOY":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_LAND_SEPOY.png", "LD_{0}_*-(Sepoys,Cipayes,Indiers).png"));
                    break;
                case "SEPOY_EXPLORATION":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_LAND_SEPOY_EXPLORATION.png", "LDE_{0}_*-(Sepoys,Cipayes,Indiers).png"));
                    break;
                case "INDIAN":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_LAND_INDIAN.png", "LD_{0}_*-GrandsLacs.png"));
                    break;
                case "INDIAN_EXPLORATION":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_LAND_INDIAN_EXPLORATION.png", "LDE_{0}_*-CoureursDesBois.png"));
                    break;
                case "ROTW_DIPLO":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ROTW_RELATION.png", "ROTWTreaty_{0}_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_ROTW_ALLIANCE.png", "ROTWTreaty_{0}_verso.png"));
                    break;
                case "MNU_ART":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_ART_MINUS.png", "Manufacture_{0}_art_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_ART_PLUS.png", "Manufacture_{0}_art_*_verso.png"));
                    break;
                case "MNU_CEREALS":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_CEREALS_MINUS.png", "Manufacture_{0}_cereales_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_CEREALS_PLUS.png", "Manufacture_{0}_cereales_*_verso.png"));
                    break;
                case "MNU_CLOTHES":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_CLOTHES_MINUS.png", "Manufacture_{0}_tissus_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_CLOTHES_PLUS.png", "Manufacture_{0}_tissus_*_verso.png"));
                    break;
                case "MNU_FISH":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_FISH_MINUS.png", "Manufacture_{0}_peche_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_FISH_PLUS.png", "Manufacture_{0}_peche_*_verso.png"));
                    break;
                case "MNU_INSTRUMENTS":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_INSTRUMENTS_MINUS.png", "Manufacture_{0}_instruments_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_INSTRUMENTS_PLUS.png", "Manufacture_{0}_instruments_*_verso.png"));
                    break;
                case "MNU_METAL":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_METAL_MINUS.png", "Manufacture_{0}_metal_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_METAL_PLUS.png", "Manufacture_{0}_metal_*_verso.png"));
                    break;
                case "MNU_METAL_SCHLESIEN":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_METAL_SCHLESIEN_MINUS.png", "Manufacture_{0}_metalsilesie_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_METAL_SCHLESIEN_PLUS.png", "Manufacture_{0}_metalsilesie_*_verso.png"));
                    break;
                case "MNU_SALT":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_SALT_MINUS.png", "Manufacture_{0}_sel*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_SALT_PLUS.png", "Manufacture_{0}_sel*_verso.png"));
                    break;
                case "MNU_WINE":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_WINE_MINUS.png", "Manufacture_{0}_vin_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_WINE_PLUS.png", "Manufacture_{0}_vin_*_verso.png"));
                    break;
                case "MNU_WOOD":
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_WOOD_MINUS.png", "Manufacture_{0}_bois_*_recto.png"));
                    types.add(new ImmutablePair<>("{0}" + File.separator + "{0}_MNU_WOOD_PLUS.png", "Manufacture_{0}_bois_*_verso.png"));
                    break;
                default:
                    log.append(country.getName()).append("\tCounter unknown.\t").append(limit.getType()).append("\n");
                    break;
            }
        }


        return types;
    }

    /**
     * Create all the directories needed for this path.
     *
     * @param path to create.
     * @throws IOException exception.
     */
    private static void createTree(Path path) throws IOException {
        if (Files.notExists(path.getParent())) {
            createTree(path.getParent());
        }
        if (Files.notExists(path)) {
            Files.createDirectory(path);
        }
    }

    /**
     * Delete all the arborescence.
     *
     * @param path to delete.
     * @throws IOException exception.
     */
    private static void deleteTree(Path path) throws IOException {
        if (Files.notExists(path)) {
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            /** {@inheritDoc} */
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            /** {@inheritDoc} */
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

        });
    }
}
