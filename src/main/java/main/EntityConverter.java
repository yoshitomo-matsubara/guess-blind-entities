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
    private static final String ENTITY_MAPPING_FILE_OPTION = "em";
    private static final String OUTPUT_TRAIN_DIR_OPTION = "otrain";
    private static final String OUTPUT_TEST_DIR_OPTION = "otest";
    private static final int AUTHOR_ID_INDEX = 3;
    private static final int SUFFIX_SIZE = 3;
    private static final int BUFFER_SIZE = 5000000;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(INPUT_TRAIN_DIR_OPTION, true, false, "[input] train dir", options);
        MiscUtil.setOption(INPUT_TEST_DIR_OPTION, true, false, "[input] test dir", options);
        MiscUtil.setOption(ENTITY_MAPPING_FILE_OPTION, true, true, "[input] extra-PaperAuthor* file", options);
        MiscUtil.setOption(OUTPUT_TRAIN_DIR_OPTION, true, false, "[output] train dir", options);
        MiscUtil.setOption(OUTPUT_TEST_DIR_OPTION, true, false, "[output] test dir", options);
        return options;
    }

    private static Map<String, String> buildEntityIdMap(String entityMappingFilePath) {
        System.out.println("\tStart:\treading entity mapping file");
        Map<String, String> entityIdMap = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(entityMappingFilePath)));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(Config.FIRST_DELIMITER);
                String[] ids = elements[1].split(Config.SECOND_DELIMITER);
                for (String id : ids) {
                    String[] keyValue = id.split(Config.KEY_VALUE_DELIMITER);
                    entityIdMap.put(elements[0] + keyValue[0], keyValue[1]);
                }
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ buildAffiliationIdMap");
            e.printStackTrace();
        }

        System.out.println("\tEnd:\treading entity mapping file");
        return entityIdMap;
    }

    private static boolean checkIfValidParams(String inputDirPath, String outputDirPath) {
        return inputDirPath != null && outputDirPath != null;
    }

    private static void convertPapers(String inputDirPath, Map<String, String> entityIdMap, String outputDirPath) {
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
                    Set<String> entityIdSet = new HashSet<>();
                    for (String authorId : authorIds) {
                        String key = elements[0] + authorId;
                        if (entityIdMap.containsKey(key)) {
                            String entityId = entityIdMap.get(key);
                            entityIdSet.add(entityId);
                        }
                    }

                    if (entityIdSet.size() == 0) {
                        continue;
                    }

                    StringBuilder sb = new StringBuilder();
                    Iterator<String> ite = entityIdSet.iterator();
                    while (ite.hasNext()) {
                        String entityId = ite.next();
                        String str = sb.length() == 0 ? entityId : Config.SECOND_DELIMITER + entityId;
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
            Set<String> fileNameSet = new HashSet<>();
            Map<String, List<String>> paperListMap = new HashMap<>();
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

    private static void convert(String inputTrainDirPath, String inputTestDirPath, String entityMappingFilePath,
                                String outputTrainDirPath, String outputTestDirPath) {
        Map<String, String> entityIdMap = buildEntityIdMap(entityMappingFilePath);
        if (checkIfValidParams(inputTrainDirPath, outputTrainDirPath)) {
            convertPapers(inputTrainDirPath, entityIdMap, outputTrainDirPath);
            distributeAffiliationFiles(outputTrainDirPath);
        }

        if (checkIfValidParams(inputTestDirPath, outputTestDirPath)) {
            convertPapers(inputTestDirPath, entityIdMap, outputTestDirPath);
        }
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("EntityConverter", options, args);
        String inputTrainDirPath = cl.hasOption(INPUT_TRAIN_DIR_OPTION) ?
                cl.getOptionValue(INPUT_TRAIN_DIR_OPTION) : null;
        String inputTestDirPath = cl.hasOption(INPUT_TEST_DIR_OPTION) ?
                cl.getOptionValue(INPUT_TEST_DIR_OPTION) : null;
        String entityMappingFilePath = cl.getOptionValue(ENTITY_MAPPING_FILE_OPTION);
        String outputTrainDirPath = cl.hasOption(OUTPUT_TRAIN_DIR_OPTION) ?
                cl.getOptionValue(OUTPUT_TRAIN_DIR_OPTION) : null;
        String outputTestDirPath = cl.hasOption(OUTPUT_TEST_DIR_OPTION) ?
                cl.getOptionValue(OUTPUT_TEST_DIR_OPTION) : null;
        convert(inputTrainDirPath, inputTestDirPath, entityMappingFilePath, outputTrainDirPath, outputTestDirPath);
    }
}
