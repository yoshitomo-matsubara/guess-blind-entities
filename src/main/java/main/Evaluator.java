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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Evaluator {
    private static final String TOP_M_OPTION = "m";
    private static final String HAL_OPTION = "hal";
    private static final String HALX_OPTION = "halx";
    private static final String UNGUESSABLE_PAPER_LIST_OUTPUT_OPTION = "uplo";
    private static final int DEFAULT_HAL_THRESHOLD = 1;
    private static final int HALX_LABEL = -1;
    private static final int INVALID_RANKING = -1;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(Config.INPUT_DIR_OPTION, true, true, "[input] input dir", options);
        MiscUtil.setOption(TOP_M_OPTION, true, true,
                "[param] top M authors in rankings used for evaluation (can be plural, separate with comma)", options);
        MiscUtil.setOption(HAL_OPTION, true, false,
                "[param, optional] HAL (Hit At Least) threshold (default = " + String.valueOf(DEFAULT_HAL_THRESHOLD)
                        + ")", options);
        MiscUtil.setOption(HALX_OPTION, false, false,
                "[param, optional] HAL (Hit At Least) threshold = # of true authors in each paper", options);
        MiscUtil.setOption(UNGUESSABLE_PAPER_LIST_OUTPUT_OPTION, true, false, "[optional, output] unguessable paper list output file", options);
        MiscUtil.setOption(Config.OUTPUT_FILE_OPTION, true, true, "[output] output file", options);
        return options;
    }

    private static String createHeader(String halThrStr, int[] topMs) {
        StringBuilder sb = new StringBuilder();
        sb.append("paper ID" + Config.FIRST_DELIMITER + "true author count" + Config.FIRST_DELIMITER + "best ranking"
                + Config.FIRST_DELIMITER + "author hit count" + Config.FIRST_DELIMITER + "HAL" + halThrStr + "@X"
                + Config.FIRST_DELIMITER + "coverage@X");
        for (int i = 0; i < topMs.length; i++) {
            String mStr = String.valueOf(topMs[i]);
            sb.append(Config.FIRST_DELIMITER + Config.FIRST_DELIMITER + "author hit count@" + mStr
                    + Config.FIRST_DELIMITER + "HAL" + halThrStr + "@" + mStr + Config.FIRST_DELIMITER + "coverage@"
                    + mStr);
        }
        return sb.toString();
    }

    private static int decideThreshold(Paper paper, int halThr) {
        return halThr == HALX_LABEL ? paper.getAuthorSize() : halThr;
    }

    public static Pair<Paper, List<Result>> readScoreFile(File file, int halThr) {
        List<Result> resultList = new ArrayList<>();
        Paper paper = null;
        try {
            int authorCount = 0;
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            paper = new Paper(line);
            while ((line = br.readLine()) != null) {
                Result result = new Result(line);
                if (paper.checkIfAuthor(result.authorId) && result.score > 0.0d) {
                    authorCount++;
                }
                resultList.add(result);
            }

            br.close();
            int threshold = decideThreshold(paper, halThr);
            if (authorCount < threshold) {
                resultList.clear();
            }
        } catch (Exception e) {
            System.err.println("Exception @ readScoreFile");
            e.printStackTrace();
        }
        return new Pair<>(paper, resultList);
    }

    public static int calcHal(int count, int threshold) {
        return count >= threshold ? 1 : 0;
    }

    private static String evaluate(List<Result> resultList, int[] topMs, int threshold, Paper paper) {
        Collections.sort(resultList);
        int trueAuthorSize = paper.getAuthorSize();
        int resultSize = resultList.size();
        int authorSizeX = 0;
        int[] authorSizeMs = MiscUtil.initIntArray(topMs.length, 0);
        int bestRanking = INVALID_RANKING;
        for (int i = 0; i < resultSize; i++) {
            Result result = resultList.get(i);
            if (paper.checkIfAuthor(result.authorId)) {
                if (result.score > 0.0d) {
                    if (bestRanking == INVALID_RANKING) {
                        bestRanking = i + 1;
                    }

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

            if (bestRanking > 0 && i >= topMs[topMs.length - 1]) {
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        int overThrAtX = calcHal(authorSizeX, threshold);
        double coverageAtX = (double) authorSizeX / (double) trueAuthorSize;
        sb.append(paper.id + Config.FIRST_DELIMITER + String.valueOf(trueAuthorSize) + Config.FIRST_DELIMITER
                + String.valueOf(bestRanking) + Config.FIRST_DELIMITER
                + String.valueOf(authorSizeX) + Config.FIRST_DELIMITER
                + String.valueOf(overThrAtX) + Config.FIRST_DELIMITER + String.valueOf(coverageAtX));
        for (int i = 0; i < authorSizeMs.length; i++) {
            int overThrAtM = calcHal(authorSizeMs[i], threshold);
            double coverageAtM = (double) authorSizeMs[i] / (double) trueAuthorSize;
            sb.append(Config.FIRST_DELIMITER + Config.FIRST_DELIMITER + String.valueOf(authorSizeMs[i])
                    + Config.FIRST_DELIMITER + String.valueOf(overThrAtM)
                    + Config.FIRST_DELIMITER + String.valueOf(coverageAtM));
        }
        return sb.toString();
    }

    private static List<String> extractElements(String line) {
        List<String> list = new ArrayList<>();
        String[] elements = line.split(Config.FIRST_DELIMITER);
        for (int i = 0; i < elements.length; i++) {
            if (elements[i].length() > 0) {
                list.add(elements[i]);
            }
        }

        list.remove(0);
        list.remove(1);
        return list;
    }

    private static List<String> createFooter(String halThrStr, int[] topMs, int trueAuthorCount, int authorX, int overThrAtX,
                                       double coverageAtX, int[] authorMs, int[] overThrAtMs, double[] coverageAtMs,
                                       int blindPaperSize, int guessablePaperSize) {
        List<String> subOutputLineList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append(Config.FIRST_DELIMITER + "true author count" + Config.FIRST_DELIMITER
                + "hit author count @ X" + Config.FIRST_DELIMITER + "HAL" + halThrStr + "@X"
                + Config.FIRST_DELIMITER + "coverage@X" + Config.FIRST_DELIMITER);
        for (int i = 0; i < topMs.length; i++) {
            sb.append(Config.FIRST_DELIMITER + "author hit count @ " + String.valueOf(topMs[i])
                    + Config.FIRST_DELIMITER + "HAL" + halThrStr + "@" + String.valueOf(topMs[i])
                    + Config.FIRST_DELIMITER + "coverage@" + String.valueOf(topMs[i]) + Config.FIRST_DELIMITER);
        }

        subOutputLineList.add(sb.toString());
        sb = new StringBuilder();
        sb.append("total" + Config.FIRST_DELIMITER + String.valueOf(trueAuthorCount) + Config.FIRST_DELIMITER
                + String.valueOf(authorX) + Config.FIRST_DELIMITER + String.valueOf(overThrAtX)
                + Config.FIRST_DELIMITER + String.valueOf(coverageAtX) + Config.FIRST_DELIMITER);
        for (int i = 0; i < authorMs.length; i++) {
            sb.append(Config.FIRST_DELIMITER + String.valueOf(authorMs[i])
                    + Config.FIRST_DELIMITER + String.valueOf(overThrAtMs[i]) + Config.FIRST_DELIMITER
                    + String.valueOf(coverageAtMs[i]) + Config.FIRST_DELIMITER);
        }

        subOutputLineList.add(sb.toString());
        sb = new StringBuilder();
        double bps = (double) blindPaperSize;
        sb.append("avg" + Config.FIRST_DELIMITER + String.valueOf((double) trueAuthorCount / bps)
                + Config.FIRST_DELIMITER + String.valueOf((double) authorX / bps)
                + Config.FIRST_DELIMITER + String.valueOf((double) overThrAtX / bps)
                + Config.FIRST_DELIMITER + String.valueOf(coverageAtX / bps) + Config.FIRST_DELIMITER);
        for (int i = 0; i < authorMs.length; i++) {
            sb.append(Config.FIRST_DELIMITER + String.valueOf((double) authorMs[i] / bps)
                    + Config.FIRST_DELIMITER + String.valueOf((double) overThrAtMs[i] / bps)
                    + Config.FIRST_DELIMITER + String.valueOf(coverageAtMs[i] / bps) + Config.FIRST_DELIMITER);
        }

        subOutputLineList.add(sb.toString());
        sb = new StringBuilder();
        sb.append("metrics" + Config.FIRST_DELIMITER + Config.FIRST_DELIMITER + Config.FIRST_DELIMITER
                + String.valueOf((double) overThrAtX / bps * 100.0d) + Config.FIRST_DELIMITER
                + String.valueOf(coverageAtX / bps * 100.0d) + Config.FIRST_DELIMITER);
        for (int i = 0; i < authorMs.length; i++) {
            sb.append(Config.FIRST_DELIMITER + Config.FIRST_DELIMITER
                    + String.valueOf((double) overThrAtMs[i] / bps * 100.0d) + Config.FIRST_DELIMITER
                    + String.valueOf(coverageAtMs[i] / bps * 100.0d) + Config.FIRST_DELIMITER);
        }

        subOutputLineList.add(sb.toString());
        subOutputLineList.add("total number of blind papers" + Config.FIRST_DELIMITER + String.valueOf(blindPaperSize));
        double guessablePct = (double) guessablePaperSize / (double) blindPaperSize * 100.0d;
        subOutputLineList.add("percentage of guessable test papers" + Config.FIRST_DELIMITER + String.valueOf(guessablePct));
        return subOutputLineList;
    }

    private static void evaluate(String inputDirPath, String topMsStr, int halThr,
                                 String uplOutputFilePath, String outputFilePath) {
        try {
            List<String> outputLineList = new ArrayList<>();
            String halThrStr = halThr != HALX_LABEL ? String.valueOf(halThr) : "X";
            int[] topMs = MiscUtil.convertToIntArray(topMsStr, Config.OPTION_DELIMITER);
            FileUtil.makeParentDir(outputFilePath);
            outputLineList.add(createHeader(halThrStr, topMs));
            List<File> inputDirList = FileUtil.getDirList(inputDirPath);
            if (inputDirList.size() == 0) {
                inputDirList.add(new File(inputDirPath));
            }

            int blindPaperSize = 0;
            int guessablePaperSize = 0;
            int trueAuthorCount = 0;
            int authorX = 0;
            int overThrAtX = 0;
            double coverageAtX = 0.0d;
            int[] authorMs = MiscUtil.initIntArray(topMs.length, 0);
            int[] overThrAtMs = MiscUtil.initIntArray(topMs.length, 0);
            double[] coverageAtMs = MiscUtil.initDoubleArray(topMs.length, 0.0d);
            List<String> unguessablePaperIdList = new ArrayList<>();
            int dirSize = inputDirList.size();
            for (int i = 0; i < dirSize; i++) {
                File inputDir = inputDirList.remove(0);
                List<File> inputFileList = FileUtil.getFileListR(inputDir.getPath());
                int fileSize = inputFileList.size();
                blindPaperSize += fileSize;
                for (int j = 0; j < fileSize; j++) {
                    File inputFile = inputFileList.remove(0);
                    Pair<Paper, List<Result>> resultPair = readScoreFile(inputFile, halThr);
                    Paper paper = resultPair.first;
                    List<Result> resultList = resultPair.second;
                    int threshold = decideThreshold(paper, halThr);
                    if (paper.getAuthorSize() < threshold || resultList.size() == 0) {
                        if (paper.getAuthorSize() < threshold) {
                            blindPaperSize--;
                        }

                        if (resultList.size() == 0) {
                            trueAuthorCount += paper.getAuthorSize();
                            unguessablePaperIdList.add(paper.id);
                        }
                        continue;
                    }

                    String outputLine = evaluate(resultList, topMs, threshold, paper);
                    outputLineList.add(outputLine);
                    List<String> elementList = extractElements(outputLine);
                    trueAuthorCount += Integer.parseInt(elementList.remove(0));
                    authorX += Integer.parseInt(elementList.remove(0));
                    overThrAtX += Integer.parseInt(elementList.remove(0));
                    coverageAtX += Double.parseDouble(elementList.remove(0));
                    guessablePaperSize++;
                    int k = 0;
                    while (elementList.size() > 0) {
                        authorMs[k] += Integer.parseInt(elementList.remove(0));
                        overThrAtMs[k] += Integer.parseInt(elementList.remove(0));
                        coverageAtMs[k] += Double.parseDouble(elementList.remove(0));
                        k++;
                    }
                }
            }

            outputLineList.add("");
            outputLineList.addAll(createFooter(halThrStr, topMs, trueAuthorCount, authorX, overThrAtX, coverageAtX,
                    authorMs, overThrAtMs, coverageAtMs, blindPaperSize, guessablePaperSize));
            FileUtil.writeFile(outputLineList, outputFilePath);
            if (uplOutputFilePath != null) {
                FileUtil.writeFile(unguessablePaperIdList, uplOutputFilePath);
            }
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
        int halThr = cl.hasOption(HAL_OPTION) ? Integer.parseInt(cl.getOptionValue(HAL_OPTION)) : DEFAULT_HAL_THRESHOLD;
        halThr = cl.hasOption(HALX_OPTION) ? HALX_LABEL : halThr;
        String uplOutputFilePath = cl.hasOption(UNGUESSABLE_PAPER_LIST_OUTPUT_OPTION) ?
                cl.getOptionValue(UNGUESSABLE_PAPER_LIST_OUTPUT_OPTION) : null;
        String outputFilePath = cl.getOptionValue(Config.OUTPUT_FILE_OPTION);
        evaluate(inputDirPath, topMsStr, halThr, uplOutputFilePath, outputFilePath);
    }
}
