package main;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import structure.Paper;

import java.io.*;
import java.util.HashSet;
import java.util.List;

public class SeenFilter {
    private static final String TRAIN_DIR_OPTION = "train";
    private static final String TEST_DIR_OPTION = "test";

    private static Options setOptions() {
        Options options = new Options();
        options.addOption(Option.builder(TRAIN_DIR_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[input] training dir")
                .build());
        options.addOption(Option.builder(TEST_DIR_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[input] test dir")
                .build());
        options.addOption(Option.builder(Config.OUTPUT_DIR_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[output] output dir")
                .build());
        return options;
    }

    private static void filter(File testFile, HashSet<String> authorIdSet, String outputDirPath) {
        try {
            File outputFile = new File(outputDirPath + "/" + testFile.getName());
            BufferedReader br = new BufferedReader(new FileReader(testFile));
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
            String line;
            while ((line = br.readLine()) != null) {
                Paper paper = new Paper(line);
                if (MiscUtil.checkIfAuthorExists(paper.getAuthorSet(), authorIdSet)) {
                    bw.write(line);
                    bw.newLine();
                }
            }

            bw.close();
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ filter");
            e.printStackTrace();
        }
    }

    private static void filter(String trainingDirPath, String testDirPath, String outputDirPath) {
        List<File> trainingFileList = FileUtil.getFileListR(trainingDirPath);
        HashSet<String> authorIdSet = MiscUtil.buildAuthorIdSet(trainingFileList);
        List<File> testFileList = FileUtil.getFileList(testDirPath);
        FileUtil.makeIfNotExist(outputDirPath);
        for (File testFile : testFileList) {
            filter(testFile, authorIdSet, outputDirPath);
        }
    }

    public static void main(String[] args) {
        Options options = setOptions();
        CommandLine cl = MiscUtil.setParams("SeenFilter", options, args);
        String trainingDirPath = cl.getOptionValue(TRAIN_DIR_OPTION);
        String testDirPath = cl.getOptionValue(TEST_DIR_OPTION);
        String outputDirPath = cl.getOptionValue(Config.OUTPUT_DIR_OPTION);
        filter(trainingDirPath, testDirPath, outputDirPath);
    }
}
