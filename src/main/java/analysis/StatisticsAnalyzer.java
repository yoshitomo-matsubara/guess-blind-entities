package analysis;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import structure.Author;
import structure.Paper;

import java.io.File;
import java.util.*;

public class StatisticsAnalyzer {
    private static final String AUTHOR_ID_DIR_OPTION = "a";

    private static Options setOptions() {
        Options options = new Options();
        options.addOption(Option.builder(AUTHOR_ID_DIR_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[input] author file")
                .build());
        options.addOption(Option.builder(Config.INPUT_DIR_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[input] author directory")
                .build());
        return options;
    }

    private static HashMap<String, String> createAuthorIdMap(String authorIdFilePath) {
        List<String> inputLineList = FileUtil.readFile(authorIdFilePath);
        HashMap<String, String> authorIdMap = new HashMap<>();
        for (String inputLine : inputLineList) {
            String[] elements = inputLine.split(Config.FIRST_DELIMITER);
            authorIdMap.put(elements[1], elements[0]);
        }
        return authorIdMap;
    }

    private static List<Author> getAuthorList(String authorIdFilePath, String inputDirPath) {
        HashMap<String, String> authorIdMap = createAuthorIdMap(authorIdFilePath);
        List<File> fileList = FileUtil.getFileList(inputDirPath);
        List<Author> authorList = new ArrayList<>();
        for (File file : fileList) {
            String authorId = file.getName();
            String authorName = authorIdMap.get(authorId);
            List<String> inputLineList = FileUtil.readFile(file);
            Author author = new Author(authorId, authorName, inputLineList);
            authorList.add(author);
        }
        return authorList;
    }

    private static void analyzeBasicMetrics(List<Author> authorList, HashSet<String> authorIdSet,
                                            HashSet<String> paperIdSet, HashSet<String> refPaperIdSet) {
        int dupPaperCount = 0;
        int dupRefPaperCount = 0;
        for (Author author : authorList) {
            authorIdSet.add(author.id);
            dupPaperCount += author.papers.length;
            for (Paper paper : author.papers) {
                paperIdSet.add(paper.id);
                dupRefPaperCount += paper.refPaperIds.length;
                for (String refPaperId : paper.refPaperIds) {
                    refPaperIdSet.add(refPaperId);
                }
            }
        }

        System.out.println("# of unique author IDs:\t" + String.valueOf(authorIdSet.size()));
        System.out.println("# of unique paper IDs:\t" + String.valueOf(paperIdSet.size()));
        System.out.println("# of unique reference paper IDs:\t" + String.valueOf(refPaperIdSet.size()));
        System.out.println("# of paper IDs / an unique author ID:\t"
                + String.valueOf((double)dupPaperCount / (double)authorIdSet.size()));
        System.out.println("# of reference paper IDs / a paper ID:\t"
                + String.valueOf((double)dupRefPaperCount / (double)dupPaperCount));
        System.out.println("# of reference paper IDs / a unique author ID:\t"
                + String.valueOf((double)dupRefPaperCount / (double)authorIdSet.size()));
    }

    private static void analyzeAveMetrics(List<Author> authorList, HashSet<String> authorIdSet,
                                          HashSet<String> paperIdSet, HashSet<String> refPaperIdSet) {
        System.out.println("# of unique paper IDs / a unique author ID:\t"
                + String.valueOf((double)paperIdSet.size() / (double)authorIdSet.size()));
        System.out.println("# of unique reference paper IDs / a unique paper ID:\t"
                + String.valueOf((double)refPaperIdSet.size() / (double)paperIdSet.size()));
        System.out.println("# of unique reference paper IDs / a unique author ID:\t"
                + String.valueOf((double)refPaperIdSet.size() / (double)authorIdSet.size()));
        TreeMap<Integer, Integer> paperCountMap = new TreeMap<>();
        TreeMap<String, Integer> yearPaperCountMap = new TreeMap<>();
        HashSet<String> usedPaperIdSet = new HashSet<>();
        TreeMap<Integer, Integer> refCountMap = new TreeMap<>();
        for (Author author : authorList) {
            authorIdSet.add(author.id);
            if (!paperCountMap.containsKey(author.papers.length)) {
                paperCountMap.put(author.papers.length, 0);
            }

            paperCountMap.put(author.papers.length, paperCountMap.get(author.papers.length) + 1);
            for (Paper paper : author.papers) {
                paperIdSet.add(paper.id);
                if (!yearPaperCountMap.containsKey(paper.year)) {
                    yearPaperCountMap.put(paper.year, 0);
                }

                if (!usedPaperIdSet.contains(paper.id)) {
                    usedPaperIdSet.add(paper.id);
                    yearPaperCountMap.put(paper.year, yearPaperCountMap.get(paper.year) + 1);
                }

                for (String refPaperId : paper.refPaperIds) {
                    refPaperIdSet.add(refPaperId);
                }

                if (!refCountMap.containsKey(paper.refPaperIds.length)) {
                    refCountMap.put(paper.refPaperIds.length, 0);
                }
                refCountMap.put(paper.refPaperIds.length, refCountMap.get(paper.refPaperIds.length) + 1);
            }
        }

        System.out.println("Distribution of # of paper IDs / an author");
        for (int key : paperCountMap.keySet()) {
            System.out.println(String.valueOf(key) + " " + String.valueOf(paperCountMap.get(key)));
        }

        System.out.println("Distribution of # of unique paper IDs in each year");
        for (String key : yearPaperCountMap.keySet()) {
            System.out.println(String.valueOf(key) + " " + String.valueOf(yearPaperCountMap.get(key)));
        }

        System.out.println("Distribution of # of reference paper IDs / an author");
        for (int key : refCountMap.keySet()) {
            System.out.println(String.valueOf(key) + " " + String.valueOf(refCountMap.get(key)));
        }
    }

    private static void analyze(String authorIdFilePath, String inputDirPath) {
        HashSet<String> authorIdSet = new HashSet<>();
        HashSet<String> paperIdSet = new HashSet<>();
        HashSet<String> refPaperIdSet = new HashSet<>();
        List<Author> authorList = getAuthorList(authorIdFilePath, inputDirPath);
        analyzeBasicMetrics(authorList, authorIdSet, paperIdSet, refPaperIdSet);
        analyzeAveMetrics(authorList, authorIdSet, paperIdSet, refPaperIdSet);
    }

    public static void main(String[] args) {
        Options options = setOptions();
        CommandLine cl = MiscUtil.setParams("StatisticsAnalyzer", options, args);
        String authorIdFilePath = cl.getOptionValue(AUTHOR_ID_DIR_OPTION);
        String inputDirPath = cl.getOptionValue(Config.INPUT_DIR_OPTION);
        analyze(authorIdFilePath, inputDirPath);
    }
}
