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

    private static int decideThreshold(Paper paper, int halThr) {
        return halThr == HALX_LABEL ? paper.getAuthorSize() : halThr;
    }

    private static Pair<Paper, List<Result>> readScoreFile(File file, int halThr) {
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

    private static int calcHal(int count, int threshold) {
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
        double recallAtX = (double) authorSizeX / (double) trueAuthorSize;
        sb.append(paper.id + Config.FIRST_DELIMITER + String.valueOf(trueAuthorSize) + Config.FIRST_DELIMITER
                + String.valueOf(bestRanking) + Config.FIRST_DELIMITER
                + String.valueOf(authorSizeX) + Config.FIRST_DELIMITER
                + String.valueOf(overThrAtX) + Config.FIRST_DELIMITER + String.valueOf(recallAtX));
        for (int i = 0; i < authorSizeMs.length; i++) {
            int overThrAtM = calcHal(authorSizeMs[i], threshold);
            double recallAtM = (double) authorSizeMs[i] / (double) topMs[i];
            sb.append(Config.FIRST_DELIMITER + Config.FIRST_DELIMITER + String.valueOf(authorSizeMs[i])
                    + Config.FIRST_DELIMITER + String.valueOf(overThrAtM)
                    + Config.FIRST_DELIMITER + String.valueOf(recallAtM));
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

    private static void evaluate(String inputDirPath, String topMsStr, int halThr,
                                 String uplOutputFilePath, String outputFilePath) {
        try {
            String halThrStr = halThr != HALX_LABEL ? String.valueOf(halThr) : "X";
            int[] topMs = convertToIntArray(topMsStr);
            FileUtil.makeParentDir(outputFilePath);
            File outputFile = new File(outputFilePath);
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
            bw.write("paper ID\ttrue author count\tbest ranking\tauthor hit count\tHAL" + halThrStr + "@X\tRecall@X\t");
            for (int i = 0; i < topMs.length; i++) {
                String mStr = String.valueOf(topMs[i]);
                bw.write("\tauthor hit count@" + mStr + "\tHAL" + halThrStr + "@" + mStr
                        + "\tRecall@" + mStr + "\t");
            }

            bw.newLine();
            List<File> inputDirList = FileUtil.getDirList(inputDirPath);
            if (inputDirList.size() == 0) {
                inputDirList.add(new File(inputDirPath));
            }

            int blindPaperSize = 0;
            int guessablePaperSize = 0;
            int trueAuthorCount = 0;
            int authorX = 0;
            int overThrAtX = 0;
            double recallAtX = 0.0d;
            int[] authorMs = MiscUtil.initIntArray(topMs.length, 0);
            int[] overThrAtMs = MiscUtil.initIntArray(topMs.length, 0);
            double[] recallAtMs = MiscUtil.initDoubleArray(topMs.length, 0.0d);
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
                    bw.write(outputLine);
                    bw.newLine();
                    List<String> elementList = extractElements(outputLine);
                    trueAuthorCount += Integer.parseInt(elementList.remove(0));
                    authorX += Integer.parseInt(elementList.remove(0));
                    overThrAtX += Integer.parseInt(elementList.remove(0));
                    recallAtX += Double.parseDouble(elementList.remove(0));
                    guessablePaperSize++;
                    int k = 0;
                    while (elementList.size() > 0) {
                        authorMs[k] += Integer.parseInt(elementList.remove(0));
                        overThrAtMs[k] += Integer.parseInt(elementList.remove(0));
                        recallAtMs[k] += Double.parseDouble(elementList.remove(0));
                        k++;
                    }
                }
            }

            bw.newLine();
            bw.write(Config.FIRST_DELIMITER + "true author count" + Config.FIRST_DELIMITER
                    + "hit author count @ X" + Config.FIRST_DELIMITER + "HAL" + halThrStr + "@X"
                    + Config.FIRST_DELIMITER + "Recall@X" + Config.FIRST_DELIMITER);
            for (int i = 0; i < topMs.length; i++) {
                bw.write(Config.FIRST_DELIMITER + "author hit count @ " + String.valueOf(topMs[i])
                        + Config.FIRST_DELIMITER + "HAL" + halThrStr + "@" + String.valueOf(topMs[i])
                        + Config.FIRST_DELIMITER + "Recall@" + String.valueOf(topMs[i]) + Config.FIRST_DELIMITER);
            }

            bw.newLine();
            bw.write("total" + Config.FIRST_DELIMITER + String.valueOf(trueAuthorCount) + Config.FIRST_DELIMITER
                    + String.valueOf(authorX) + Config.FIRST_DELIMITER + String.valueOf(overThrAtX)
                    + Config.FIRST_DELIMITER + String.valueOf(recallAtX) + Config.FIRST_DELIMITER);
            for (int i = 0; i < authorMs.length; i++) {
                bw.write(Config.FIRST_DELIMITER + String.valueOf(authorMs[i])
                        + Config.FIRST_DELIMITER + String.valueOf(overThrAtMs[i]) + Config.FIRST_DELIMITER
                        + String.valueOf(recallAtMs[i]) + Config.FIRST_DELIMITER);
            }

            bw.newLine();
            double bps = (double) blindPaperSize;
            bw.write("avg" + Config.FIRST_DELIMITER + String.valueOf((double) trueAuthorCount / bps)
                    + Config.FIRST_DELIMITER + String.valueOf((double) authorX / bps)
                    + Config.FIRST_DELIMITER + String.valueOf((double) overThrAtX / bps)
                    + Config.FIRST_DELIMITER + String.valueOf(recallAtX / bps) + Config.FIRST_DELIMITER);
            for (int i = 0; i < authorMs.length; i++) {
                bw.write(Config.FIRST_DELIMITER + String.valueOf((double) authorMs[i] / bps)
                        + Config.FIRST_DELIMITER + String.valueOf((double) overThrAtMs[i] / bps)
                        + Config.FIRST_DELIMITER + String.valueOf(recallAtMs[i] / bps) + Config.FIRST_DELIMITER);
            }

            bw.newLine();
            bw.write("metrics" + Config.FIRST_DELIMITER + Config.FIRST_DELIMITER + Config.FIRST_DELIMITER
                    + String.valueOf((double) overThrAtX / bps * 100.0d) + Config.FIRST_DELIMITER
                    + String.valueOf(recallAtX / bps * 100.0d) + Config.FIRST_DELIMITER);
            for (int i = 0; i < authorMs.length; i++) {
                bw.write(Config.FIRST_DELIMITER + Config.FIRST_DELIMITER
                        + String.valueOf((double) overThrAtMs[i] / bps * 100.0d) + Config.FIRST_DELIMITER
                        + String.valueOf(recallAtMs[i] / bps * 100.0d) + Config.FIRST_DELIMITER);
            }

            bw.newLine();
            bw.write("total number of blind papers" + Config.FIRST_DELIMITER + String.valueOf(blindPaperSize));
            bw.newLine();
            double guessablePct = (double) guessablePaperSize / (double) blindPaperSize * 100.0d;
            bw.write("percentage of guessable test papers" + Config.FIRST_DELIMITER + String.valueOf(guessablePct));
            bw.newLine();
            bw.close();
            if (uplOutputFilePath != null) {
                FileUtil.makeParentDir(uplOutputFilePath);
                File uplOutputFile = new File(uplOutputFilePath);
                bw = new BufferedWriter(new FileWriter(uplOutputFile));
                for (String unguessablePaperId : unguessablePaperIdList) {
                    bw.write(unguessablePaperId);
                    bw.newLine();
                }
                bw.close();
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
