package sub;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Paper;

import java.io.*;
import java.util.*;

public class RefPaperConverter {
    private static final String INPUT_TRAIN_OPTION = "itrain";
    private static final String INPUT_TEST_OPTION = "itest";
    private static final String PAPER_FILE_OPTION = "pf";
    private static final String OUTPUT_TRAIN_OPTION = "otrain";
    private static final String OUTPUT_TEST_OPTION = "otest";

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(INPUT_TRAIN_OPTION, true, true,
                "[input] input training dir", options);
        MiscUtil.setOption(INPUT_TEST_OPTION, true, true,
                "[input] input test dir", options);
        MiscUtil.setOption(PAPER_FILE_OPTION, true, true, "[input] paper-list.csv", options);
        MiscUtil.setOption(OUTPUT_TRAIN_OPTION, true, true,
                "[output] output training dir", options);
        MiscUtil.setOption(OUTPUT_TEST_OPTION, true, true,
                "[output] output test dir", options);
        return options;
    }

    private static HashMap<String, String> buildPaperMap(String paperFilePath) {
        System.out.println("Start: reading " + paperFilePath);
        HashMap<String, String> paperMap = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(paperFilePath)));
            String line;
            while ((line = br.readLine()) != null) {
                Paper paper = new Paper(line);
                StringBuilder sb = new StringBuilder();
                Iterator<String> ite = paper.getAuthorSet().iterator();
                while (ite.hasNext()) {
                    String str = sb.length() == 0 ? ite.next() : Config.SECOND_DELIMITER + ite.next();
                    sb.append(str);
                }
                paperMap.put(paper.id, sb.toString());
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ buildPaperMap");
            e.printStackTrace();
        }

        System.out.println("End: reading " + paperFilePath);
        System.out.println(String.valueOf(paperMap.size()) + " papers in " + paperFilePath);
        return paperMap;
    }

    private static void convertAuthors(String inputDirPath, HashMap<String, String> paperMap, String outputDirPath) {
        try {
            List<File> inputDirList = FileUtil.getDirList(inputDirPath);
            HashSet<String> refPaperIdSet = new HashSet<>();
            int hitCount = 0;
            int dirSize = inputDirList.size();
            for (int i = 0; i < dirSize; i++) {
                System.out.println("\tStage " + String.valueOf(i + 1));
                File inputDir = inputDirList.remove(0);
                String baseOutputDirPath = outputDirPath + inputDir.getName() + "/";
                FileUtil.makeDirIfNotExist(baseOutputDirPath);
                List<File> inputFileList = FileUtil.getFileListR(inputDir.getPath());
                for (File inputFile : inputFileList) {
                    BufferedReader br = new BufferedReader(new FileReader(inputFile));
                    List<String> outputLineList = new ArrayList<>();
                    String line;
                    while ((line = br.readLine()) != null) {
                        Paper paper = new Paper(line);
                        StringBuilder sb = new StringBuilder();
                        for (String refPaperId : paper.refPaperIds) {
                            if (!refPaperIdSet.contains(refPaperId)) {
                                refPaperIdSet.add(refPaperId);
                                if (paperMap.containsKey(refPaperId)) {
                                    hitCount++;
                                }
                            }

                            if (paperMap.containsKey(refPaperId)) {
                                String authorsStr = paperMap.get(refPaperId);
                                String str = sb.length() == 0 ? authorsStr : Config.SECOND_DELIMITER + authorsStr;
                                sb.append(str);
                            }
                        }

                        if (sb.length() > 0) {
                            int lastIdx = line.lastIndexOf(Config.FIRST_DELIMITER);
                            outputLineList.add(line.substring(0, lastIdx) + Config.FIRST_DELIMITER + sb.toString());
                        }
                    }

                    br.close();
                    if (outputLineList.size() > 0) {
                        BufferedWriter bw = new BufferedWriter(new FileWriter(baseOutputDirPath + inputFile.getName()));
                        for (String outputLine : outputLineList) {
                            bw.write(outputLine);
                            bw.newLine();
                        }
                        bw.close();
                    }
                }
            }

            System.out.println(String.valueOf(refPaperIdSet.size()) + " unique ref paper IDs");
            System.out.println(String.valueOf(hitCount) + " hit unique ref paper IDs");
            System.out.println(String.valueOf((double) hitCount / (double) refPaperIdSet.size() * 100.0d) + "%");
        } catch (Exception e) {
            System.err.println("Exception @ convertAuthors");
            e.printStackTrace();
        }
    }

    private static void convertPapers(String inputDirPath, HashMap<String, String> paperMap, String outputDirPath) {
        try {
            System.out.println("\tReading " + inputDirPath);
            List<File> inputFileList = FileUtil.getFileList(inputDirPath);
            FileUtil.makeDirIfNotExist(outputDirPath);
            HashSet<String> refPaperIdSet = new HashSet<>();
            int hitCount = 0;
            for (File inputFile : inputFileList) {
                BufferedReader br = new BufferedReader(new FileReader(inputFile));
                BufferedWriter bw = new BufferedWriter(new FileWriter(outputDirPath + inputFile.getName()));
                String line;
                while ((line = br.readLine()) != null) {
                    Paper paper = new Paper(line);
                    StringBuilder sb = new StringBuilder();
                    for (String refPaperId : paper.refPaperIds) {
                        if (!refPaperIdSet.contains(refPaperId)) {
                            refPaperIdSet.add(refPaperId);
                            if (paperMap.containsKey(refPaperId)) {
                                hitCount++;
                            }
                        }

                        if (paperMap.containsKey(refPaperId)) {
                            String authorsStr = paperMap.get(refPaperId);
                            String str = sb.length() == 0 ? authorsStr : Config.SECOND_DELIMITER + authorsStr;
                            sb.append(str);
                        }
                    }

                    if (sb.length() > 0) {
                        int lastIdx = line.lastIndexOf(Config.FIRST_DELIMITER);
                        bw.write(line.substring(0, lastIdx) + Config.FIRST_DELIMITER + sb.toString());
                        bw.newLine();
                    }
                }

                bw.close();
                br.close();
            }

            System.out.println(String.valueOf(refPaperIdSet.size()) + " unique ref paper IDs");
            System.out.println(String.valueOf(hitCount) + " hit unique ref paper IDs");
            System.out.println(String.valueOf((double) hitCount / (double) refPaperIdSet.size() * 100.0d) + "%");
        } catch (Exception e) {
            System.err.println("Exception @ convertPapers");
            e.printStackTrace();
        }
    }

    private static void convert(String inputTrainDirPath, String inputTestDirPath, String paperFilePath,
                                String outputTrainDirPath, String outputTestDirPath) {
        HashMap<String, String> paperMap = buildPaperMap(paperFilePath);
        convertAuthors(inputTrainDirPath, paperMap, outputTrainDirPath);
        convertPapers(inputTrainDirPath, paperMap, outputTrainDirPath);
        convertPapers(inputTestDirPath, paperMap, outputTestDirPath);
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("RefPaperConverter", options, args);
        String inputTrainDirPath = cl.getOptionValue(INPUT_TRAIN_OPTION);
        String inputTestDirPath = cl.getOptionValue(INPUT_TEST_OPTION);
        String paperFilePath = cl.getOptionValue(PAPER_FILE_OPTION);
        String outputTrainDirPath = cl.getOptionValue(OUTPUT_TRAIN_OPTION);
        String outputTestDirPath = cl.getOptionValue(OUTPUT_TEST_OPTION);
        convert(inputTrainDirPath, inputTestDirPath, paperFilePath, outputTrainDirPath, outputTestDirPath);
    }
}
