package main;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.*;
import java.util.*;

public class EntityConverter {
    private static final String INPUT_TRAIN_DIR_OPTION = "itrain";
    private static final String INPUT_TEST_DIR_OPTION = "itest";
    private static final String AFFILS_FILE_OPTION = "a";
    private static final String OUTPUT_TRAIN_DIR_OPTION = "otrain";
    private static final String OUTPUT_TEST_DIR_OPTION = "otest";
    private static final int AUTHOR_ID_INDEX = 3;
    private static final int SUFFIX_SIZE = 3;
    private static final int BUFFER_SIZE = 5000000;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(INPUT_TRAIN_DIR_OPTION, true, false, "[input] train dir", options);
        MiscUtil.setOption(INPUT_TEST_DIR_OPTION, true, false, "[input] test dir", options);
        MiscUtil.setOption(AFFILS_FILE_OPTION, true, true, "[input] extra-PaperAuthorAffiliations file", options);
        MiscUtil.setOption(OUTPUT_TRAIN_DIR_OPTION, true, false, "[output] train dir", options);
        MiscUtil.setOption(OUTPUT_TEST_DIR_OPTION, true, false, "[output] test dir", options);
        return options;
    }

    private static HashMap<String, String> buildAffiliationIdMap(String affilsFilePath) {
        System.out.println("\tStart:\treading affiliation file");
        HashMap<String, String> affilIdMap = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(affilsFilePath)));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(Config.FIRST_DELIMITER);
                String[] ids = elements[1].split(Config.SECOND_DELIMITER);
                for (String id : ids) {
                    String[] keyValue = id.split(Config.KEY_VALUE_DELIMITER);
                    affilIdMap.put(elements[0] + keyValue[0], keyValue[1]);
                }
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ buildAffiliationIdMap");
            e.printStackTrace();
        }

        System.out.println("\tEnd:\treading affiliation file");
        return affilIdMap;
    }

    private static boolean checkIfValidParams(String inputDirPath, String outputDirPath) {
        return inputDirPath != null && outputDirPath != null;
    }

    private static void convertPapers(String inputDirPath, HashMap<String, String> affilIdMap, String outputDirPath) {
        System.out.println("\tStart:\tconverting paper files in " + inputDirPath);
        List<File> inputFileList = FileUtil.getFileList(inputDirPath);
        FileUtil.makeDirIfNotExist(outputDirPath);
        try {
            for (File inputFile : inputFileList) {
                String fileName = inputFile.getName();
                File outputFile = new File(outputDirPath + fileName);
                BufferedReader br = new BufferedReader(new FileReader(inputFile));
                BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] elements = line.split(Config.FIRST_DELIMITER);
                    String[] authorIds = elements[3].split(Config.SECOND_DELIMITER);
                    HashSet<String> affilIdSet = new HashSet<>();
                    for (String authorId : authorIds) {
                        String key = elements[0] + authorId;
                        if (affilIdMap.containsKey(key)) {
                            String affilId = affilIdMap.get(key);
                            affilIdSet.add(affilId);
                        }
                    }

                    if (affilIdSet.size() == 0) {
                        continue;
                    }

                    StringBuilder sb = new StringBuilder();
                    Iterator<String> ite = affilIdSet.iterator();
                    while (ite.hasNext()) {
                        String affilId = ite.next();
                        String str = sb.length() == 0 ? affilId : Config.SECOND_DELIMITER + affilId;
                        sb.append(str);
                    }

                    bw.write(elements[0] + Config.FIRST_DELIMITER + elements[1] + Config.FIRST_DELIMITER
                            + elements[2] + Config.FIRST_DELIMITER
                            + sb.toString() + Config.FIRST_DELIMITER + elements[4]);
                    bw.newLine();
                }

                bw.close();
                br.close();
            }
        } catch (Exception e) {
            System.err.println("Exception @ convertPapers");
            e.printStackTrace();
        }
        System.out.println("\tEnd:\tconverting paper files in " + inputDirPath);
    }

    private static void distributeAffiliationFiles(String outputTrainDirPath) {
        System.out.println("\tStart:\tdistributing paper files in " + outputTrainDirPath);
        try {
            HashSet<String> fileNameSet = new HashSet<>();
            HashMap<String, List<String>> paperListMap = new HashMap<>();
            int count = 0;
            List<File> inputFileList = FileUtil.getFileList(outputTrainDirPath);
            for (File inputFile : inputFileList) {
                BufferedReader br = new BufferedReader(new FileReader(inputFile));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] elements = line.split(Config.FIRST_DELIMITER);
                    String[] authorIds = elements[AUTHOR_ID_INDEX].split(Config.SECOND_DELIMITER);
                    for (String authorId : authorIds) {
                        if (!paperListMap.containsKey(authorId)) {
                            paperListMap.put(authorId, new ArrayList<>());
                        }

                        paperListMap.get(authorId).add(line);
                        count++;
                        if (count % BUFFER_SIZE == 0) {
                            FileUtil.distributeFiles(paperListMap, fileNameSet, true, SUFFIX_SIZE, outputTrainDirPath);
                        }
                    }
                }
                br.close();
            }
            FileUtil.distributeFiles(paperListMap, fileNameSet, true, SUFFIX_SIZE, outputTrainDirPath);
        } catch (Exception e) {
            System.err.println("Exception @ convertPapers");
            e.printStackTrace();
        }
        System.out.println("\tEnd:\tdistributing paper files in " + outputTrainDirPath);
    }

    private static void convert(String inputTrainDirPath, String inputTestDirPath, String affilsFilePath,
                                String outputTrainDirPath, String outputTestDirPath) {
        HashMap<String, String> affilIdMap = buildAffiliationIdMap(affilsFilePath);
        if (checkIfValidParams(inputTrainDirPath, outputTrainDirPath)) {
            convertPapers(inputTrainDirPath, affilIdMap, outputTrainDirPath);
            distributeAffiliationFiles(outputTrainDirPath);
        }

        if (checkIfValidParams(inputTestDirPath, outputTestDirPath)) {
            convertPapers(inputTestDirPath, affilIdMap, outputTestDirPath);
        }
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("EntityConverter", options, args);
        String inputTrainDirPath = cl.hasOption(INPUT_TRAIN_DIR_OPTION) ?
                cl.getOptionValue(INPUT_TRAIN_DIR_OPTION) : null;
        String inputTestDirPath = cl.hasOption(INPUT_TEST_DIR_OPTION) ?
                cl.getOptionValue(INPUT_TEST_DIR_OPTION) : null;
        String affilsFilePath = cl.getOptionValue(AFFILS_FILE_OPTION);
        String outputTrainDirPath = cl.hasOption(OUTPUT_TRAIN_DIR_OPTION) ?
                cl.getOptionValue(OUTPUT_TRAIN_DIR_OPTION) : null;
        String outputTestDirPath = cl.hasOption(OUTPUT_TEST_DIR_OPTION) ?
                cl.getOptionValue(OUTPUT_TEST_DIR_OPTION) : null;
        convert(inputTrainDirPath, inputTestDirPath, affilsFilePath, outputTrainDirPath, outputTestDirPath);
    }
}
