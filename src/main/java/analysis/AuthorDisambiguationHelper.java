package analysis;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Paper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class AuthorDisambiguationHelper {
    private static final String TRAIN_DIR_OPTION = "tr";
    private static final String THRESHOLD_OPTION = "thr";
    private static final String DELIMITER = " ";
    private static final int DEFAULT_THRESHOLD = 1;
    private static final int INCOMPLETE_MATCH = -1;
    public static final int COMPLETE_MATCH = 0;
    public static final int ABBREVIATED_MATCH = 1;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(Config.INPUT_FILE_OPTION, true, true, "[input] input file", options);
        MiscUtil.setOption(TRAIN_DIR_OPTION, true, true, "[input] train dir", options);
        MiscUtil.setOption(THRESHOLD_OPTION, true, false, "[param, optional] filtering threshold", options);
        MiscUtil.setOption(Config.OUTPUT_FILE_OPTION, true, true, "[output] output file", options);
        return options;
    }

    private static Map<String, Set<String>> buildFilteredCollabSetMap(String trainDirPath, int threshold) {
        if (threshold < 2) {
            return CollaborativeResultAnalyzer.buildCollabSetMap(trainDirPath);
        }

        System.out.println("Start: building filtered collaborator set map");
        Map<String, Map<String, Integer>> collabCountMap = new TreeMap<>();
        List<File> trainFileList = FileUtil.getFileList(trainDirPath);
        try {
            for (File trainFile : trainFileList) {
                BufferedReader br = new BufferedReader(new FileReader(trainFile));
                String line;
                while ((line = br.readLine()) != null) {
                    Paper paper = new Paper(line);
                    List<String> authorIdList = new ArrayList<>(paper.getAuthorIdSet());
                    for (String authorId : authorIdList) {
                        if (!collabCountMap.containsKey(authorId)) {
                            collabCountMap.put(authorId, new HashMap<>());
                        }
                    }

                    for (int i = 0; i < authorIdList.size(); i++) {
                        String authorIdA = authorIdList.get(i);
                        Map<String, Integer> mapA = collabCountMap.get(authorIdA);
                        for (int j = i + 1; j < authorIdList.size(); j++) {
                            String authorIdB = authorIdList.get(j);
                            mapA.put(authorIdB, mapA.getOrDefault(authorIdB, 0) + 1);
                            Map<String, Integer> mapB = collabCountMap.get(authorIdB);
                            mapB.put(authorIdA, mapB.getOrDefault(authorIdA, 0) + 1);
                        }
                    }
                }
                br.close();
            }
        } catch (Exception e) {
            System.err.println("Exception @ buildFilteredCollabSetMap");
            e.printStackTrace();
        }

        Map<String, Set<String>> filteredCollabSetMap = new TreeMap<>();
        for (String authorId : collabCountMap.keySet()) {
            Map<String, Integer> subMap = collabCountMap.get(authorId);
            Set<String> coauthorIdSet = new HashSet<>();
            for (String coauthorId : subMap.keySet()) {
                if (subMap.get(coauthorId) >= threshold) {
                    coauthorIdSet.add(coauthorId);
                }
            }

            if (coauthorIdSet.size() > 0) {
                filteredCollabSetMap.put(authorId, coauthorIdSet);
            }
        }

        System.out.println("End: building filtered collaborator set map");
        return filteredCollabSetMap;
    }

    private static Map<String, String> buildAuthorNameMap(String filePath, Map<String, Set<String>> collabSetMap) {
        System.out.println("Start: building author name map");
        Set<String> requiredIdSet = new HashSet<>();
        for (String authorId : collabSetMap.keySet()) {
            requiredIdSet.add(authorId);
            Set<String> collabIdSet = collabSetMap.get(authorId);
            if (collabIdSet != null && collabIdSet.size() > 0) {
                requiredIdSet.addAll(collabIdSet);
            }
        }

        Map<String, String> map = new TreeMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(Config.FIRST_DELIMITER);
                if (requiredIdSet.contains(elements[0])) {
                    map.put(elements[0], elements[1]);
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("End: building author name map");
        return map;
    }

    private static Set<String> buildTargetAuthorIdSet(String trainDirPath) {
        Set<String> targetAuthorIdSet = new HashSet<>();
        List<File> authorFileList = FileUtil.getFileList(FileUtil.getDirList(trainDirPath));
        for (File authorFile : authorFileList) {
            targetAuthorIdSet.add(authorFile.getName());
        }
        return targetAuthorIdSet;
    }

    private static boolean checkIfCompleteMatching(String authorName, String coauthorName) {
        return authorName.equals(coauthorName);
    }

    private static boolean checkIfAbbreviatedMatching(String authorName, String coauthorName) {
        int authorFirstIndex = authorName.indexOf(DELIMITER);
        int coauthorFirstIndex = coauthorName.indexOf(DELIMITER);
        if (authorFirstIndex < 0 || coauthorFirstIndex < 0) {
            return false;
        }

        String authorFirstName = authorName.substring(0, authorFirstIndex);
        String coauthorFirstName = coauthorName.substring(0, coauthorFirstIndex);
        if (authorFirstName.length() == 1 || coauthorFirstName.length() == 1) {
            return authorFirstName.charAt(0) == coauthorFirstName.charAt(0) &&
                    checkIfCompleteMatching(authorName.substring(authorFirstIndex + 1),
                            coauthorName.substring(coauthorFirstIndex + 1));
        }

        return false;
    }

    private static int rankSimilarity(String authorName, String coauthorName) {
        if (checkIfCompleteMatching(authorName, coauthorName)) {
            return COMPLETE_MATCH;
        } else if (checkIfAbbreviatedMatching(authorName, coauthorName)) {
            return ABBREVIATED_MATCH;
        }
        return INCOMPLETE_MATCH;
    }

    private static void detectSimilarAuthors(Map<String, String> authorNameMap, Map<String, Set<String>> collabSetMap,
                                             Set<String> targetAuthorIdSet, String outputFilePath) {
        List<String> outputLineList = new ArrayList<>();
        Set<String> doneIdPairSet = new HashSet<>();
        for (String authorId : targetAuthorIdSet) {
            if (!authorNameMap.containsKey(authorId) || !collabSetMap.containsKey(authorId)) {
                continue;
            }

            String authorName = authorNameMap.get(authorId);
            Set<String> firstLevelCollabSet = collabSetMap.get(authorId);
            Set<String> secondLevelMergedCollabSet = new HashSet<>();
            for (String firstLevelCoauthorId : firstLevelCollabSet) {
                for (String secondLevelCoauthorId : collabSetMap.get(firstLevelCoauthorId)) {
                    if (!firstLevelCollabSet.contains(secondLevelCoauthorId)
                            && !secondLevelCoauthorId.equals(authorId)
                            && targetAuthorIdSet.contains(secondLevelCoauthorId)) {
                        secondLevelMergedCollabSet.add(secondLevelCoauthorId);
                    }
                }
            }

            for (String secondLevelCoauthorId : secondLevelMergedCollabSet) {
                if (doneIdPairSet.contains(secondLevelCoauthorId + authorId)
                        || !authorNameMap.containsKey(secondLevelCoauthorId)) {
                    continue;
                }

                String secondLevelCoauthorName = authorNameMap.get(secondLevelCoauthorId);
                int rank = rankSimilarity(authorName, secondLevelCoauthorName);
                if (rank > INCOMPLETE_MATCH) {
                    outputLineList.add(authorId + Config.FIRST_DELIMITER + secondLevelCoauthorId
                            + Config.FIRST_DELIMITER + String.valueOf(rank) + Config.FIRST_DELIMITER + authorName
                            + Config.FIRST_DELIMITER + secondLevelCoauthorName);
                    doneIdPairSet.add(authorId + secondLevelCoauthorId);
                }
            }
        }
        FileUtil.writeFile(outputLineList, outputFilePath);
    }

    private static void analyze(String inputFilePath, String trainDirPath, int threshold, String outputFilePath) {
        Map<String, Set<String>> collabSetMap = buildFilteredCollabSetMap(trainDirPath, threshold);
        Map<String, String> authorNameMap = buildAuthorNameMap(inputFilePath, collabSetMap);
        Set<String> targetAuthorIdSet = buildTargetAuthorIdSet(trainDirPath);
        detectSimilarAuthors(authorNameMap, collabSetMap, targetAuthorIdSet, outputFilePath);
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("AuthorDisambiguationHelper", options, args);
        String inputFilePath = cl.getOptionValue(Config.INPUT_FILE_OPTION);
        String trainDirPath = cl.getOptionValue(TRAIN_DIR_OPTION);
        int threshold = cl.hasOption(THRESHOLD_OPTION) ?
                Integer.parseInt(cl.getOptionValue(THRESHOLD_OPTION)) : DEFAULT_THRESHOLD;
        String outputFilePath = cl.getOptionValue(Config.OUTPUT_FILE_OPTION);
        analyze(inputFilePath, trainDirPath, threshold, outputFilePath);
    }
}
