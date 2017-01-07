package main;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import structure.Pair;
import structure.Paper;
import structure.Result;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Evaluator {
    private static Options setOptions() {
        Options options = new Options();
        options.addOption(Option.builder(Config.INPUT_DIR_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[input] input dir")
                .build());
        options.addOption(Option.builder(Config.OUTPUT_FILE_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[output] output file")
                .build());
        return options;
    }

    private static Pair<Paper, List<Result>> readScoreFile(File file) {
        List<Result> resultList = new ArrayList<>();
        Paper paper = null;
        try {
            boolean authorIncluded = false;
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            paper = new Paper(line);
            while ((line = br.readLine()) != null) {
                Result result = new Result(line);
                if (paper.checkIfAuthor(result.authorId)) {
                    authorIncluded = true;
                }
                resultList.add(result);
            }

            br.close();
            if (!authorIncluded) {
                return null;
            }
        } catch (Exception e) {
            System.err.println("Exception @ readScoreFile");
            e.printStackTrace();
        }
        return new Pair(paper, resultList);
    }

    private static String evaluate(List<Result> resultList, Paper paper) {
        int trueAuthorSize = paper.getAuthorSize();
        int authorSize = 0;
        for (int i = 0; i < trueAuthorSize; i++) {
            Result result = resultList.get(i);
            if (paper.checkIfAuthor(result.authorId)) {
                authorSize++;
            }
        }

        int overOneAtX = authorSize > 0 ? 1 : 0;
        double recallAtX = (double) authorSize / (double) trueAuthorSize;
        return String.valueOf(trueAuthorSize) + Config.FIRST_DELIMITER + String.valueOf(authorSize)
                + Config.FIRST_DELIMITER + String.valueOf(overOneAtX) + Config.FIRST_DELIMITER
                + String.valueOf(recallAtX);
    }

    private static void evaluate(String inputDirPath, String outputFilePath) {
        try {
            FileUtil.makeParentDir(outputFilePath);
            File outputFile = new File(outputFilePath);
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
            List<File> inputFileList = FileUtil.getFileList(inputDirPath);
            int size = inputFileList.size();
            for (int i = 0; i < size; i++) {
                File inputFile = inputFileList.remove(0);
                Pair<Paper, List<Result>> resultPair = readScoreFile(inputFile);
                Paper paper = resultPair.key;
                List<Result> resultList = resultPair.value;
                if (resultPair == null || paper == null || resultList.size() == 0) {
                    continue;
                }

                Collections.sort(resultList);
                String outputLine = evaluate(resultList, paper);
                bw.write(outputLine);
                bw.newLine();
            }
            bw.close();
        } catch (Exception e) {
            System.err.println("Exception @ evaluate");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Options options = setOptions();
        CommandLine cl = MiscUtil.setParams("Evaluator", options, args);
        String inputDirPath = cl.getOptionValue(Config.INPUT_DIR_OPTION);
        String outputFilePath = cl.getOptionValue(Config.OUTPUT_FILE_OPTION);
        evaluate(inputDirPath, outputFilePath);
    }
}
