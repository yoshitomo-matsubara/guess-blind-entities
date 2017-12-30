package kddcup2016;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Pair;

import java.io.*;
import java.util.*;

public class CountryExtractor {
    private static final String COUNTRY2AFFIL_FILE_OPTION = "c";
    private static final String AFFILS_FILE_OPTION = "a";
    private static final String EXTRA_AFFILS_FILE_OPTION = "ea";
    private static final int COUNTRY_ID_INDEX = 1;
    private static final int UNIVERSITY_ID_INDEX = 2;
    private static final int MIN_NAME_LENGTH = 4;
    private static final int MIN_SPACE_COUNT = 1;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(COUNTRY2AFFIL_FILE_OPTION, true, true, "[input] country-university-id file", options);
        MiscUtil.setOption(AFFILS_FILE_OPTION, true, true, "[input] Affiliations file", options);
        MiscUtil.setOption(EXTRA_AFFILS_FILE_OPTION, true, true, "[input] extra-PaperAuthorAffiliations file", options);
        MiscUtil.setOption(Config.OUTPUT_FILE_OPTION, true, true, "[output] output file", options);
        return options;
    }

    private static boolean checkIfValidName(String name) {
        return name.split(" ").length >= MIN_SPACE_COUNT && name.length() >= MIN_NAME_LENGTH;
    }

    private static Pair<Map<String, String>, TreeMap<Integer, List<String>>> readCountryFileMap(String filePath) {
        System.out.println("\tStart:\treading country-university-id file");
        Map<String, String> universityCountryMap = new HashMap<>();
        TreeMap<Integer, List<String>> universityNameListMap = new TreeMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(Config.FIRST_DELIMITER);
                if (elements.length < 3 || !checkIfValidName(elements[UNIVERSITY_ID_INDEX])) {
                    continue;
                }

                int length = elements[UNIVERSITY_ID_INDEX].length();
                if (!universityNameListMap.containsKey(length)) {
                    universityNameListMap.put(length, new ArrayList<>());
                }

                String lowerCaseName = elements[UNIVERSITY_ID_INDEX].toLowerCase();
                universityCountryMap.put(lowerCaseName, elements[COUNTRY_ID_INDEX]);
                universityNameListMap.get(length).add(lowerCaseName);
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ readCountryFileMap");
            e.printStackTrace();
        }

        System.out.println("\tEnd:\treading country-university-id file");
        return new Pair<>(universityCountryMap, universityNameListMap);
    }

    private static Map<String, String> buildConvertMap(String affilsFilePath,
                                                           Map<String, String> universityCountryMap,
                                                           TreeMap<Integer, List<String>> universityNameListMap) {
        Map<String, String> convertMap = new HashMap<>();
        try {
            int totalCount = 0;
            int hitCount = 0;
            BufferedReader br = new BufferedReader(new FileReader(new File(affilsFilePath)));
            String line;
            while ((line = br.readLine()) != null) {
                totalCount++;
                String[] elements = line.split(Config.FIRST_DELIMITER);
                if (!checkIfValidName(elements[1])) {
                    continue;
                }

                String searchName = elements[1];
                int length = searchName.length();
                for (int currentLength : universityNameListMap.descendingKeySet()) {
                    if (currentLength > length) {
                        continue;
                    }

                    boolean isHit = false;
                    List<String> nameList = universityNameListMap.get(currentLength);
                    for (String name : nameList) {
                        if (searchName.indexOf(name) == 0 ||
                                (searchName.indexOf(name) > 0 && searchName.indexOf(" " + name) > 0)) {
                            String countryId = universityCountryMap.get(name);
                            convertMap.put(elements[0], countryId);
                            isHit = true;
                            hitCount++;
                        }
                    }

                    if (isHit) {
                        break;
                    }
                }
            }

            br.close();
            System.out.println("total count: " + String.valueOf(totalCount));
            System.out.println("hit count: " + String.valueOf(hitCount));
        } catch (Exception e) {
            System.err.println("Exception @ buildConvertMap");
            e.printStackTrace();
        }
        return convertMap;
    }

    private static void convertToCountry(String extraAffilsFilePath, Map<String, String> convertMap, String outputFilePath) {
        try {
            FileUtil.makeParentDir(outputFilePath);
            File outputFile = new File(outputFilePath);
            BufferedReader br = new BufferedReader(new FileReader(new File(extraAffilsFilePath)));
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
            String line;
            while ((line = br.readLine()) != null) {
                StringBuilder sb = new StringBuilder();
                String[] elements = line.split(Config.FIRST_DELIMITER);
                String[] ids = elements[1].split(Config.SECOND_DELIMITER);
                for (String id : ids) {
                    String[] keyValues = id.split(Config.KEY_VALUE_DELIMITER);
                    if (convertMap.containsKey(keyValues[1])) {
                        String subStr = keyValues[0] + Config.KEY_VALUE_DELIMITER + convertMap.get(keyValues[1]);
                        String str = sb.length() == 0 ? subStr : Config.SECOND_DELIMITER + subStr;
                        sb.append(str);
                    }
                }

                if (sb.length() > 0) {
                    bw.write(elements[0] + Config.FIRST_DELIMITER + sb.toString());
                    bw.newLine();
                }
            }

            bw.close();
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ convertToCountry");
            e.printStackTrace();
        }
    }

    private static void extract(String country2AffilFilePath, String affilsFilePath,
                                String extraAffilsFilePath, String outputFilePath) {
        Pair<Map<String, String>, TreeMap<Integer, List<String>>> pair = readCountryFileMap(country2AffilFilePath);
        Map<String, String> universityCountryMap = pair.first;
        TreeMap<Integer, List<String>> universityNameListMap = pair.second;
        Map<String, String> convertMap = buildConvertMap(affilsFilePath, universityCountryMap, universityNameListMap);
        convertToCountry(extraAffilsFilePath, convertMap, outputFilePath);
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("CountryExtractor", options, args);
        String country2AffilFilePath = cl.getOptionValue(COUNTRY2AFFIL_FILE_OPTION);
        String affilsFilePath = cl.getOptionValue(AFFILS_FILE_OPTION);
        String extraAffilsFilePath = cl.getOptionValue(EXTRA_AFFILS_FILE_OPTION);
        String outputFilePath = cl.getOptionValue(Config.OUTPUT_FILE_OPTION);
        extract(country2AffilFilePath, affilsFilePath, extraAffilsFilePath, outputFilePath);
    }
}
