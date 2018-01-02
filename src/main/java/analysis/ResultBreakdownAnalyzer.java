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

public class ResultBreakdownAnalyzer {
    private static final String ENTITY_OPTION = "entity";
    private static final String ID_FILE_OPTION = "id";
    private static final String TOP_M_OPTION = "m";
    private static final String HAL_OPTION = "hal";
    private static final String HALX_OPTION = "halx";
    private static final String AUTHOR = "author";
    private static final String AFFILIATION = "affil";
    private static final String NATIONALITY = "nation";
    private static final int DEFAULT_HAL_THRESHOLD = 1;
    private static final int HALX_LABEL = -1;
    private static final int[] AUTHOR_INDICES = new int[]{0, 1};
    private static final int[] AFFIL_INDICES = new int[]{0, 1};
    private static final int[] NATION_INDICES = new int[]{0, 3};

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(Config.INPUT_DIR_OPTION, true, true, "[input] input dir", options);
        MiscUtil.setOption(ENTITY_OPTION, true, true, "[param] entity type", options);
        MiscUtil.setOption(ID_FILE_OPTION, true, true, "[input] entity ID file", options);
        MiscUtil.setOption(TOP_M_OPTION, true, true,
                "[param] top M authors in rankings used for evaluation (can be plural, separate with comma)", options);
        MiscUtil.setOption(HAL_OPTION, true, false,
                "[param, optional] HAL (Hit At Least) threshold (default = " + String.valueOf(DEFAULT_HAL_THRESHOLD)
                        + ")", options);
        MiscUtil.setOption(HALX_OPTION, false, false,
                "[param, optional] HAL (Hit At Least) threshold = # of true authors in each paper", options);
        MiscUtil.setOption(Config.OUTPUT_DIR_OPTION, true, true, "[output] output dir", options);
        return options;
    }

    private static Map<String, String> buildEntityMap(String entityType, String idFilePath) {
        Map<String, String> entityMap = new HashMap<>();
        int[] indices = entityType.equals(AUTHOR) ? AUTHOR_INDICES :
                entityType.equals(AFFILIATION) ? AFFIL_INDICES : NATION_INDICES;
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(idFilePath)));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(Config.FIRST_DELIMITER);
                entityMap.put(elements[indices[0]], elements[indices[1]]);
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ buildEntityMap");
            e.printStackTrace();
        }
        return entityMap;
    }

    private static void initArrayMapIfEmpty(String key, int arraySize, Map<String, Integer[]> arrayMap) {
        if (arrayMap.containsKey(key)) {
            return;
        }

        Integer[] array = new Integer[arraySize];
        for (int i = 0; i < array.length; i++) {
            array[i] = 0;
        }
        arrayMap.put(key, array);
    }

    private static int decideThreshold(Paper paper, int halThr) {
        return halThr == HALX_LABEL ? paper.getAuthorSize() : halThr;
    }

    private static void evaluate(List<Result> resultList, int[] topMs, Paper paper, Map<String, Integer> entityCountMap,
                                 Map<String, Integer[]> identifiedEntityCountMap) {
        for (String entityId : paper.getAuthorIdSet()) {
            if (!entityCountMap.containsKey(entityId)) {
                entityCountMap.put(entityId, 1);
            } else {
                entityCountMap.put(entityId, entityCountMap.get(entityId) + 1);
            }
        }

        Collections.sort(resultList);
        int trueAuthorSize = paper.getAuthorSize();
        int resultSize = resultList.size();
        for (int i = 0; i < resultSize; i++) {
            Result result = resultList.get(i);
            if (paper.checkIfAuthor(result.authorId) && result.score > 0.0d) {
                if (i < trueAuthorSize) {
                    initArrayMapIfEmpty(result.authorId, topMs.length + 1, identifiedEntityCountMap);
                    identifiedEntityCountMap.get(result.authorId)[0]++;
                }

                for (int j = 0; j < topMs.length; j++) {
                    if (i < topMs[j]) {
                        initArrayMapIfEmpty(result.authorId, topMs.length + 1, identifiedEntityCountMap);
                        identifiedEntityCountMap.get(result.authorId)[j + 1]++;
                    }
                }
            }

            if (i >= topMs[topMs.length - 1]) {
                break;
            }
        }
    }

    private static int calcTotalValue(Map<String, Integer> map) {
        int total = 0;
        for (String key : map.keySet()) {
            total += map.get(key);
        }
        return total;
    }

    private static void writeFiles(Map<String, Integer> entityCountMap, Map<String, Integer[]> identifiedEntityCountMap,
                                   int blindPaperSize, int guessablePaperSize, int halThr, int[] topMs,
                                   String entityType, String idFilePath, String outputDirPath) {
        Map<String, String> entityMap = buildEntityMap(entityType, idFilePath);
        FileUtil.makeDirIfNotExist(outputDirPath);
        try {
            String halThrStr = halThr != HALX_LABEL ? String.valueOf(halThr) : "x";
            double totalTrueEntitySize = (double) calcTotalValue(entityCountMap);
            for (int i = 0; i < topMs.length + 1; i++) {
                String topStr = i == 0 ? "x" : String.valueOf(topMs[i - 1]);
                String outputFilePath = outputDirPath + "/hal" + halThrStr + "-top" + topStr + ".csv";
                BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilePath)));
                bw.write("Blind Paper Count" + Config.FIRST_DELIMITER + String.valueOf(blindPaperSize)
                        + Config.FIRST_DELIMITER + "Guessable Paper Count" + Config.FIRST_DELIMITER
                        + String.valueOf(guessablePaperSize));
                bw.newLine();
                bw.write("Entity ID" + Config.FIRST_DELIMITER + "Entity Name" + Config.FIRST_DELIMITER
                        + "True Entity Count" + Config.FIRST_DELIMITER + "Identified Entity Count"
                        + Config.FIRST_DELIMITER + "True Entity Rate" + Config.FIRST_DELIMITER
                        + "Identified Entity Rate"+ Config.FIRST_DELIMITER + "True Entity Coverage");
                bw.newLine();
                for (String entityId : entityCountMap.keySet()) {
                    String entityName = entityMap.get(entityId);
                    int trueEntitySize = entityCountMap.get(entityId);
                    int identifiedEntitySize = identifiedEntityCountMap.containsKey(entityId) ?
                            identifiedEntityCountMap.get(entityId)[i] : 0;
                    bw.write(entityId + Config.FIRST_DELIMITER + entityName + Config.FIRST_DELIMITER
                            + String.valueOf(trueEntitySize) + Config.FIRST_DELIMITER
                            + String.valueOf(identifiedEntitySize) + Config.FIRST_DELIMITER
                            + String.valueOf(((double) trueEntitySize / totalTrueEntitySize)) + Config.FIRST_DELIMITER
                            + String.valueOf(((double) identifiedEntitySize / totalTrueEntitySize))
                            + Config.FIRST_DELIMITER
                            + String.valueOf(((double) identifiedEntitySize / (double) trueEntitySize)));
                    bw.newLine();
                }
                bw.close();
            }
        } catch (Exception e) {
            System.err.println("Exception @ writeFiles");
            e.printStackTrace();
        }
    }

    private static void analyze(String inputDirPath, String entityType, String idFilePath, String topMsStr,
                                 int halThr, String outputDirPath) {
        if (!entityType.equals(AUTHOR) && !entityType.equals(AFFILIATION) && !entityType.equals(NATIONALITY)) {
            return;
        }
        try {
            Map<String, Integer> entityCountMap = new TreeMap<>();
            Map<String, Integer[]> identifiedEntityCountMap = new TreeMap<>();
            int[] topMs = MiscUtil.convertToIntArray(topMsStr);
            List<File> inputDirList = FileUtil.getDirList(inputDirPath);
            if (inputDirList.size() == 0) {
                inputDirList.add(new File(inputDirPath));
            }

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

                    evaluate(resultList, topMs, paper, entityCountMap, identifiedEntityCountMap);
                    guessablePaperSize++;
                }
            }
            writeFiles(entityCountMap, identifiedEntityCountMap, blindPaperSize, guessablePaperSize,
                    halThr, topMs, entityType, idFilePath, outputDirPath);
        } catch (Exception e) {
            System.err.println("Exception @ analyze");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("ResultBreakdownAnalyzer", options, args);
        String inputDirPath = cl.getOptionValue(Config.INPUT_DIR_OPTION);
        String entityType = cl.getOptionValue(ENTITY_OPTION);
        String idFilePath = cl.getOptionValue(ID_FILE_OPTION);
        String topMsStr = cl.getOptionValue(TOP_M_OPTION);
        int halThr = cl.hasOption(HAL_OPTION) ? Integer.parseInt(cl.getOptionValue(HAL_OPTION)) : DEFAULT_HAL_THRESHOLD;
        halThr = cl.hasOption(HALX_OPTION) ? HALX_LABEL : halThr;
        String outputDirPath = cl.getOptionValue(Config.OUTPUT_DIR_OPTION);
        analyze(inputDirPath, entityType, idFilePath, topMsStr, halThr, outputDirPath);
    }
}
