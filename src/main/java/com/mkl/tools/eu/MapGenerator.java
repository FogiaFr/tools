package com.mkl.tools.eu;

import com.mkl.tools.eu.map.ClientGenerator;
import com.mkl.tools.eu.map.DBGenerator;
import com.mkl.tools.eu.map.DataExtractor;
import com.mkl.tools.eu.util.ToolsUtil;
import com.mkl.tools.eu.vo.country.Country;
import com.mkl.tools.eu.vo.province.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility to generate various files for the EU Map whose the map in the format .geo.json.
 *
 * @author MKL
 */
public final class MapGenerator {

    /** No constructor for utility class. */
    private MapGenerator() {

    }

    /**
     * Do all the stuff.
     *
     * @param args no args.
     * @throws Exception exception.
     */
    public static void main(String[] args) throws Exception {
        Writer log = ToolsUtil.createFileWriter("src/main/resources/log.txt", false);
        Writer sqlWriter = ToolsUtil.createFileWriter("src/main/resources/output/delete_insert_referentiel.sql", false);

        try {
            Map<String, List<Path>> specialBorders = new HashMap<>();
            Map<String, Province> provinces = new HashMap<>();
            List<Border> borders = new ArrayList<>();

            Map<String, Map<String, List<String>>> aliases = DataExtractor.extractAliases(log);

            Map<String, Region> regions = DataExtractor.extractRegions(aliases, log);

            DataExtractor.extractPaths(provinces, specialBorders, regions, aliases, "input/europe.grid.ps", false, log);
            DataExtractor.extractPaths(provinces, specialBorders, regions, aliases, "input/rotw.grid.ps", true, log);
            // World is round
            borders.add(new Border(provinces.get("sPacifique NE"), provinces.get("sPacifique"), null));
            borders.add(new Border(provinces.get("sPacifique SE"), provinces.get("sPacifique"), null));
            // Bering strait
            borders.add(new Border(provinces.get("rKamchatka~I"), provinces.get("rAmour~N"), "BERING_STRAIT"));
            borders.add(new Border(provinces.get("rKamchatka~I"), provinces.get("rBaikal~NE"), "BERING_STRAIT"));
            borders.add(new Border(provinces.get("rKamchatka~I"), provinces.get("rYakoutie~S"), "BERING_STRAIT"));
            borders.add(new Border(provinces.get("rKamchatka~I"), provinces.get("rYakoutie~NE"), "BERING_STRAIT"));

            DataExtractor.createSpecialBoxes(provinces, log);

            // Provinces need to be restructured for trade zone/rotw process
            for (Province province : provinces.values()) {
                province.restructure();
            }

            DataExtractor.extractProvinceData(provinces, aliases, "input/europe.utf", false, log);
            DataExtractor.extractProvinceData(provinces, aliases, "input/rotw.utf", true, log);

            Map<String, Country> countries = DataExtractor.createCountries(log);

            DataExtractor.extractCountriesData(countries, provinces, aliases, log);

            DataExtractor.extractSeaData(provinces, countries, borders, aliases, "input/source.eps", false, log);
            DataExtractor.extractSeaData(provinces, countries, borders, aliases, "input/sourceRotw.eps", true, log);

            List<Mine> mines = new ArrayList<>();
            mines.addAll(DataExtractor.extractMinesSaltData(provinces, countries, borders, aliases, "input/portsandmines.eps", false, log));
            mines.addAll(DataExtractor.extractMinesSaltData(provinces, countries, borders, aliases, "input/portsandminesRotw.eps", true, log));

            for (Province province : provinces.values()) {
                if (province.getInfo() != null && province.getInfo().getDefaultOwner() == null) {
                    log.append(province.getName()).append("\tProvince has no owner\n");
                }
                if (StringUtils.equals("SEA", province.getTerrain()) && province.getSeaInfo() == null) {
                    log.append(province.getName()).append("\tSea zone has no info\n");
                } else if (!StringUtils.equals("SEA", province.getTerrain()) && province.getSeaInfo() != null) {
                    log.append(province.getName()).append("\tProvince has sea info\n");
                }
            }

            for (Region region : regions.values()) {
                int nbReal = 0;
                for (String province : provinces.keySet()) {
                    if (province.startsWith("r" + region.getName() + "~")) {
                        nbReal++;
                    }
                }

                if (nbReal != region.getNumber()) {
                    log.append(region.getName()).append("\tRegion has wrong number of provinces.\t")
                            .append(Integer.toString(region.getNumber())).append("\t").append(Integer.toString(nbReal))
                            .append("\n");
                }
            }

//        CounterGenerator.moveExistingCounter(countries,
//                "D:\\dev\\upide\\old-lipn.univ-paris13.fr\\~dubacq\\europa\\pions\\0.6\\",
//                "D:\\dev\\workspace\\eu\\front\\eu-front-client\\data\\counters\\v2\\",
//                log);

            ClientGenerator.createMapData(provinces, log);

            ClientGenerator.createBorderData(borders, provinces, specialBorders, log);

            DBGenerator.createDeleteScript(sqlWriter);

            DBGenerator.createProvincesData(provinces, borders, regions, sqlWriter);

            DBGenerator.createCountriesData(countries, sqlWriter);

            DBGenerator.createMinesData(mines, sqlWriter);
        } finally {
            IOUtils.closeQuietly(sqlWriter);
            IOUtils.closeQuietly(log);
        }
    }
}
