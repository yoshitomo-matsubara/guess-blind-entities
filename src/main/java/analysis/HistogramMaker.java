package analysis;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Paper;

import java.io.*;
import java.util.*;

public class HistogramMaker {
    private static final String PAPER_FILE_OPTION = "p";
    private static final String START_YEAR_OPTION = "s";
    private static final String END_YEAR_OPTION = "e";
    private static final String AUTHOR_DIR_OPTION = "a";
    private static final String TMP_AUTHOR_HISTOGRAM_FILE_PREFIX = "tmp-a-";
    private static final String TMP_REF_AUTHOR_HISTOGRAM_FILE_PREFIX = "tmp-r-";
    private static final String COMPLETE_PREFIX = "comp-";
    private static final String PAPER_HIST_FILE_NAME = "paper-histogram.csv";
    private static final String AUTHOR_HIST_FILE_NAME = "author-histogram.csv";
    private static final String REF_AUTHOR_HIST_FILE_NAME = "refauthor-histogram.csv";
    private static final int INVALID_VALUE = -1;
    private static final int DEFAULT_ARRAY_SIZE = 5000;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(PAPER_FILE_OPTION, true, false, "[input, optional] paper file", options);
        MiscUtil.setOption(START_YEAR_OPTION, true, false,
                "[param, optional] start year, -" + PAPER_FILE_OPTION + " is required", options);
        MiscUtil.setOption(END_YEAR_OPTION, true, false,
                "[param, optional] end year, -" + PAPER_FILE_OPTION + " is required", options);
        MiscUtil.setOption(AUTHOR_DIR_OPTION, true, false, "[input, optional] author directory", options);
        MiscUtil.setOption(Config.TMP_DIR_OPTION, true, false, "[param, optional] temporary directory", options);
        MiscUtil.setOption(Config.OUTPUT_DIR_OPTION, true, true, "[output] output directory", options);
        return options;
    }

    private static boolean checkIfPaperMode(String paperFilePath, int startYear, int endYear) {
        return paperFilePath != null && startYear != INVALID_VALUE && endYear != INVALID_VALUE && startYear <= endYear;
    }

    private static boolean checkIfAuthorMode(String authorDirPath) {
        return authorDirPath != null;
    }

    private static void writeHistogramFile(Map<Integer, Integer> treeMap, String outputFilePath) {
        System.out.println("\tStart:\twriting " + outputFilePath);
        try {
            FileUtil.makeParentDir(outputFilePath);
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilePath)));
            for (int key : treeMap.keySet()) {
                bw.write(String.valueOf(key) + Config.FIRST_DELIMITER
                        + String.valueOf(treeMap.get(key)));
                bw.newLine();
            }
            bw.close();
        } catch (Exception e) {
            System.err.println("Exception @ writeHistogramFile");
            e.printStackTrace();
        }
        System.out.println("\tEnd:\twriting " + outputFilePath);
    }

    private static void writeHistogramFile(int[] array, Map<Integer, Integer> treeMap, String outputFilePath) {
        System.out.println("\tStart:\twriting " + outputFilePath);
        try {
            FileUtil.makeParentDir(outputFilePath);
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilePath)));
            for (int i = 0; i < array.length; i++) {
                if (array[i] > 0) {
                    bw.write(String.valueOf(i) + Config.FIRST_DELIMITER + String.valueOf(array[i]));
                    bw.newLine();
                }
            }

            for (int key : treeMap.keySet()) {
                bw.write(String.valueOf(key) + Config.FIRST_DELIMITER
                        + String.valueOf(treeMap.get(key)));
                bw.newLine();
            }
            bw.close();
        } catch (Exception e) {
            System.err.println("Exception @ writeHistogramFile");
            e.printStackTrace();
        }
        System.out.println("\tEnd:\twriting " + outputFilePath);
    }

    private static void makePaperHistogram(String paperFilePath, int startYear, int endYear, String outputDirPath) {
        System.out.println("Start:\treading " + paperFilePath);
        try {
            Map<Integer, Integer> refPaperCountMap = new TreeMap<>();
            BufferedReader br = new BufferedReader(new FileReader(new File(paperFilePath)));
            String line;
            while ((line = br.readLine()) != null) {
                Paper paper = new Paper(line);
                int year = Integer.parseInt(paper.year);
                if (startYear <= year && year <= endYear) {
                    int refPaperSize = paper.refPaperIds.length;
                    refPaperCountMap.put(refPaperSize, refPaperCountMap.getOrDefault(refPaperSize, 0) + 1);
                }
            }

            br.close();
            writeHistogramFile(refPaperCountMap, outputDirPath + PAPER_HIST_FILE_NAME);
        } catch (Exception e) {
            System.err.println("Exception @ makePaperHistogram");
            e.printStackTrace();
        }
        System.out.println("End:\treading " + paperFilePath);
    }

    private static boolean checkIfCompleted(String authorDirName, String tmpDirPath) {
        File tmpFileA = new File(tmpDirPath + COMPLETE_PREFIX + TMP_AUTHOR_HISTOGRAM_FILE_PREFIX + authorDirName);
        File tmpFileR = new File(tmpDirPath + COMPLETE_PREFIX + TMP_REF_AUTHOR_HISTOGRAM_FILE_PREFIX + authorDirName);
        return tmpFileA.exists() && tmpFileR.exists();
    }

    private static void mergeHistogramFiles(String prefix, List<String> suffixList, String outputFilePath) {
        try {
            int[] counts = MiscUtil.initIntArray(DEFAULT_ARRAY_SIZE, 0);
            Map<Integer, Integer> countMap = new TreeMap<>();
            for (String suffix : suffixList) {
                File file = new File(prefix + suffix);
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] elements = line.split(Config.FIRST_DELIMITER);
                    int key = Integer.parseInt(elements[0]);
                    int value = Integer.parseInt(elements[1]);
                    if (key < counts.length) {
                        counts[key] += value;
                    } else {
                        countMap.put(key, countMap.getOrDefault(key, 0) + value);
                    }
                }

                br.close();
                file.delete();
            }
            writeHistogramFile(counts, countMap, outputFilePath);
        } catch (Exception e) {
            System.err.println("Exception @ mergeHistogramFiles");
            e.printStackTrace();
        }
    }

    private static void makeAuthorHistogram(String authorDirPath, String tmpDirPath, String outputDirPath) {
        System.out.println("Start:\treading author files");
        try {
            String outputTmpDirPath = tmpDirPath == null ? outputDirPath : tmpDirPath;
            String compTmpPrefixA = outputTmpDirPath + COMPLETE_PREFIX + TMP_AUTHOR_HISTOGRAM_FILE_PREFIX;
            String compTmpPrefixR = outputTmpDirPath + COMPLETE_PREFIX + TMP_REF_AUTHOR_HISTOGRAM_FILE_PREFIX;
            List<File> authorDirList = FileUtil.getDirList(authorDirPath);
            if (authorDirList.size() == 0) {
                authorDirList.add(new File(authorDirPath));
            }

            List<String> suffixList = new ArrayList<>();
            int dirSize = authorDirList.size();
            for (int i = 0; i < dirSize; i++) {
                System.out.println("\tStage " + String.valueOf(i + 1) + " / " + String.valueOf(dirSize));
                File authorDir = authorDirList.remove(0);
                String authorDirName = authorDir.getName();
                suffixList.add(authorDirName);
                if (checkIfCompleted(authorDirName, outputTmpDirPath)) {
                    System.out.println("\t" + authorDirName + " has been already completed");
                    continue;
                }

                int[] refAuthorCounts = MiscUtil.initIntArray(DEFAULT_ARRAY_SIZE, 0);
                int[] authorCounts = MiscUtil.initIntArray(DEFAULT_ARRAY_SIZE, 0);
                Map<Integer, Integer> exRefAuthorCountMap = new TreeMap<>();
                Map<Integer, Integer> exAuthorCountMap = new TreeMap<>();
                List<File> authorFileList = FileUtil.getFileListR(authorDir.getPath());
                int size = authorFileList.size();
                for (int j = 0; j < size; j++) {
                    File authorFile = authorFileList.remove(0);
                    int totalCount = 0;
                    Set<String> refPaperIdSet = new HashSet<>();
                    BufferedReader br = new BufferedReader(new FileReader(authorFile));
                    String line;
                    while ((line = br.readLine()) != null) {
                        Paper paper = new Paper(line);
                        for (String refPaperId : paper.refPaperIds) {
                            refPaperIdSet.add(refPaperId);
                        }
                        totalCount++;
                    }

                    br.close();
                    if (totalCount < authorCounts.length) {
                        authorCounts[totalCount]++;
                    } else {
                        exAuthorCountMap.put(totalCount, exAuthorCountMap.getOrDefault(totalCount, 0) + 1);
                    }

                    int refPaperSize = refPaperIdSet.size();
                    if (refPaperSize < refAuthorCounts.length) {
                        refAuthorCounts[refPaperSize]++;
                    } else {
                        exRefAuthorCountMap.put(refPaperSize, exRefAuthorCountMap.getOrDefault(refPaperSize, 0) + 1);
                    }
                }

                writeHistogramFile(authorCounts, exAuthorCountMap, outputTmpDirPath
                        + TMP_AUTHOR_HISTOGRAM_FILE_PREFIX + authorDirName);
                writeHistogramFile(refAuthorCounts, exRefAuthorCountMap, outputTmpDirPath
                        + TMP_REF_AUTHOR_HISTOGRAM_FILE_PREFIX + authorDirName);
                File tmpFileA = new File(outputTmpDirPath + TMP_AUTHOR_HISTOGRAM_FILE_PREFIX + authorDirName);
                tmpFileA.renameTo(new File(compTmpPrefixA + authorDirName));
                File tmpFileR = new File(outputTmpDirPath + TMP_REF_AUTHOR_HISTOGRAM_FILE_PREFIX + authorDirName);
                tmpFileR.renameTo(new File(compTmpPrefixR + authorDirName));
            }

            mergeHistogramFiles(compTmpPrefixA, suffixList, outputDirPath + AUTHOR_HIST_FILE_NAME);
            mergeHistogramFiles(compTmpPrefixR, suffixList, outputDirPath + REF_AUTHOR_HIST_FILE_NAME);
        } catch (Exception e) {
            System.err.println("Exception @ makeAuthorHistogram");
            e.printStackTrace();
        }
        System.out.println("End:\treading author files");
    }

    private static void analyze(String paperFilePath, int startYear, int endYear,
                                String authorDirPath, String tmpDirPath, String outputDirPath) {
        FileUtil.makeDirIfNotExist(outputDirPath);
        if (checkIfPaperMode(paperFilePath, startYear, endYear)) {
            makePaperHistogram(paperFilePath, startYear, endYear, outputDirPath);
        }

        if (checkIfAuthorMode(authorDirPath)) {
            makeAuthorHistogram(authorDirPath, tmpDirPath, outputDirPath);
        }
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("HistogramMaker", options, args);
        String paperFilePath = cl.hasOption(PAPER_FILE_OPTION) ? cl.getOptionValue(PAPER_FILE_OPTION) : null;
        int startYear = cl.hasOption(START_YEAR_OPTION) ?
                Integer.parseInt(cl.getOptionValue(START_YEAR_OPTION)) : INVALID_VALUE;
        int endYear = cl.hasOption(END_YEAR_OPTION) ?
                Integer.parseInt(cl.getOptionValue(END_YEAR_OPTION)) : INVALID_VALUE;
        String authorDirPath = cl.hasOption(AUTHOR_DIR_OPTION) ? cl.getOptionValue(AUTHOR_DIR_OPTION) : null;
        String tmpDirPath = cl.hasOption(Config.TMP_DIR_OPTION) ? cl.getOptionValue(Config.TMP_DIR_OPTION) : null;
        String outputDirPath = cl.getOptionValue(Config.OUTPUT_DIR_OPTION);
        analyze(paperFilePath, startYear, endYear, authorDirPath, tmpDirPath, outputDirPath);
    }
}
