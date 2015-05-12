package com.mkl.tools.eu;


import com.mkl.tools.eu.util.ToolsUtil;
import com.mkl.tools.eu.vo.country.Country;
import com.mkl.tools.eu.vo.map.ClientGenerator;
import com.mkl.tools.eu.vo.map.DBGenerator;
import com.mkl.tools.eu.vo.map.DataExtractor;
import com.mkl.tools.eu.vo.province.Border;
import com.mkl.tools.eu.vo.province.Path;
import com.mkl.tools.eu.vo.province.Province;

import java.io.Writer;
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
        Map<String, List<Path>> specialBorders = new HashMap<>();
        Map<String, Province> provinces = new HashMap<>();

        Map<String, Map<String, List<String>>> aliases = DataExtractor.extractAliases(log);

        DataExtractor.extractPaths(provinces, specialBorders, aliases, "input/europe.grid.ps", false, log);
        DataExtractor.extractPaths(provinces, specialBorders, aliases, "input/rotw.grid.ps", true, log);
        Map<String, Province> specialBoxes = DataExtractor.createSpecialBoxes(log);

        DataExtractor.extractProvinceData(provinces, aliases, log);

        Map<String, Country> countries = DataExtractor.createCountries(log);

        DataExtractor.extractCountriesData(countries, provinces, aliases, log);

        for (Province province : provinces.values()) {
            if (province.getInfo() != null && province.getInfo().getDefaultOwner() == null) {
                log.append(province.getName()).append("\tProvince has no owner\n");
            }
        }

        ClientGenerator.createMapData(provinces, specialBoxes, log);

        List<Border> borders = ClientGenerator.createProvincesData(provinces, specialBorders, countries, log);

        Writer sqlWriter = ToolsUtil.createFileWriter("src/main/resources/output/provinces_countries.sql", false);

        DBGenerator.createDBInjection(provinces, borders, countries, sqlWriter, log);

        DBGenerator.createCountriesData(countries, sqlWriter, log);

        sqlWriter.flush();
        sqlWriter.close();

        log.flush();
        log.close();
    }
}
