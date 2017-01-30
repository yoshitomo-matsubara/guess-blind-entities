package kddcup2016;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class LevelUpConverter {
    private static final String FOS_HIERARCHY_FILE_OPTION = "f";
    private static final String MIN_COUNT_THRESHOLD_OPTION = "t";
    private static final int DEFAULT_MIN_COUNT_THRESHOLD = 1;
    private static final int CHILD_FOS_ID_INDEX = 0;
    private static final int CHILD_FOS_LEVEL_INDEX = 1;
    private static final int PARENT_FOS_ID_INDEX = 2;
    private static final int PARENT_FOS_LEVEL_INDEX = 3;
    private static final int VENUE_ID_INDEX = 0;
    private static final int FOS_LIST_INDEX = 1;

    private static Options setOptions() {
        Options options = new Options();
        MiscUtil.setOption(Config.INPUT_FILE_OPTION, true, true, "[input] input file", options);
        MiscUtil.setOption(FOS_HIERARCHY_FILE_OPTION, true, true, "[input] FieldOfStudyHierarchy file", options);
        MiscUtil.setOption(MIN_COUNT_THRESHOLD_OPTION, true, false,
                "[param, optional] minimum count threshold for each bag of fields", options);
        MiscUtil.setOption(Config.OUTPUT_FILE_OPTION, true, true, "[output] output file", options);
        return options;
    }

    private static void addNewFos(HashMap<String, FieldOfStudy> fosHierarchyMap, String fosId, String fosLevel) {
        fosHierarchyMap.put(fosId, new FieldOfStudy(fosId, fosLevel));
    }

    private static boolean checkIfValid(String[] elements) {
        if (elements.length != 5) {
            return false;
        }

        for (String element : elements) {
            if (element == null || element.length() == 0) {
                return false;
            }
        }
        return true;
    }

    private static HashMap<String, FieldOfStudy> buildFosHierarchyMap(List<String> lineList) {
        System.out.println("Start:\tbuilding hierarchy");
        HashMap<String, FieldOfStudy> fosHierarchyMap = new HashMap<>();
        int size = lineList.size();
        for (int i = 0;i < size; i++) {
            String line = lineList.remove(0);
            String[] elements = line.split(Config.FIRST_DELIMITER);
            if (!checkIfValid(elements)) {
                continue;
            }

            if (!fosHierarchyMap.containsKey(elements[CHILD_FOS_ID_INDEX])) {
                addNewFos(fosHierarchyMap, elements[CHILD_FOS_ID_INDEX], elements[CHILD_FOS_LEVEL_INDEX]);
            }

            if (!fosHierarchyMap.containsKey(elements[PARENT_FOS_ID_INDEX])) {
                addNewFos(fosHierarchyMap, elements[PARENT_FOS_ID_INDEX], elements[PARENT_FOS_LEVEL_INDEX]);
            }

            FieldOfStudy childFos = fosHierarchyMap.get(elements[CHILD_FOS_ID_INDEX]);
            childFos.addUpperFos(fosHierarchyMap.get(elements[PARENT_FOS_ID_INDEX]));
        }

        System.out.println("End:\tbuilding hierarchy");
        return fosHierarchyMap;
    }

    private static HashSet<String> convertToTopFosIdSet(FieldOfStudy fos) {
        HashSet<String> topFosIdSet = new HashSet<>();
        if (fos != null && fos.isTopLevel()) {
            topFosIdSet.add(fos.id);
        }

        if (fos == null || fos.getUpperFosList().size() == 0) {
            return topFosIdSet;
        }

        List<FieldOfStudy> upperFosList = fos.getUpperFosList();
        for (FieldOfStudy upperFos : upperFosList) {
            HashSet<String> subFosIdSet = convertToTopFosIdSet(upperFos);
            topFosIdSet.addAll(subFosIdSet);
        }
        return topFosIdSet;
    }

    private static void convert(String inputFilePath, HashMap<String, FieldOfStudy> fosHierarchyMap,
                                int minCountThr, String outputFilePath) {
        System.out.println("Start:\tconverting");
        try {
            File inputFile = new File(inputFilePath);
            FileUtil.makeParentDir(outputFilePath);
            File outputFile = new File(outputFilePath);
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(Config.FIRST_DELIMITER);
                String venueId = elements[VENUE_ID_INDEX];
                String fosListStr = elements[FOS_LIST_INDEX];
                String[] bofStrs = fosListStr.split(Config.SECOND_DELIMITER);
                BagOfFields bof = new BagOfFields(venueId);
                for (String bofStr : bofStrs) {
                    String[] keyValue = bofStr.split(Config.KEY_VALUE_DELIMITER);
                    String fosId = keyValue[0];
                    int count = Integer.parseInt(keyValue[1]);
                    FieldOfStudy fos = fosHierarchyMap.get(fosId);
                    HashSet<String> topFosIdSet = convertToTopFosIdSet(fos);
                    Iterator<String> ite = topFosIdSet.iterator();
                    while (ite.hasNext()) {
                        String fieldId = ite.next();
                        bof.countUp(fieldId, count);
                    }
                }

                if (bof.getTotalCount() >= minCountThr) {
                    bw.write(bof.toString());
                    bw.newLine();
                }
            }

            bw.close();
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ convert");
            e.printStackTrace();
        }
        System.out.println("End:\tconverting");
    }

    private static void convert(String inputFilePath, String foshFilePath, int minCountThr, String outputFilePath) {
        List<String> lineList = FileUtil.readFile(foshFilePath);
        HashMap<String, FieldOfStudy> fosHierarchyMap = buildFosHierarchyMap(lineList);
        convert(inputFilePath, fosHierarchyMap, minCountThr, outputFilePath);
    }

    public static void main(String[] args) {
        Options options = setOptions();
        CommandLine cl = MiscUtil.setParams("LevelUpConverter for KDD Cup 2016 dataset", options, args);
        String inputFilePath = cl.getOptionValue(Config.INPUT_FILE_OPTION);
        String foshFilePath = cl.getOptionValue(FOS_HIERARCHY_FILE_OPTION);
        int minCountThr = cl.hasOption(MIN_COUNT_THRESHOLD_OPTION) ?
                Integer.parseInt(cl.getOptionValue(MIN_COUNT_THRESHOLD_OPTION)) : DEFAULT_MIN_COUNT_THRESHOLD;
        String outputFilePath = cl.getOptionValue(Config.OUTPUT_FILE_OPTION);
        convert(inputFilePath, foshFilePath, minCountThr, outputFilePath);
    }
}
