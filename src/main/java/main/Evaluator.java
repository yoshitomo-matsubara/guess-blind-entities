package main;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Pair;
import structure.Paper;
import structure.Result;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Evaluator {
    private static final String TOP_M_OPTION = "m";

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(Config.INPUT_DIR_OPTION, true, true, "[input] input dir", options);
        MiscUtil.setOption(TOP_M_OPTION, true, true,
                "[param] top M authors in rankings used for evaluation [can be plural, separate with comma]", options);
        MiscUtil.setOption(Config.OUTPUT_FILE_OPTION, true, true, "[output] output file", options);
        return options;
    }

    private static int[] convertToIntArray(String str) {
        String[] elements = str.split(Config.OPTION_DELIMITER);
        List<Integer> list = new ArrayList<>();
        for (String element : elements) {
            list.add(Integer.parseInt(element));
        }

        Collections.sort(list);
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
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

    private static String evaluate(List<Result> resultList, int[] topMs, Paper paper) {
        Collections.sort(resultList);
        int trueAuthorSize = paper.getAuthorSize();
        int resultSize = resultList.size();
        int authorSizeX = 0;
        int[] authorSizeMs = MiscUtil.initIntArray(topMs.length, 0);
        int size = topMs[topMs.length - 1] > trueAuthorSize ? topMs[topMs.length - 1] : trueAuthorSize;
        if (resultSize < size) {
            size = resultSize;
        }

        for (int i = 0; i < size; i++) {
            Result result = resultList.get(i);
            if (paper.checkIfAuthor(result.authorId) && result.score > 0.0d) {
                if (i < trueAuthorSize) {
                    authorSizeX++;
                }

                for (int j = 0; j < authorSizeMs.length; j++) {
                    if (i < topMs[j]) {
                        authorSizeMs[j]++;
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        int overOneAtX = authorSizeX > 0 ? 1 : 0;
        double recallAtX = (double) authorSizeX / (double) trueAuthorSize;
        sb.append(paper.id + Config.FIRST_DELIMITER + String.valueOf(trueAuthorSize) + Config.FIRST_DELIMITER
                + String.valueOf(authorSizeX) + Config.FIRST_DELIMITER + String.valueOf(overOneAtX)
                + Config.FIRST_DELIMITER + String.valueOf(recallAtX));
        for (int i = 0; i < authorSizeMs.length; i++) {
            int overOneAtM = authorSizeMs[i] > 0 ? 1 : 0;
            double recallAtM = (double) authorSizeMs[i] / (double) topMs[i];
            sb.append(Config.FIRST_DELIMITER + Config.FIRST_DELIMITER + String.valueOf(authorSizeMs[i])
                    + Config.FIRST_DELIMITER + String.valueOf(overOneAtM)
                    + Config.FIRST_DELIMITER + String.valueOf(recallAtM));
        }
        return sb.toString();
    }

    private static void evaluate(String inputDirPath, String topMsStr, String outputFilePath) {
        try {
            int[] topMs = convertToIntArray(topMsStr);
            FileUtil.makeParentDir(outputFilePath);
            File outputFile = new File(outputFilePath);
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
            bw.write("paper ID\ttrue author count\thit author count\tHAL@X\tRecall@X\t");
            for (int i = 0; i < topMs.length; i++) {
                String mStr = String.valueOf(topMs[i]);
                bw.write("\tauthor hit count@" + mStr + "\tHAL@" + String.valueOf(topMs[i]) + "\tRecall@" + mStr + "\t");
            }
            
            bw.newLine();
            List<File> inputFileList = FileUtil.getFileList(inputDirPath);
            int size = inputFileList.size();
            for (int i = 0; i < size; i++) {
                File inputFile = inputFileList.remove(0);
                Pair<Paper, List<Result>> resultPair = readScoreFile(inputFile);
                if (resultPair == null || resultPair.key == null || resultPair.value.size() == 0) {
                    continue;
                }

                Paper paper = resultPair.key;
                List<Result> resultList = resultPair.value;
                String outputLine = evaluate(resultList, topMs, paper);
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
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("Evaluator", options, args);
        String inputDirPath = cl.getOptionValue(Config.INPUT_DIR_OPTION);
        String topMsStr = cl.getOptionValue(TOP_M_OPTION);
        String outputFilePath = cl.getOptionValue(Config.OUTPUT_FILE_OPTION);
        evaluate(inputDirPath, topMsStr, outputFilePath);
    }
}
