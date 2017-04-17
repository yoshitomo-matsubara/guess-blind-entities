package main;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Paper;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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

    private static void convert(String inputDirPath, HashMap<String, String> paperMap, String outputDirPath) {
        List<File> inputFileList = FileUtil.getFileList(inputDirPath);
        System.out.println("\tReading " + inputDirPath);
        try {
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
            System.err.println("Exception @ convert");
            e.printStackTrace();
        }
    }

    private static void convert(String inputTrainDirPath, String inputTestDirPath, String paperFilePath,
                                String outputTrainDirPath, String outputTestDirPath) {
        HashMap<String, String> paperMap = buildPaperMap(paperFilePath);
        convert(inputTrainDirPath, paperMap, outputTrainDirPath);
        convert(inputTestDirPath, paperMap, outputTestDirPath);
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
