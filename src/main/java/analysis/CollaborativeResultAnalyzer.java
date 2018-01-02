package analysis;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import main.Evaluator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Pair;
import structure.Paper;
import structure.Result;

import java.io.*;
import java.util.*;

public class CollaborativeResultAnalyzer {
    private static final String TRAIN_DIR_OPTION = "tr";
    private static final String HAL_OPTION = "hal";
    private static final String HALX_OPTION = "halx";
    private static final String ALL_FILE_NAME = "all.csv";
    private static final String SUMMARY_FILE_NAME = "summary.csv";
    private static final int DEFAULT_HAL_THRESHOLD = 1;
    private static final int HALX_LABEL = -1;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(Config.INPUT_DIR_OPTION, true, true, "[input] input dir", options);
        MiscUtil.setOption(TRAIN_DIR_OPTION, true, true, "[input] train dir", options);
        MiscUtil.setOption(HAL_OPTION, true, false,
                "[param, optional] HAL (Hit At Least) threshold (default = " + String.valueOf(DEFAULT_HAL_THRESHOLD)
                        + ")", options);
        MiscUtil.setOption(HALX_OPTION, false, false,
                "[param, optional] HAL (Hit At Least) threshold = # of true authors in each paper", options);
        MiscUtil.setOption(Config.OUTPUT_DIR_OPTION, true, true, "[output] output dir", options);
        return options;
    }

    private static Map<String, Set<String>> buildCollabSetMap(String trainDirPath) {
        System.out.println("Start: building collaborator set map");
        Map<String, Set<String>> collabSetMap = new TreeMap<>();
        List<File> trainFileList = FileUtil.getFileList(trainDirPath);
        try {
            for (File trainFile : trainFileList) {
                BufferedReader br = new BufferedReader(new FileReader(trainFile));
                String line;
                while ((line = br.readLine()) != null) {
                    Paper paper = new Paper(line);
                    List<String> authorIdList = new ArrayList<>(paper.getAuthorIdSet());
                    for (String authorId : authorIdList) {
                        if (!collabSetMap.containsKey(authorId)) {
                            collabSetMap.put(authorId, new HashSet<>());
                        }
                    }

                    for (int i = 0; i < authorIdList.size(); i++) {
                        String authorIdA = authorIdList.get(i);
                        for (int j = i + 1; j < authorIdList.size(); j++) {
                            String authorIdB = authorIdList.get(j);
                            collabSetMap.get(authorIdA).add(authorIdB);
                            collabSetMap.get(authorIdB).add(authorIdA);
                        }
                    }
                }
                br.close();
            }
        } catch (Exception e) {
            System.err.println("Exception @ buildCollabSetMap");
            e.printStackTrace();
        }

        System.out.println("End: building collaborator set map");
        return collabSetMap;
    }

    private static int decideThreshold(Paper paper, int halThr) {
        return halThr == HALX_LABEL ? paper.getAuthorSize() : halThr;
    }

    private static void putAndInitListIfNotExist(String key, int value, Map<String, List<Integer>> map) {
        if (!map.containsKey(key)) {
            map.put(key, new ArrayList<>());
        }
        map.get(key).add(value);
    }

    private static String evaluate(List<Result> resultList, Paper paper, Map<String, Set<String>> collabSetMap,
                                   Map<String, List<Integer>> trueEntityRankListMap,
                                   Map<String, List<Integer>> collabRankListMap) {
        Set<String> mergedCollabSet = new HashSet<>();
        for (String entityId : paper.getAuthorIdSet()) {
            Set<String> collabSet = collabSetMap.get(entityId);
            if (collabSet != null && collabSet.size() > 0) {
                mergedCollabSet.addAll(collabSet);
            }
        }

        List<Integer> trueEntityRankList = new ArrayList<>();
        List<Integer> collabRankList = new ArrayList<>();
        List<String> trueEntityIdList = new ArrayList<>();
        List<String> collabIdList = new ArrayList<>();
        Collections.sort(resultList);
        int trueAuthorSize = paper.getAuthorSize();
        int resultSize = resultList.size();
        int maxCount = trueAuthorSize + mergedCollabSet.size();
        for (int i = 0; i < resultSize; i++) {
            Result result = resultList.get(i);
            int rank = i + 1;
            if (paper.checkIfAuthor(result.authorId) && result.score > 0.0d) {
                trueEntityRankList.add(rank);
                trueEntityIdList.add(result.authorId);
                putAndInitListIfNotExist(result.authorId, rank, trueEntityRankListMap);
            } else if (!paper.checkIfAuthor(result.authorId) && mergedCollabSet.contains(result.authorId)
                    && result.score > 0.0d) {
                collabRankList.add(rank);
                collabIdList.add(result.authorId);
                putAndInitListIfNotExist(result.authorId, rank, collabRankListMap);
            }

            if (trueEntityIdList.size() + collabIdList.size() >= maxCount) {
                break;
            }
        }

        String trueEntityRankStr = MiscUtil.convertListToString(trueEntityRankList, Config.OPTION_DELIMITER);
        String collabRankStr = MiscUtil.convertListToString(collabRankList, Config.OPTION_DELIMITER);
        String trueEntityIdStr = MiscUtil.convertListToString(trueEntityIdList, Config.OPTION_DELIMITER);
        String collabIdStr = MiscUtil.convertListToString(collabIdList, Config.OPTION_DELIMITER);
        return paper.id + Config.FIRST_DELIMITER + trueEntityRankStr + Config.FIRST_DELIMITER + collabRankStr
                + Config.FIRST_DELIMITER + Config.FIRST_DELIMITER + trueEntityIdStr
                + Config.FIRST_DELIMITER + collabIdStr;
    }

    private static double calcAverage(List<Integer> valueList) {
        double sum = 0.0d;
        if (valueList.size() == 0) {
            return 0.0d;
        }
        for (int value : valueList) {
            sum += (double) value;
        }
        return sum / (double) valueList.size();
    }

    private static void writeSummaryFile(String header, Map<String, List<Integer>> trueEntityRankListMap,
                                         Map<String, List<Integer>> collabRankListMap, String outputFilePath) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilePath)));
            bw.write(header);
            bw.newLine();
            bw.write("Entity ID" + Config.FIRST_DELIMITER + "Average Rank as Identified Entity"
                    + Config.FIRST_DELIMITER + "Average Rank as Past Collaborator" + Config.FIRST_DELIMITER
                    + Config.FIRST_DELIMITER + "Ranks as Identified Entity"
                    + Config.FIRST_DELIMITER + "Ranks as Past Collaborator");
            bw.newLine();
            Set<String> mergedEntityIdSet = new HashSet<>();
            mergedEntityIdSet.addAll(trueEntityRankListMap.keySet());
            mergedEntityIdSet.addAll(collabRankListMap.keySet());
            for (String entityId : mergedEntityIdSet) {
                List<Integer> trueEntityRankList =  trueEntityRankListMap.getOrDefault(entityId, new ArrayList<>());
                List<Integer> collabRankList =  collabRankListMap.getOrDefault(entityId, new ArrayList<>());
                double avgIdentifiedRank = calcAverage(trueEntityRankList);
                double avgCollabRank = calcAverage(collabRankList);
                String identifiedRankStr = MiscUtil.convertListToString(trueEntityRankList, Config.OPTION_DELIMITER);
                String collabRankStr = MiscUtil.convertListToString(collabRankList, Config.OPTION_DELIMITER);
                bw.write(entityId + Config.FIRST_DELIMITER + String.valueOf(avgIdentifiedRank)
                        + Config.FIRST_DELIMITER + String.valueOf(avgCollabRank) + Config.FIRST_DELIMITER
                        + Config.FIRST_DELIMITER + identifiedRankStr + Config.FIRST_DELIMITER + collabRankStr);
                bw.newLine();
            }
            bw.close();
        } catch (Exception e) {
            System.err.println("Exception @ writeSummaryFile");
            e.printStackTrace();
        }
    }

    private static void analyze(String inputDirPath, String trainDirPath, int halThr, String outputDirPath) {

        try {
            List<File> inputDirList = FileUtil.getDirList(inputDirPath);
            Map<String, Set<String>> collabSetMap = buildCollabSetMap(trainDirPath);
            Map<String, List<Integer>> trueEntityRankListMap = new TreeMap<>();
            Map<String, List<Integer>> collabRankListMap = new TreeMap<>();
            if (inputDirList.size() == 0) {
                inputDirList.add(new File(inputDirPath));
            }

            List<String> outputLineList = new ArrayList<>();
            int blindPaperSize = 0;
            int guessablePaperSize = 0;
            int dirSize = inputDirList.size();
            for (int i = 0; i < dirSize; i++) {
                File inputDir = inputDirList.remove(0);
                List<File> inputFileList = FileUtil.getFileListR(inputDir.getPath());
                int fileSize = inputFileList.size();
                blindPaperSize += fileSize;
                for (int j = 0; j < fileSize; j++) {
                    File inputFile = inputFileList.remove(0);
                    Pair<Paper, List<Result>> resultPair = Evaluator.readScoreFile(inputFile, halThr);
                    Paper paper = resultPair.first;
                    List<Result> resultList = resultPair.second;
                    int threshold = decideThreshold(paper, halThr);
                    if (paper.getAuthorSize() < threshold || resultList.size() == 0) {
                        if (paper.getAuthorSize() < threshold) {
                            blindPaperSize--;
                        }
                        continue;
                    }

                    String outputLine =
                            evaluate(resultList, paper, collabSetMap, trueEntityRankListMap, collabRankListMap);
                    outputLineList.add(outputLine);
                    guessablePaperSize++;
                }
            }

            String header = "Blind Paper Count" + Config.FIRST_DELIMITER + String.valueOf(blindPaperSize)
                    + Config.FIRST_DELIMITER + "Guessable Paper Count" + Config.FIRST_DELIMITER
                    + String.valueOf(guessablePaperSize);
            outputLineList.add(0, header);
            outputLineList.add(1, "Entity ID" + Config.FIRST_DELIMITER + "Identified Entities' Ranks"
                    + Config.FIRST_DELIMITER + "Past Collaborators' Ranks" + Config.FIRST_DELIMITER
                    + Config.FIRST_DELIMITER + "Identified Entities' IDs"
                    + Config.FIRST_DELIMITER + "Past Collaborators' IDs");
            FileUtil.writeFile(outputLineList, outputDirPath + "/" + ALL_FILE_NAME);
            writeSummaryFile(header, trueEntityRankListMap, collabRankListMap, outputDirPath + "/" + SUMMARY_FILE_NAME);
        } catch (Exception e) {
            System.err.println("Exception @ analyze");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("CollaborativeResultAnalyzer", options, args);
        String inputDirPath = cl.getOptionValue(Config.INPUT_DIR_OPTION);
        String trainDirPath = cl.getOptionValue(TRAIN_DIR_OPTION);
        int halThr = cl.hasOption(HAL_OPTION) ? Integer.parseInt(cl.getOptionValue(HAL_OPTION)) : DEFAULT_HAL_THRESHOLD;
        halThr = cl.hasOption(HALX_OPTION) ? HALX_LABEL : halThr;
        String outputDirPath = cl.getOptionValue(Config.OUTPUT_DIR_OPTION);
        analyze(inputDirPath, trainDirPath, halThr, outputDirPath);
    }
}
