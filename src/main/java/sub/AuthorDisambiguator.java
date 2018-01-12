package sub;

import analysis.AuthorDisambiguationHelper;
import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class AuthorDisambiguator {
    private static final String INPUT_TRAIN_DIR_OPTION = "itrain";
    private static final String INPUT_TEST_DIR_OPTION = "itest";
    private static final String MATCHING_OPTION = "match";
    private static final String OUTPUT_TABLE_FILE_OPTION = "table";
    private static final String OUTPUT_TRAIN_DIR_OPTION = "otrain";
    private static final String OUTPUT_TEST_DIR_OPTION = "otest";
    private static final String NAME_DELIMITER = " ";
    private static final int FIRST_AUTHOR_ID_INDEX = 0;
    private static final int SECOND_AUTHOR_ID_INDEX = 1;
    private static final int MATCHING_LABEL_INDEX = 2;
    private static final int FIRST_AUTHOR_NAME_INDEX = 3;
    private static final int SECOND_AUTHOR_NAME_INDEX = 4;
    private static final int AUTHOR_LIST_INDEX = 3;
    public static final int COMPLETE_MATCH = AuthorDisambiguationHelper.COMPLETE_MATCH;
    public static final int ABBREVIATED_MATCH = AuthorDisambiguationHelper.ABBREVIATED_MATCH;


    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(Config.INPUT_FILE_OPTION, true, true, "[input] input file", options);
        MiscUtil.setOption(INPUT_TRAIN_DIR_OPTION, true, false, "[input, optional] input train dir", options);
        MiscUtil.setOption(INPUT_TEST_DIR_OPTION, true, false, "[input, optional] input test dir", options);
        MiscUtil.setOption(MATCHING_OPTION, true, true,
                "[param, optional] target matching labels (separated by comma(s))", options);
        MiscUtil.setOption(OUTPUT_TABLE_FILE_OPTION, true, false, "[output, optional] output file for disambiguation table", options);
        MiscUtil.setOption(OUTPUT_TRAIN_DIR_OPTION, true, false, "[output, optional] output train dir", options);
        MiscUtil.setOption(OUTPUT_TEST_DIR_OPTION, true, false, "[output, optional] output test dir", options);
        return options;
    }

    private static boolean checkIfTargetIsFullNameMap(int matchingLabel, String firstAuthorFirstName,
                                                      String secondAuthorFirstName, Set<Integer> targetLabelSet) {
        return matchingLabel == COMPLETE_MATCH && (targetLabelSet.size() == 1 ||
                (firstAuthorFirstName.length() > 1 && secondAuthorFirstName.length() > 1));
    }

    private static void initIfNotExist(String key, Map<String, Map<String, String>> mapOfMap) {
        if (!mapOfMap.containsKey(key)) {
            mapOfMap.put(key, new HashMap<>());
        }
    }

    private static void initIfNotExist(String firstKey, String secondKey,
                                       Map<String, Map<String, List<String>>> mapOfMap) {
        if (!mapOfMap.containsKey(firstKey)) {
            mapOfMap.put(firstKey, new HashMap<>());
        }
        if (!mapOfMap.get(firstKey).containsKey(secondKey)) {
            mapOfMap.get(firstKey).put(secondKey, new ArrayList<>());
        }
    }

    private static boolean checkIfTargetIsShortenNameMap(int matchingLabel, String firstAuthorFirstName,
                                                         String secondAuthorFirstName) {
        return matchingLabel == COMPLETE_MATCH || (matchingLabel == ABBREVIATED_MATCH
                && (firstAuthorFirstName.length() == 1 || secondAuthorFirstName.length() == 1));
    }

    private static Map<String, List<Pair<String, String>>> convertToCandidateListMap(
            Map<String, Map<String, String>> fullNameMap, Map<String, Map<String, List<String>>> shortenedNameMap) {
        List<String> shortenedNameList = new ArrayList<>(shortenedNameMap.keySet());
        for (String shortenedName : shortenedNameList) {
            Map<String, List<String>> authorIdListMap = shortenedNameMap.get(shortenedName);
            if (authorIdListMap.size() > 2) {
                shortenedNameMap.remove(shortenedName);
            }
        }

        for (String shortenedName : shortenedNameMap.keySet()) {
            if (!fullNameMap.containsKey(shortenedName)) {
                fullNameMap.put(shortenedName, new HashMap<>());
            }

            Map<String, String> nameMap = fullNameMap.get(shortenedName);
            Map<String, List<String>> authorIdListMap = shortenedNameMap.get(shortenedName);
            for (String authorName : authorIdListMap.keySet()) {
                // Remove overlapped disambiguation
                if (shortenedName.equals(authorName) && fullNameMap.containsKey(authorName)) {
                    fullNameMap.remove(authorName);
                }

                List<String> authorIdList = authorIdListMap.get(authorName);
                for (String authorId : authorIdList) {
                    nameMap.put(authorId, authorName);
                }
            }
        }

        Map<String, List<Pair<String, String>>> candidateListMap = new TreeMap<>();
        for (String authorFullName : fullNameMap.keySet()) {
            List<Pair<String, String>> pairList = new ArrayList<>();
            Map<String, String> authorNameMap = fullNameMap.get(authorFullName);
            for (String authorId : authorNameMap.keySet()) {
                pairList.add(new Pair<>(authorId, authorNameMap.get(authorId)));
            }
            candidateListMap.put(authorFullName, pairList);
        }
        return candidateListMap;
    }

    private static Map<String, List<Pair<String, String>>> buildCandidateListMap(String inputFilePath,
                                                                                 String matchingStr) {
        int[] matchingLabels = MiscUtil.convertToIntArray(matchingStr, Config.OPTION_DELIMITER);
        Set<Integer> targetLabelSet = new HashSet<>();
        for (int label : matchingLabels) {
            targetLabelSet.add(label);
        }

        Map<String, Map<String, String>> fullNameMap = new HashMap<>();
        Map<String, Map<String, List<String>>> shortenedNameMap = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(inputFilePath)));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(Config.FIRST_DELIMITER);
                int matchingLabel = Integer.parseInt(elements[MATCHING_LABEL_INDEX]);
                if (!targetLabelSet.contains(matchingLabel)) {
                    continue;
                }

                String firstAuthorId = elements[FIRST_AUTHOR_ID_INDEX];
                String secondAuthorId = elements[SECOND_AUTHOR_ID_INDEX];
                String firstAuthorName = elements[FIRST_AUTHOR_NAME_INDEX];
                String secondAuthorName = elements[SECOND_AUTHOR_NAME_INDEX];
                int firstAuthorNameIdx = firstAuthorName.indexOf(NAME_DELIMITER);
                int secondAuthorNameIdx = secondAuthorName.indexOf(NAME_DELIMITER);
                if (firstAuthorNameIdx < 0 || secondAuthorNameIdx < 0) {
                    continue;
                }

                String firstAuthorFirstName = firstAuthorName.substring(0, firstAuthorNameIdx);
                String secondAuthorFirstName = secondAuthorName.substring(0, secondAuthorNameIdx);
                if (checkIfTargetIsFullNameMap(matchingLabel, firstAuthorFirstName,
                        secondAuthorFirstName, targetLabelSet)) {
                    // firstAuthorName is identical with secondAuthorName
                    String key = firstAuthorName;
                    initIfNotExist(key, fullNameMap);
                    fullNameMap.get(key).put(firstAuthorId, firstAuthorName);
                    fullNameMap.get(key).put(secondAuthorId, secondAuthorName);
                } else if (checkIfTargetIsShortenNameMap(matchingLabel, firstAuthorFirstName, secondAuthorFirstName)) {
                    // firstAuthorName is NOT identical with secondAuthorName
                    String key = firstAuthorFirstName.length() == 1 ? firstAuthorName : secondAuthorName;
                    initIfNotExist(key, firstAuthorName, shortenedNameMap);
                    initIfNotExist(key, secondAuthorName, shortenedNameMap);
                    shortenedNameMap.get(key).get(firstAuthorName).add(firstAuthorId);
                    shortenedNameMap.get(key).get(secondAuthorName).add(secondAuthorId);
                }

            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ buildCandidateListMap");
            e.printStackTrace();
        }
        return convertToCandidateListMap(fullNameMap, shortenedNameMap);
    }

    private static void writeDisambiguationTableFile(Map<String, List<Pair<String, String>>> candidateListMap,
                                                     String outputTableFilePath) {
        List<String> outputLine = new ArrayList<>();
        for (String disambiguatedAuthorName : candidateListMap.keySet()) {
            List<Pair<String, String>> pairList = candidateListMap.get(disambiguatedAuthorName);
            List<String> authorIdList = new ArrayList<>();
            List<String> authorNameList = new ArrayList<>();
            for (Pair<String, String> pair : pairList) {
                authorIdList.add(pair.first);
                authorNameList.add(pair.second);
            }
            String line = disambiguatedAuthorName + Config.FIRST_DELIMITER + String.valueOf(pairList.size())
                    + Config.FIRST_DELIMITER + MiscUtil.convertListToString(authorIdList, Config.SECOND_DELIMITER)
                    + Config.FIRST_DELIMITER + MiscUtil.convertListToString(authorNameList, Config.OPTION_DELIMITER);
            outputLine.add(line);
        }
        FileUtil.writeFile(outputLine, outputTableFilePath);
    }

    private static Map<String, String> convertToDisambiguatedMap(
            Map<String, List<Pair<String, String>>> candidateListMap) {
        Map<String, String> disambiguatedMap = new HashMap<>();
        Random rand = new Random();
        for (String key : candidateListMap.keySet()) {
            List<Pair<String, String>> pairList = candidateListMap.get(key);
            Pair<String, String> representativePair = pairList.remove(rand.nextInt(pairList.size()));
            for (Pair<String, String> pair : pairList) {
                disambiguatedMap.put(pair.first, representativePair.first);
            }
        }
        return disambiguatedMap;
    }

    private static String disambiguateAuthorInPaper(String line, Map<String, String> disambiguatedMap) {
        String[] elements = line.split(Config.FIRST_DELIMITER);
        String[] authorIds = elements[AUTHOR_LIST_INDEX].split(Config.SECOND_DELIMITER);
        StringBuilder sb = new StringBuilder();
        for (String authorId : authorIds) {
            if (disambiguatedMap.containsKey(authorId)) {
                authorId = disambiguatedMap.get(authorId);
            }

            String str = sb.length() == 0 ? authorId : Config.SECOND_DELIMITER + authorId;
            sb.append(str);
        }

        elements[AUTHOR_LIST_INDEX] = sb.toString();
        return MiscUtil.convertListToString(Arrays.asList(elements), Config.FIRST_DELIMITER);
    }

    private static void disambiguateAuthorAndMergeFile(List<File> authorFileList, Map<String, String> disambiguatedMap,
                                                       String outputDirPath) {
        for (File authorFile : authorFileList) {
            List<String> outputLineList = new ArrayList<>();
            List<String> inputLineList = FileUtil.readFile(authorFile);
            for (String inputLine : inputLineList) {
                outputLineList.add(disambiguateAuthorInPaper(inputLine, disambiguatedMap));
            }
            FileUtil.writeFile(outputLineList, outputDirPath + FileUtil.getParentDirName(authorFile.getPath())
                    + "/" + authorFile.getName());
        }

        List<File> outputAuthorFileList = FileUtil.getFileList(FileUtil.getDirList(outputDirPath));
        Map<String, String> outputAuthorFilePathMap = new HashMap<>();
        for (File outputAuthorFile : outputAuthorFileList) {
            outputAuthorFilePathMap.put(outputAuthorFile.getName(), outputAuthorFile.getPath());
        }

        for (File outputAuthorFile : outputAuthorFileList) {
            String authorId = outputAuthorFile.getName();
            if (!disambiguatedMap.containsKey(authorId)) {
                continue;
            }

            List<String> outputLineList = new ArrayList<>();
            List<String> inputLineList = FileUtil.readFile(outputAuthorFile);
            for (String inputLine : inputLineList) {
                outputLineList.add(disambiguateAuthorInPaper(inputLine, disambiguatedMap));
            }

            String disambiguatedAuthorId = disambiguatedMap.get(authorId);
            String disambiguatedAuthorFilePath = outputAuthorFilePathMap.get(disambiguatedAuthorId);
            FileUtil.overwriteFile(outputLineList, false, disambiguatedAuthorFilePath);
            FileUtil.deleteFile(outputAuthorFile.getPath());
            System.out.println("Deleted and added " + authorId + " to " + disambiguatedAuthorId);
        }
    }

    private static void disambiguate(String inputDirPath, Map<String, String> disambiguatedMap, boolean isTraining,
                                     String outputDirPath) {
        List<File> paperFileList = FileUtil.getFileList(inputDirPath);
        for (File paperFile : paperFileList) {
            List<String> outputLineList = new ArrayList<>();
            List<String> inputLineList = FileUtil.readFile(paperFile);
            for (String inputLine : inputLineList) {
                outputLineList.add(disambiguateAuthorInPaper(inputLine, disambiguatedMap));
            }
            FileUtil.writeFile(outputLineList, outputDirPath + paperFile.getName());
        }

        if (isTraining) {
            List<File> authorFileList = FileUtil.getFileList(FileUtil.getDirList(inputDirPath));
            disambiguateAuthorAndMergeFile(authorFileList, disambiguatedMap, outputDirPath);
        }
    }

    private static void disambiguate(String inputFilePath, String inputTrainDirPath, String inputTestDirPath,
                                     String matchingStr, String outputTableFilePath,
                                     String outputTrainDirPath, String outputTestDirPath) {
        Map<String, List<Pair<String, String>>> candidateListMap = buildCandidateListMap(inputFilePath, matchingStr);
        if (outputTableFilePath != null) {
            writeDisambiguationTableFile(candidateListMap, outputTableFilePath);
        }

        Map<String, String> disambiguatedMap = convertToDisambiguatedMap(candidateListMap);
        if (inputTrainDirPath != null && outputTrainDirPath != null) {
            disambiguate(inputTrainDirPath, disambiguatedMap, true, outputTrainDirPath);
        }

        if (inputTestDirPath != null && outputTestDirPath != null) {
            disambiguate(inputTestDirPath, disambiguatedMap, false, outputTestDirPath);
        }
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("AuthorDisambiguator", options, args);
        String inputFilePath = cl.getOptionValue(Config.INPUT_FILE_OPTION);
        String inputTrainDirPath = cl.hasOption(INPUT_TRAIN_DIR_OPTION) ?
                cl.getOptionValue(INPUT_TRAIN_DIR_OPTION) : null;
        String inputTestDirPath = cl.hasOption(INPUT_TEST_DIR_OPTION) ?
                cl.getOptionValue(INPUT_TEST_DIR_OPTION) : null;
        String matchingStr = cl.hasOption(MATCHING_OPTION) ? cl.getOptionValue(MATCHING_OPTION) : null;
        String outputTableFilePath = cl.hasOption(OUTPUT_TABLE_FILE_OPTION) ?
                cl.getOptionValue(OUTPUT_TABLE_FILE_OPTION) : null;
        String outputTrainDirPath = cl.hasOption(OUTPUT_TRAIN_DIR_OPTION) ?
                cl.getOptionValue(OUTPUT_TRAIN_DIR_OPTION) : null;
        String outputTestDirPath = cl.hasOption(OUTPUT_TEST_DIR_OPTION) ?
                cl.getOptionValue(OUTPUT_TEST_DIR_OPTION) : null;
        disambiguate(inputFilePath, inputTrainDirPath, inputTestDirPath, matchingStr, outputTableFilePath,
                outputTrainDirPath, outputTestDirPath);
    }
}
