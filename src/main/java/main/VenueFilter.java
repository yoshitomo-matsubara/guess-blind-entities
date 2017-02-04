package main;

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

public class VenueFilter {
    private static final String VENUE_ID_LIST_FILE_OPTION = "vid";
    private static final String TRAIN_INPUT_DIR_OPTION = "itrain";
    private static final String TEST_INPUT_DIR_OPTION = "itest";
    private static final String TRAIN_OUTPUT_DIR_OPTION = "otrain";
    private static final String TEST_OUTPUT_DIR_OPTION = "otest";
    private static final int VENUE_ID_INDEX = 0;
    private static final int SUFFIX_SIZE = 3;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(VENUE_ID_LIST_FILE_OPTION, true, true, "[param] venue ID list file", options);
        MiscUtil.setOption(TRAIN_INPUT_DIR_OPTION, true, false,
                "[input, optional] training dir, -" + TRAIN_OUTPUT_DIR_OPTION + " is required", options);
        MiscUtil.setOption(TEST_INPUT_DIR_OPTION, true, false,
                "[input, optional] test dir, -" + TEST_OUTPUT_DIR_OPTION + " is required", options);
        MiscUtil.setOption(TRAIN_OUTPUT_DIR_OPTION, true, false,
                "[output, optional] filtered training dir, -" + TRAIN_INPUT_DIR_OPTION + " is required", options);
        MiscUtil.setOption(TEST_OUTPUT_DIR_OPTION, true, false,
                "[output, optional] filtered test dir, -" + TEST_INPUT_DIR_OPTION + " is required", options);
        return options;
    }

    private static HashSet<String> readVenueIdListFile(String vidListFilePath) {
        HashSet<String> vidSet = new HashSet<>();
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

    private static void filterTrainData(String trainInputDirPath, HashSet<String> vidSet, String trainOutputDirPath) {
        try {
            List<File> inputDirList = FileUtil.getDirList(trainInputDirPath);
            if (inputDirList.size() == 0) {
                inputDirList.add(new File(trainInputDirPath));
            }

            int dirSize = inputDirList.size();
            for (int i = 0; i < dirSize; i++) {
                File dir = inputDirList.remove(0);
                List<File> inputFileList = FileUtil.getFileListR(dir.getPath());
                int fileSize = inputFileList.size();
                for (int j = 0; j < fileSize; j++) {
                    File inputFile = inputFileList.remove(0);
                    boolean isHit = false;
                    List<String> lineList = new ArrayList<>();
                    BufferedReader br = new BufferedReader(new FileReader(inputFile));
                    String line;
                    while ((line = br.readLine()) != null) {
                        lineList.add(line);
                        Paper paper = new Paper(line);
                        if (vidSet.contains(paper.venueId)) {
                            isHit = true;
                        }
                    }

                    br.close();
                    if (isHit) {
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

    private static void filterTestData(String testInputDirPath, HashSet<String> vidSet, String testOutputDirPath) {
        try {
            List<File> inputDirList = FileUtil.getDirList(testInputDirPath);
            if (inputDirList.size() == 0) {
                inputDirList.add(new File(testInputDirPath));
            }

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

    private static void filter(String vidListFilePath, String trainInputDirPath, String testInputDirPath,
                               String trainOutputDirPath, String testOutputDirPath) {
        HashSet<String> vidSet = readVenueIdListFile(vidListFilePath);
        if (vidSet.size() == 0) {
            return;
        }

        if (trainInputDirPath != null && trainOutputDirPath != null) {
            filterTrainData(trainInputDirPath, vidSet, trainOutputDirPath);
        }

        if (testInputDirPath != null && testOutputDirPath != null) {
            filterTestData(testInputDirPath, vidSet, testOutputDirPath);
        }
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("VenueFilter", options, args);
        String vidListFilePath = cl.getOptionValue(VENUE_ID_LIST_FILE_OPTION);
        String trainInputDirPath = cl.hasOption(TRAIN_INPUT_DIR_OPTION) ?
                cl.getOptionValue(TRAIN_INPUT_DIR_OPTION) : null;
        String testInputDirPath = cl.hasOption(TEST_INPUT_DIR_OPTION) ?
                cl.getOptionValue(TEST_INPUT_DIR_OPTION) : null;
        String trainOutputDirPath = cl.hasOption(TRAIN_OUTPUT_DIR_OPTION) ?
                cl.getOptionValue(TRAIN_OUTPUT_DIR_OPTION) : null;
        String testOutputDirPath = cl.hasOption(TEST_OUTPUT_DIR_OPTION) ?
                cl.getOptionValue(TEST_OUTPUT_DIR_OPTION) : null;
        filter(vidListFilePath, trainInputDirPath, testInputDirPath, trainOutputDirPath, testOutputDirPath);
    }
}
