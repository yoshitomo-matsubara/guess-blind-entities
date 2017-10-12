package main;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Paper;

import java.io.*;
import java.util.*;

public class PaperExtractor {
    private static final String TRAIN_DIR_OPTION = "train";
    private static final int BUFFER_SIZE = 100000;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(TRAIN_DIR_OPTION, true, true, "[input] training dir", options);
        MiscUtil.setOption(Config.INPUT_FILE_OPTION, true, true, "[input] paper list file", options);
        MiscUtil.setOption(Config.OUTPUT_DIR_OPTION, true, true, "[output] output dir", options);
        return options;
    }

    public static void readAuthorFiles(List<File> trainingFileList, Set<String> paperIdSet) {
        System.out.println("\tStart:\treading author files");
        try {
            for (File trainingFile : trainingFileList) {
                BufferedReader br = new BufferedReader(new FileReader(trainingFile));
                String line;
                while ((line = br.readLine()) != null) {
                    int index = line.indexOf(Config.FIRST_DELIMITER);
                    String paperId = line.substring(0, index);
                    if (!paperIdSet.contains(paperId)) {
                        paperIdSet.add(paperId);
                    }
                }
                br.close();
            }
        } catch (Exception e) {
            System.err.println("Exception @ readAuthorFiles");
            e.printStackTrace();
        }

        System.out.println("\tEnd:\treading author files");
    }

    private static void writeFiles(String inputFilePath, Set<String> paperIdSet, String outputDirPath) {
        try {
            FileUtil.makeDirIfNotExist(outputDirPath);
            Map<String, List<String>> yearMap = new HashMap<>();
            BufferedReader br = new BufferedReader(new FileReader(new File(inputFilePath)));
            String line;
            int count = 0;
            Set<String> yearSet = new HashSet<>();
            while ((line = br.readLine()) != null) {
                Paper paper = new Paper(line);
                if (paperIdSet.contains(paper.id)) {
                    if (!yearMap.containsKey(paper.year)) {
                        yearMap.put(paper.year, new ArrayList<>());
                    }

                    yearMap.get(paper.year).add(line);
                    count++;
                }

                if (count % BUFFER_SIZE == 0 && count > 0) {
                    for (String year : yearMap.keySet()) {
                        List<String> lineList = yearMap.get(year);
                        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputDirPath + year), yearSet.contains(year)));
                        for (String outputLine : lineList) {
                            bw.write(outputLine);
                            bw.newLine();
                        }

                        if (!yearSet.contains(year)) {
                            yearSet.add(year);
                        }
                        bw.close();
                    }
                    yearMap.clear();
                }
            }

            br.close();
            for (String year : yearMap.keySet()) {
                List<String> lineList = yearMap.get(year);
                BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputDirPath + year), yearSet.contains(year)));
                for (String outputLine : lineList) {
                    bw.write(outputLine);
                    bw.newLine();
                }
                bw.close();
            }
        } catch (Exception e) {
            System.err.println("Exception @ writeFiles");
            e.printStackTrace();
        }
    }

    private static void reconvert(String trainingDirPath, String inputFilePath, String outputDirPath) {
        List<File> authorDirList = FileUtil.getDirList(trainingDirPath);
        if (authorDirList.size() == 0) {
            authorDirList.add(new File(trainingDirPath));
        }

        int dirSize = authorDirList.size();
        Set<String> paperIdSet = new HashSet<>();
        for (int i = 0; i < dirSize; i++) {
            File authorDir = authorDirList.remove(0);
            System.out.println("Stage " + String.valueOf(i + 1) + "/" + String.valueOf(dirSize));
            List<File> trainingFileList = FileUtil.getFileListR(authorDir.getPath());
            readAuthorFiles(trainingFileList, paperIdSet);
            trainingFileList.clear();
        }
        writeFiles(inputFilePath, paperIdSet, outputDirPath);
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("PaperExtractor", options, args);
        String trainingDirPath = cl.getOptionValue(TRAIN_DIR_OPTION);
        String inputFilePath = cl.getOptionValue(Config.INPUT_FILE_OPTION);
        String outputDirPath = cl.getOptionValue(Config.OUTPUT_DIR_OPTION);
        reconvert(trainingDirPath, inputFilePath, outputDirPath);
    }
}
