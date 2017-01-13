package main;

import common.Config;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.*;
import java.util.*;

public class PaperSeparator {
    private static final String TRAIN_START_YEAR_OPTION = "strain";
    private static final String TRAIN_END_YEAR_OPTION = "etrain";
    private static final String TRAIN_OUTPUT_DIR_OPTION = "otrain";
    private static final String TEST_START_YEAR_OPTION = "stest";
    private static final String TEST_END_YEAR_OPTION = "etest";
    private static final String TEST_OUTPUT_DIR_OPTION = "otest";
    private static final String SAMPLE_RATE_OPTION = "rate";
    private static final int PAPER_ELEMENT_SIZE = 5;
    private static final int AUTHOR_ID_INDEX = 3;
    private static final int INVALID_VALUE = -1;
    private static final int TRAIN_BUFFER_SIZE = 5000000;
    private static final int TEST_BUFFER_SIZE = 2500000;
    private static final float INVALID_RATE = -Float.MAX_VALUE;

    private static Options setOptions() {
        Options options = new Options();
        options.addOption(Option.builder(Config.INPUT_FILE_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[input] input file")
                .build());
        options.addOption(Option.builder(TRAIN_START_YEAR_OPTION)
                .hasArg(true)
                .required(false)
                .desc("[param, optional] start year for training, -" + TRAIN_END_YEAR_OPTION + " is required")
                .build());
        options.addOption(Option.builder(TRAIN_END_YEAR_OPTION)
                .hasArg(true)
                .required(false)
                .desc("[param, optional] end year for training, -" + TRAIN_START_YEAR_OPTION + " is required")
                .build());
        options.addOption(Option.builder(TRAIN_OUTPUT_DIR_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[output, optional] output training dir, -" + TRAIN_START_YEAR_OPTION + " and -"
                        + TRAIN_END_YEAR_OPTION + " are required")
                .build());
        options.addOption(Option.builder(TEST_START_YEAR_OPTION)
                .hasArg(true)
                .required(false)
                .desc("[param, optional] start year for testing, -" + TEST_END_YEAR_OPTION + " is required")
                .build());
        options.addOption(Option.builder(TEST_END_YEAR_OPTION)
                .hasArg(true)
                .required(false)
                .desc("[param, optional] end year for testing, -" + TEST_START_YEAR_OPTION + " is required")
                .build());
        options.addOption(Option.builder(TEST_OUTPUT_DIR_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[output, optional] output test dir, -" + TEST_START_YEAR_OPTION + " and -"
                        + TEST_END_YEAR_OPTION + " are required")
                .build());
        options.addOption(Option.builder(SAMPLE_RATE_OPTION)
                .hasArg(true)
                .required(false)
                .desc("[param, optional] random sampling rate (0 < rate <= 1)")
                .build());
        return options;
    }

    private static boolean checkIfValidParams(int startYear, int endYear, String outputDirPath) {
        if (startYear == INVALID_VALUE || endYear == INVALID_VALUE || outputDirPath == null) {
            return false;
        } else if (endYear < startYear) {
            return false;
        }
        return true;
    }

    private static void distributeFiles(HashMap<String, List<String>> hashMap,
                                       HashSet<String> fileNameSet, String outputDirPath) {
        try {
            for (String key : hashMap.keySet()) {
                File outputFile = new File(outputDirPath + "/" + key);
                String outputFileName = outputFile.getName();
                BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, fileNameSet.contains(outputFileName)));
                List<String> valueList = hashMap.get(key);
                for (String value : valueList) {
                    bw.write(value);
                    bw.newLine();
                }

                bw.close();
                fileNameSet.add(outputFileName);
            }
        } catch (Exception e) {
            System.err.println("Exception @ distributeFiles");
            e.printStackTrace();
        }
        hashMap.clear();
    }

    private static boolean checkIfValidPaper(String[] elements) {
        if (elements.length != PAPER_ELEMENT_SIZE) {
            return false;
        }

        for (int i = 0; i < elements.length; i++) {
            if (elements[i] == null || elements[i].length() == 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkIfValidMode(boolean mode, int startYear, int endYear, int year) {
        return mode && startYear <= year && year <= endYear;
    }

    private static void separate(String inputFilePath, int trainStartYear, int trainEndYear, int testStartYear,
                                 int testEndYear, float sampleRate, String outputTrainDirPath, String outputTestDirPath) {
        try {
            boolean trainMode = checkIfValidParams(trainStartYear, trainEndYear, outputTrainDirPath);
            boolean testMode = checkIfValidParams(testStartYear, testEndYear, outputTestDirPath);
            if (trainMode) {
                File dir = new File(outputTrainDirPath);
                dir.mkdirs();
            }

            if (testMode) {
                File dir = new File(outputTestDirPath);
                dir.mkdirs();
            }

            Random rand = new Random();
            boolean isSampled = sampleRate != INVALID_RATE;
            int trainCount = 0;
            int testCount = 0;
            HashMap<String, List<String>> trainListMap = new HashMap<>();
            HashMap<String, List<String>> testListMap = new HashMap<>();
            HashSet<String> trainFileNameSet = new HashSet<>();
            HashSet<String> testFileNameSet = new HashSet<>();
            BufferedReader br = new BufferedReader(new FileReader(new File(inputFilePath)));
            String line;
            while ((line = br.readLine()) != null) {
                String yearStr = line.split(Config.FIRST_DELIMITER)[1];
                int year = Integer.parseInt(yearStr);
                String[] elements = line.split(Config.FIRST_DELIMITER);
                if (!checkIfValidPaper(elements)) {
                    continue;
                }

                if (checkIfValidMode(trainMode, trainStartYear, trainEndYear, year)) {
                    if (isSampled && rand.nextFloat() > sampleRate) {
                        continue;
                    }

                    // key: author ID
                    String[] authorIds = elements[AUTHOR_ID_INDEX].split(Config.SECOND_DELIMITER);
                    for (String authorId : authorIds) {
                        if (!trainListMap.containsKey(authorId)) {
                            trainListMap.put(authorId, new ArrayList<>());
                        }

                        trainListMap.get(authorId).add(line);
                        trainCount++;
                        if (trainCount % TRAIN_BUFFER_SIZE == 0) {
                            distributeFiles(trainListMap, trainFileNameSet, outputTrainDirPath);
                        }
                    }
                } else if (checkIfValidMode(testMode, testStartYear, testEndYear, year)) {
                    if (isSampled && rand.nextFloat() > sampleRate) {
                        continue;
                    }

                    if (!testListMap.containsKey(yearStr)) {
                        testListMap.put(yearStr, new ArrayList<>());
                    }

                    // key: year
                    testListMap.get(yearStr).add(line);
                    testCount++;
                    if (testCount % TEST_BUFFER_SIZE == 0) {
                        distributeFiles(testListMap, testFileNameSet, outputTestDirPath);
                    }
                }
            }

            br.close();
            distributeFiles(trainListMap, trainFileNameSet, outputTrainDirPath);
            distributeFiles(testListMap, testFileNameSet, outputTestDirPath);
        } catch (Exception e) {
            System.err.println("Exception @ separate");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Options options = setOptions();
        CommandLine cl = MiscUtil.setParams("PaperSeparator", options, args);
        String inputFilePath = cl.getOptionValue(Config.INPUT_FILE_OPTION);
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
        float sampleRate = cl.hasOption(SAMPLE_RATE_OPTION) ?
                Float.parseFloat(cl.getOptionValue(SAMPLE_RATE_OPTION)) : INVALID_RATE;
        separate(inputFilePath, trainStartYear, trainEndYear, testStartYear,
                testEndYear, sampleRate, trainOutputDirPath, testOutputDirPath);
    }
}
