package analysis;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Author;
import structure.Paper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class StatisticalAnalyzer {
    private static final String PAPERS_FILE_OPTION = "p";
    private static final String AFFILS_FILE_OPTION = "af";
    private static final String REFS_FILE_OPTION = "r";
    private static final String AUTHOR_DIR_OPTION = "au";
    private static final String TEST_DIROPTION = "test";
    private static final int PAPER_ID_INDEX = 0;
    private static final int AFFIL_ID_INDEX = 1;
    private static final int PAPER_REF_ID_INDEX = 1;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(PAPERS_FILE_OPTION, true, false, "[input, optional] min-Papers file", options);
        MiscUtil.setOption(AFFILS_FILE_OPTION, true, false,
                "[input, optional] min-PaperAuthorAffiliations file", options);
        MiscUtil.setOption(REFS_FILE_OPTION, true, false, "[input, optional] min-PaperReferences file", options);
        MiscUtil.setOption(TEST_DIROPTION, true, false, "[input, optional] test dir", options);
        MiscUtil.setOption(AUTHOR_DIR_OPTION, true, false, "[input, optional] author directory", options);
        return options;
    }

    private static void analyzeMinFile(String orgFilePath, int index, String title) {
        try {
            Set<String> idSet = new HashSet<>();
            File orgFile = new File(orgFilePath);
            BufferedReader br = new BufferedReader(new FileReader(orgFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(Config.FIRST_DELIMITER);
                if (!idSet.contains(elements[index])) {
                    idSet.add(elements[index]);
                }
            }

            br.close();
            System.out.println(title + ": \t" + String.valueOf(idSet.size()));
        } catch (Exception e) {
            System.err.println("Exception @ analyzeMinFile");
            e.printStackTrace();
        }
    }

    private static void analyzeMinListFile(String orgFilePath, int index, String title) {
        try {
            Set<String> idSet = new HashSet<>();
            File orgFile = new File(orgFilePath);
            BufferedReader br = new BufferedReader(new FileReader(orgFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(Config.FIRST_DELIMITER);
                String[] ids = elements[index].split(Config.SECOND_DELIMITER);
                for (String id : ids) {
                    if (!idSet.contains(id)) {
                        idSet.add(id);
                    }
                }
            }

            br.close();
            System.out.println(title + ": \t" + String.valueOf(idSet.size()));
        } catch (Exception e) {
            System.err.println("Exception @ analyzeMinListFile");
            e.printStackTrace();
        }
    }

    private static List<Author> getAuthorList(String authorDirPath) {
        List<File> fileList = FileUtil.getFileListR(authorDirPath);
        List<Author> authorList = new ArrayList<>();
        for (File file : fileList) {
            String authorId = file.getName();
            List<String> inputLineList = FileUtil.readFile(file);
            Author author = new Author(authorId, inputLineList);
            authorList.add(author);
        }
        return authorList;
    }

    private static void analyzeBasicMetrics(List<Author> authorList, Set<String> authorIdSet,
                                            Set<String> paperIdSet, Set<String> refPaperIdSet) {
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
        System.out.println("# of paper IDs (including overlapped):\t" + String.valueOf(dupPaperCount));
        System.out.println("# of reference paper IDs (including overlapped):\t" + String.valueOf(dupRefPaperCount));
    }

    private static void analyzeAveMetrics(List<Author> authorList, Set<String> authorIdSet,
                                          Set<String> paperIdSet, Set<String> refPaperIdSet) {
        System.out.println("# of unique paper IDs / a unique author ID:\t"
                + String.valueOf((double)paperIdSet.size() / (double)authorIdSet.size()));
        System.out.println("# of unique reference paper IDs / a unique paper ID:\t"
                + String.valueOf((double)refPaperIdSet.size() / (double)paperIdSet.size()));
        System.out.println("# of unique reference paper IDs / a unique author ID:\t"
                + String.valueOf((double)refPaperIdSet.size() / (double)authorIdSet.size()));
        Map<Integer, Integer> paperCountMap = new TreeMap<>();
        Map<String, Integer> yearPaperCountMap = new TreeMap<>();
        Set<String> usedPaperIdSet = new HashSet<>();
        Map<Integer, Integer> refCountMap = new TreeMap<>();
        for (Author author : authorList) {
            authorIdSet.add(author.id);
            paperCountMap.put(author.papers.length, paperCountMap.getOrDefault(author.papers.length, 0) + 1);
            for (Paper paper : author.papers) {
                paperIdSet.add(paper.id);
                if (!usedPaperIdSet.contains(paper.id)) {
                    usedPaperIdSet.add(paper.id);
                    yearPaperCountMap.put(paper.year, yearPaperCountMap.getOrDefault(paper.year, 0) + 1);
                }

                for (String refPaperId : paper.refPaperIds) {
                    refPaperIdSet.add(refPaperId);
                }
                refCountMap.put(paper.refPaperIds.length, refCountMap.getOrDefault(paper.refPaperIds.length, 0) + 1);
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

    private static void analyzeOverlappedAuthor(String testDirPath, Set<String> trainingAuthorIdSet) {
        List<File> testFileList = FileUtil.getFileList(testDirPath);
        Set<String> testAuthorIdSet = new HashSet<>();
        Set<String> overlappedIdSet = new HashSet<>();
        try {
            for (File testFile : testFileList) {
                BufferedReader br = new BufferedReader(new FileReader(testFile));
                String line;
                while ((line = br.readLine()) != null) {
                    Paper paper = new Paper(line);
                    for (String authorId : paper.getAuthorIdSet()) {
                        if (!testAuthorIdSet.contains(authorId)) {
                            testAuthorIdSet.add(authorId);
                        }

                        if (trainingAuthorIdSet.contains(authorId) && !overlappedIdSet.contains(authorId)) {
                            overlappedIdSet.add(authorId);
                        }
                    }
                }
                br.close();
            }

            double overlapped = (double) overlappedIdSet.size() / (double) testAuthorIdSet.size() * 100.0d;
            System.out.println("% of overlapped authors between training and test datasets: "
                    + String.valueOf(overlapped) + "%");
        } catch (Exception e) {
            System.err.println("Exception @ analyzeOverlappedAuthor");
            e.printStackTrace();
        }
    }

    private static void analyzeAuthors(String authorDirPath, String testDirPath) {
        Set<String> authorIdSet = new HashSet<>();
        Set<String> paperIdSet = new HashSet<>();
        Set<String> refPaperIdSet = new HashSet<>();
        List<Author> authorList = getAuthorList(authorDirPath);
        analyzeBasicMetrics(authorList, authorIdSet, paperIdSet, refPaperIdSet);
        analyzeAveMetrics(authorList, authorIdSet, paperIdSet, refPaperIdSet);
        if (testDirPath != null) {
            analyzeOverlappedAuthor(testDirPath, authorIdSet);
        }
    }

    private static void analyze(String papersFilePath, String affilsFilePath,
                                String refsFilePath, String authorDirPath, String testDirPath) {
        if (papersFilePath != null) {
            analyzeMinFile(papersFilePath, PAPER_ID_INDEX, "# of unique paper IDs in " + papersFilePath);
        }

        if (affilsFilePath != null) {
            analyzeMinListFile(affilsFilePath, AFFIL_ID_INDEX, "# of unique affilation IDs in " + affilsFilePath);
        }

        if (refsFilePath != null) {
            analyzeMinListFile(refsFilePath, PAPER_REF_ID_INDEX, "# of unique reference paper IDs in " + refsFilePath);
        }

        if (authorDirPath != null && testDirPath != null) {
            analyzeAuthors(authorDirPath, testDirPath);
        }
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("StatisticalAnalyzer", options, args);
        String papersFilePath = cl.hasOption(PAPERS_FILE_OPTION) ? cl.getOptionValue(PAPERS_FILE_OPTION) : null;
        String affilsFilePath = cl.hasOption(AFFILS_FILE_OPTION) ? cl.getOptionValue(AFFILS_FILE_OPTION) : null;
        String refsFilePath = cl.hasOption(REFS_FILE_OPTION) ? cl.getOptionValue(REFS_FILE_OPTION) : null;
        String authorDirPath = cl.hasOption(AUTHOR_DIR_OPTION) ? cl.getOptionValue(AUTHOR_DIR_OPTION) : null;
        String testDirPath = cl.hasOption(TEST_DIROPTION) ? cl.getOptionValue(TEST_DIROPTION) : null;
        analyze(papersFilePath, affilsFilePath, refsFilePath, authorDirPath, testDirPath);
    }
}
