package analysis;

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
import java.util.TreeMap;

public class HistogramMaker {
    private static final String PAPER_FILE_OPTION = "p";
    private static final String START_YEAR_OPTION = "s";
    private static final String END_YEAR_OPTION = "e";
    private static final String AUTHOR_DIR_OPTION = "a";
    private static final String PAPER_HIST_FILE_NAME = "paper-histogram.csv";
    private static final String AUTHOR_HIST_FILE_NAME = "author-histogram.csv";
    private static final String REF_AUTHOR_HIST_FILE_NAME = "refauthor-histogram.csv";
    private static final int INVALID_VALUE = -1;
    private static final int DEFAULT_ARRAY_SIZE = 5000;

    private static Options setOptions() {
        Options options = new Options();
        options.addOption(Option.builder(PAPER_FILE_OPTION)
                .hasArg(true)
                .required(false)
                .desc("[input, optional] paper file")
                .build());
        options.addOption(Option.builder(START_YEAR_OPTION)
                .hasArg(true)
                .required(false)
                .desc("[param, optional] start year, -" + PAPER_FILE_OPTION + " is required")
                .build());
        options.addOption(Option.builder(END_YEAR_OPTION)
                .hasArg(true)
                .required(false)
                .desc("[param, optional] end year, -" + PAPER_FILE_OPTION + " is required")
                .build());
        options.addOption(Option.builder(AUTHOR_DIR_OPTION)
                .hasArg(true)
                .required(false)
                .desc("[input, optional] author directory")
                .build());
        options.addOption(Option.builder(Config.OUTPUT_DIR_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[output] output directory")
                .build());
        return options;
    }

    private static boolean checkIfPaperMode(String paperFilePath, int startYear, int endYear) {
        return paperFilePath != null && startYear != INVALID_VALUE && endYear != INVALID_VALUE && startYear <= endYear;
    }

    private static boolean checkIfAuthorMode(String authorDirPath) {
        return authorDirPath != null;
    }

    private static void writeHistogramFile(TreeMap<Integer, Integer> treeMap, String outputFilePath) {
        System.out.println("\tStart:\twriting " + outputFilePath);
        try {
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

    private static void writeHistogramFile(int[] array, TreeMap<Integer, Integer> treeMap, String outputFilePath) {
        System.out.println("\tStart:\twriting " + outputFilePath);
        try {
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
            TreeMap<Integer, Integer> refPaperCountMap = new TreeMap<>();
            BufferedReader br = new BufferedReader(new FileReader(new File(paperFilePath)));
            String line;
            while ((line = br.readLine()) != null) {
                Paper paper = new Paper(line);
                int year = Integer.parseInt(paper.year);
                if (startYear <= year && year <= endYear) {
                    int refPaperSize = paper.refPaperIds.length;
                    if (!refPaperCountMap.containsKey(refPaperSize)) {
                        refPaperCountMap.put(refPaperSize, 1);
                    } else {
                        refPaperCountMap.put(refPaperSize, refPaperCountMap.get(refPaperSize) + 1);
                    }
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

    private static void makeAuthorHistogram(String authorDirPath, String outputDirPath) {
        System.out.println("Start:\treading author files");
        try {
            int[] refAuthorCounts = MiscUtil.initIntArray(DEFAULT_ARRAY_SIZE, 0);
            int[] authorCounts = MiscUtil.initIntArray(DEFAULT_ARRAY_SIZE, 0);
            TreeMap<Integer, Integer> exRefAuthorCountMap = new TreeMap<>();
            TreeMap<Integer, Integer> exAuthorCountMap = new TreeMap<>();
            List<File> authorDirList = FileUtil.getDirList(authorDirPath);
            if (authorDirList.size() == 0) {
                authorDirList.add(new File(authorDirPath));
            }

            int dirSize = authorDirList.size();
            for (int i = 0; i < dirSize; i++) {
                System.out.println("\tStage " + String.valueOf(i + 1) + " / " + String.valueOf(dirSize));
                File authorDir = authorDirList.remove(0);
                List<File> authorFileList = FileUtil.getFileListR(authorDir.getPath());
                int size = authorFileList.size();
                for (int j = 0; j < size; j++) {
                    File authorFile = authorFileList.remove(0);
                    int totalCount = 0;
                    HashSet<String> refPaperIdSet = new HashSet<>();
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
                    } else if (!exAuthorCountMap.containsKey(totalCount)) {
                        exAuthorCountMap.put(totalCount, 1);
                    } else {
                        exAuthorCountMap.put(totalCount, exAuthorCountMap.get(totalCount) + 1);
                    }

                    int refPaperSize = refPaperIdSet.size();
                    if (refPaperSize < refAuthorCounts.length) {
                        refAuthorCounts[refPaperSize]++;
                    } else if (!exRefAuthorCountMap.containsKey(refPaperSize)) {
                        exRefAuthorCountMap.put(refPaperSize, 1);
                    } else {
                        exRefAuthorCountMap.put(refPaperSize, exRefAuthorCountMap.get(refPaperSize) + 1);
                    }
                }
            }

            writeHistogramFile(refAuthorCounts, exRefAuthorCountMap, outputDirPath + REF_AUTHOR_HIST_FILE_NAME);
            writeHistogramFile(authorCounts, exAuthorCountMap, outputDirPath + AUTHOR_HIST_FILE_NAME);
        } catch (Exception e) {
            System.err.println("Exception @ makeAuthorHistogram");
            e.printStackTrace();
        }
        System.out.println("End:\treading author files");
    }

    private static void analyze(String paperFilePath, int startYear, int endYear,
                                String authorDirPath, String outputDirPath) {
        FileUtil.makeIfNotExist(outputDirPath);
        if (checkIfPaperMode(paperFilePath, startYear, endYear)) {
            makePaperHistogram(paperFilePath, startYear, endYear, outputDirPath);
        }

        if (checkIfAuthorMode(authorDirPath)) {
            makeAuthorHistogram(authorDirPath, outputDirPath);
        }
    }

    public static void main(String[] args) {
        Options options = setOptions();
        CommandLine cl = MiscUtil.setParams("StatisticsAnalyzer", options, args);
        String paperFilePath = cl.hasOption(PAPER_FILE_OPTION) ? cl.getOptionValue(PAPER_FILE_OPTION) : null;
        int startYear = cl.hasOption(START_YEAR_OPTION) ?
                Integer.parseInt(cl.getOptionValue(START_YEAR_OPTION)) : INVALID_VALUE;
        int endYear = cl.hasOption(END_YEAR_OPTION) ?
                Integer.parseInt(cl.getOptionValue(END_YEAR_OPTION)) : INVALID_VALUE;
        String authorDirPath = cl.hasOption(AUTHOR_DIR_OPTION) ? cl.getOptionValue(AUTHOR_DIR_OPTION) : null;
        String outputDirPath = cl.getOptionValue(Config.OUTPUT_DIR_OPTION);
        analyze(paperFilePath, startYear, endYear, authorDirPath, outputDirPath);
    }
}
