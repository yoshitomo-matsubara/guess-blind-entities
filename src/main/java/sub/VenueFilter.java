package sub;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Paper;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VenueFilter {
    private static final String VENUE_ID_LIST_FILE_OPTION = "vid";
    private static final String MIN_HIT_COUNT_OPTION = "mhc";
    private static final String TRAIN_INPUT_DIR_OPTION = "itrain";
    private static final String TEST_INPUT_DIR_OPTION = "itest";
    private static final String TRAIN_START_YEAR_OPTION = "strain";
    private static final String TRAIN_END_YEAR_OPTION = "etrain";
    private static final String TEST_START_YEAR_OPTION = "stest";
    private static final String TEST_END_YEAR_OPTION = "etest";
    private static final String TRAIN_OUTPUT_DIR_OPTION = "otrain";
    private static final String TEST_OUTPUT_DIR_OPTION = "otest";
    private static final int VENUE_ID_INDEX = 0;
    private static final int SUFFIX_SIZE = 3;
    private static final int DEFAULT_MIN_HIT_COUNT = 1;
    private static final int INVALID_VALUE = -1;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(VENUE_ID_LIST_FILE_OPTION, true, true, "[param] venue ID list file", options);
        MiscUtil.setOption(MIN_HIT_COUNT_OPTION, true, false,
                "[param, option] min hit count for training", options);
        MiscUtil.setOption(TRAIN_INPUT_DIR_OPTION, true, false,
                "[input, optional] training dir, -" + TRAIN_OUTPUT_DIR_OPTION + " is required", options);
        MiscUtil.setOption(TEST_INPUT_DIR_OPTION, true, false,
                "[input, optional] test dir, -" + TEST_OUTPUT_DIR_OPTION + " is required", options);
        MiscUtil.setOption(TRAIN_OUTPUT_DIR_OPTION, true, false,
                "[output, optional] filtered training dir, -" + TRAIN_INPUT_DIR_OPTION + " is required", options);
        MiscUtil.setOption(TRAIN_START_YEAR_OPTION, true, false,
                "[param, optional] start year for training, -" + TRAIN_END_YEAR_OPTION + " is required", options);
        MiscUtil.setOption(TRAIN_END_YEAR_OPTION, true, false,
                "[param, optional] end year for training, -" + TRAIN_START_YEAR_OPTION + " is required", options);
        MiscUtil.setOption(TEST_START_YEAR_OPTION, true, false,
                "[param, optional] start year for testing, -" + TEST_END_YEAR_OPTION + " is required", options);
        MiscUtil.setOption(TEST_END_YEAR_OPTION, true, false,
                "[param, optional] end year for testing, -" + TEST_START_YEAR_OPTION + " is required", options);
        MiscUtil.setOption(TEST_OUTPUT_DIR_OPTION, true, false,
                "[output, optional] filtered test dir, -" + TEST_INPUT_DIR_OPTION + " is required", options);
        return options;
    }

    private static Set<String> readVenueIdListFile(String vidListFilePath) {
        Set<String> vidSet = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(vidListFilePath)));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(Config.FIRST_DELIMITER);
                vidSet.add(elements[VENUE_ID_INDEX]);
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ readVenueIdListFile");
            e.printStackTrace();
        }
        return vidSet;
    }

    private static void filterTrainData(String trainInputDirPath, Set<String> vidSet, int minHitCount,
                                        int trainStartYear, int trainEndYear, String trainOutputDirPath) {
        try {
            List<File> inputDirList = FileUtil.getDirList(trainInputDirPath);
            if (inputDirList.size() == 0) {
                inputDirList.add(new File(trainInputDirPath));
            }

            boolean isYearFiltered = trainStartYear != INVALID_VALUE && trainEndYear != INVALID_VALUE
                        && trainStartYear <= trainEndYear;
            int dirSize = inputDirList.size();
            for (int i = 0; i < dirSize; i++) {
                File dir = inputDirList.remove(0);
                List<File> inputFileList = FileUtil.getFileListR(dir.getPath());
                int fileSize = inputFileList.size();
                for (int j = 0; j < fileSize; j++) {
                    File inputFile = inputFileList.remove(0);
                    int hitCount = 0;
                    List<String> lineList = new ArrayList<>();
                    BufferedReader br = new BufferedReader(new FileReader(inputFile));
                    String line;
                    while ((line = br.readLine()) != null) {
                        lineList.add(line);
                        Paper paper = new Paper(line);
                        int year = Integer.parseInt(paper.year);
                        if (isYearFiltered && (year < trainStartYear || year > trainEndYear)) {
                            continue;
                        }

                        if (vidSet.contains(paper.venueId)) {
                            hitCount++;
                        }
                    }

                    br.close();
                    if (hitCount >= minHitCount) {
                        String fileName = inputFile.getName();
                        String suffix = fileName.substring(fileName.length() - SUFFIX_SIZE);
                        File outputFile = new File(trainOutputDirPath + suffix + "/" + fileName);
                        FileUtil.makeParentDir(outputFile.getPath());
                        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
                        for (String outputLine : lineList) {
                            bw.write(outputLine);
                            bw.newLine();
                        }
                        bw.close();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Exception @ filterTestData");
            e.printStackTrace();
        }
    }

    private static void filterTestData(String testInputDirPath, Set<String> vidSet, int testStartYear,
                                       int testEndYear, String testOutputDirPath) {
        try {
            List<File> inputDirList = FileUtil.getDirList(testInputDirPath);
            if (inputDirList.size() == 0) {
                inputDirList.add(new File(testInputDirPath));
            }

            boolean isYearFiltered = testStartYear != INVALID_VALUE && testEndYear != INVALID_VALUE
                        && testStartYear <= testEndYear;
            FileUtil.makeDirIfNotExist(testOutputDirPath);
            int dirSize = inputDirList.size();
            for (int i = 0; i < dirSize; i++) {
                File dir = inputDirList.remove(0);
                List<File> inputFileList = FileUtil.getFileListR(dir.getPath());
                int fileSize = inputFileList.size();
                for (int j = 0; j < fileSize; j++) {
                    File inputFile = inputFileList.remove(0);
                    File outputFile = new File(testOutputDirPath + inputFile.getName());
                    BufferedReader br = new BufferedReader(new FileReader(inputFile));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
                    String line;
                    while ((line = br.readLine()) != null) {
                        Paper paper = new Paper(line);
                        int year = Integer.parseInt(paper.year);
                        if (isYearFiltered && (year < testStartYear || year > testEndYear)) {
                            continue;
                        }

                        if (vidSet.contains(paper.venueId)) {
                            bw.write(line);
                            bw.newLine();
                        }
                    }

                    bw.close();
                    br.close();
                }
            }
        } catch (Exception e) {
            System.err.println("Exception @ filterTestData");
            e.printStackTrace();
        }
    }

    private static void filter(String vidListFilePath, int minHitCount, String trainInputDirPath,
                               String testInputDirPath, int trainStartYear, int trainEndYear, int testStartYear,
                               int testEndYear, String trainOutputDirPath, String testOutputDirPath) {
        Set<String> vidSet = readVenueIdListFile(vidListFilePath);
        if (vidSet.size() == 0) {
            return;
        }

        if (trainInputDirPath != null && trainOutputDirPath != null) {
            filterTrainData(trainInputDirPath, vidSet, minHitCount, trainStartYear, trainEndYear, trainOutputDirPath);
        }

        if (testInputDirPath != null && testOutputDirPath != null) {
            filterTestData(testInputDirPath, vidSet, testStartYear, testEndYear, testOutputDirPath);
        }
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("VenueFilter", options, args);
        String vidListFilePath = cl.getOptionValue(VENUE_ID_LIST_FILE_OPTION);
        int minHitCount = cl.hasOption(MIN_HIT_COUNT_OPTION) ?
                Integer.parseInt(MIN_HIT_COUNT_OPTION) : DEFAULT_MIN_HIT_COUNT;
        String trainInputDirPath = cl.hasOption(TRAIN_INPUT_DIR_OPTION) ?
                cl.getOptionValue(TRAIN_INPUT_DIR_OPTION) : null;
        String testInputDirPath = cl.hasOption(TEST_INPUT_DIR_OPTION) ?
                cl.getOptionValue(TEST_INPUT_DIR_OPTION) : null;
        int trainStartYear = cl.hasOption(TRAIN_START_YEAR_OPTION) ?
                Integer.parseInt(cl.getOptionValue(TRAIN_START_YEAR_OPTION)) : INVALID_VALUE;
        int trainEndYear = cl.hasOption(TRAIN_END_YEAR_OPTION) ?
                Integer.parseInt(cl.getOptionValue(TRAIN_END_YEAR_OPTION)) : INVALID_VALUE;
        int testStartYear = cl.hasOption(TEST_START_YEAR_OPTION) ?
                Integer.parseInt(cl.getOptionValue(TEST_START_YEAR_OPTION)) : INVALID_VALUE;
        int testEndYear = cl.hasOption(TEST_END_YEAR_OPTION) ?
                Integer.parseInt(cl.getOptionValue(TEST_END_YEAR_OPTION)) : INVALID_VALUE;
        String trainOutputDirPath = cl.hasOption(TRAIN_OUTPUT_DIR_OPTION) ?
                cl.getOptionValue(TRAIN_OUTPUT_DIR_OPTION) : null;
        String testOutputDirPath = cl.hasOption(TEST_OUTPUT_DIR_OPTION) ?
                cl.getOptionValue(TEST_OUTPUT_DIR_OPTION) : null;
        filter(vidListFilePath, minHitCount, trainInputDirPath, testInputDirPath, trainStartYear, trainEndYear,
                testStartYear, testEndYear, trainOutputDirPath, testOutputDirPath);
    }
}
