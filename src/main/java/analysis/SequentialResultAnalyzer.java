package analysis;

import common.Config;
import common.FileUtil;
import common.MathUtil;
import common.MiscUtil;
import main.Evaluator;
import model.LogisticRegressionModel;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Pair;
import structure.Paper;
import structure.Result;

import java.io.*;
import java.util.*;

public class SequentialResultAnalyzer {
    private static final String AFFILS_FILE_OPTION = "a";
    private static final String MODEL_DIR_OPTION = "model";
    private static final String TOP_M_OPTION = "m";
    private static final String HAL_OPTION = "hal";
    private static final String HALX_OPTION = "halx";
    private static final String FILE_NAME_PREFIX = "author-size-";
    private static final int DEFAULT_HAL_THRESHOLD = 1;
    private static final int HALX_LABEL = -1;
    private static final int PAPER_ID_INDEX = 0;
    private static final int AUTHOR_ID_INDEX = 1;
    private static final int AUTHOR_SEQUENCE_NUMBER_INDEX = 5;
    private static final int INVALID_VALUE = -1;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(Config.INPUT_DIR_OPTION, true, true, "[input] input dir", options);
        MiscUtil.setOption(AFFILS_FILE_OPTION, true, true, "[input] PaperAuthorAffiliations file", options);
        MiscUtil.setOption(MODEL_DIR_OPTION, true, true, "[input] model dir", options);
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

    private static Set<String> buildRequiredPaperIdSet(List<File> inputDirList) {
        Set<String> requiredPaperIdSet = new HashSet<>();
        for (File inputDir : inputDirList) {
            List<File> inputFileList = FileUtil.getFileListR(inputDir.getPath());
            for (File inputFile : inputFileList) {
                requiredPaperIdSet.add(inputFile.getName());
            }
        }
        return requiredPaperIdSet;
    }

    private static int convertToInteger(String str) {
        int value = INVALID_VALUE;
        try {
            value = Integer.parseInt(str);
        } catch (Exception e) {
            return INVALID_VALUE;
        }
        return value;
    }

    private static Map<String, Map<String, Integer>> buildSequentialNumberMap(String affilsFilePath,
                                                                              List<File> inputDirList) {
        Map<String, Map<String, Integer>> sequentialNumberMap = new HashMap<>();
        Set<String> requiredPaperIdSet = buildRequiredPaperIdSet(inputDirList);
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(affilsFilePath)));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(Config.FIRST_DELIMITER);
                String paperId = elements[PAPER_ID_INDEX];
                String authorId = elements[AUTHOR_ID_INDEX];
                int authorSequenceNumber = convertToInteger(elements[AUTHOR_SEQUENCE_NUMBER_INDEX]);
                if (!requiredPaperIdSet.contains(paperId) || authorId.length() == 0 || authorSequenceNumber < 1) {
                    continue;
                }

                if (!sequentialNumberMap.containsKey(paperId)) {
                    sequentialNumberMap.put(paperId, new HashMap<>());
                }
                sequentialNumberMap.get(paperId).put(authorId, authorSequenceNumber);
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ buildSequentialNumberMap");
            e.printStackTrace();
        }
        return sequentialNumberMap;
    }

    private static Map<String, Integer[]> buildModelIndicatorsMap(String modelDirPath) {
        Map<String, Integer[]> modelIndicatorsMap = new HashMap<>();
        List<File> modelFileList = FileUtil.getFileList(modelDirPath);
        try {
            for (File modelFile : modelFileList) {
                BufferedReader br = new BufferedReader(new FileReader(modelFile));
                String line;
                while ((line = br.readLine()) != null) {
                    LogisticRegressionModel model = new LogisticRegressionModel(line);
                    modelIndicatorsMap.put(model.authorId, new Integer[]{model.paperIds.length,
                            model.getCitationIdSize(), model.getTotalCitationCount()});
                }
                br.close();
            }
        } catch (Exception e) {
            System.err.println("Exception @ buildModelIndicatorsMap");
            e.printStackTrace();
        }
        return modelIndicatorsMap;
    }

    private static int decideThreshold(Paper paper, int halThr) {
        return halThr == HALX_LABEL ? paper.getAuthorSize() : halThr;
    }

    private static void initMapsIfNotExist(int key, Map<Integer, Map<Integer, Integer>> entityCountMap,
                                           Map<Integer, Map<Integer, Integer[]>> identifiedEntityCountMap,
                                           Map<Integer, Map<Integer, List<List<Integer>>>> indicatorArrayMap) {
        if (!entityCountMap.containsKey(key)) {
            entityCountMap.put(key, new HashMap<>());
        }

        if (!identifiedEntityCountMap.containsKey(key)) {
            identifiedEntityCountMap.put(key, new HashMap<>());
        }

        if (!indicatorArrayMap.containsKey(key)) {
            indicatorArrayMap.put(key, new HashMap<>());
        }
    }

    private static void initListOfListMapIfNotExist(int key, int listSize, Map<Integer, List<List<Integer>>> map) {
        if (map.containsKey(key)) {
            return;
        }

        List<List<Integer>> listOfList = new ArrayList<>();
        for (int i = 0; i < listSize; i++) {
            listOfList.add(new ArrayList<>());
        }
        map.put(key, listOfList);
    }

    private static void updateMaps(Paper paper, Map<String, Integer[]> modelIndicatorsMap,
                                   Map<Integer, Map<Integer, Integer>> entityCountMap,
                                   Map<Integer, Map<Integer, Integer[]>> identifiedEntityCountMap,
                                   Map<Integer, Map<Integer, List<List<Integer>>>> indicatorListMap) {
        int authorSize = paper.getAuthorSize();
        initMapsIfNotExist(authorSize, entityCountMap, identifiedEntityCountMap, indicatorListMap);
        Map<Integer, Integer> subEntityCountMap = entityCountMap.get(authorSize);
        Map<Integer, List<List<Integer>>> subIndicatorListMap = indicatorListMap.get(authorSize);
        List<String> authorIdList = new ArrayList<>(paper.getAuthorIdSet());
        for (int i = 1; i <= authorSize; i++) {
            if (!subEntityCountMap.containsKey(i)) {
                subEntityCountMap.put(i, 1);
            } else {
                subEntityCountMap.put(i, subEntityCountMap.get(i) + 1);
            }

            String authorId = authorIdList.get(i - 1);
            initListOfListMapIfNotExist(i, 3, subIndicatorListMap);
            if (modelIndicatorsMap.containsKey(authorId)) {
                Integer[] indicators = modelIndicatorsMap.get(authorId);
                for (int j = 0; j < indicators.length; j++) {
                    subIndicatorListMap.get(i).get(j).add(indicators[j]);

                }
            }
        }
    }

    private static void evaluate(List<Result> resultList, int[] topMs, Paper paper, boolean guessable,
                                 Map<String, Map<String, Integer>> sequentialNumberMap,
                                 Map<String, Integer[]> modelIndicatorsMap,
                                 Map<Integer, Map<Integer, Integer>> entityCountMap,
                                 Map<Integer, Map<Integer, Integer[]>> identifiedEntityCountMap,
                                 Map<Integer, Map<Integer, List<List<Integer>>>> indicatorListMap) {
        if (!sequentialNumberMap.containsKey(paper.id)) {
            return;
        }

        updateMaps(paper, modelIndicatorsMap, entityCountMap, identifiedEntityCountMap, indicatorListMap);
        int authorSize = paper.getAuthorSize();
        Map<Integer, Integer[]> subIdentifiedEntityCountMap = identifiedEntityCountMap.get(authorSize);
        if (!guessable) {
            return;
        }

        Map<String, Integer> authorNumberMap = sequentialNumberMap.get(paper.id);
        Collections.sort(resultList);
        int trueAuthorSize = paper.getAuthorSize();
        int resultSize = resultList.size();
        for (int i = 0; i < resultSize; i++) {
            Result result = resultList.get(i);
            if (paper.checkIfAuthor(result.authorId) && result.score > 0.0d
                    && authorNumberMap.containsKey(result.authorId)) {
                int authorSequenceNumber = authorNumberMap.get(result.authorId);
                if (i < trueAuthorSize) {
                    MiscUtil.initArrayMapNotExist(authorSequenceNumber, topMs.length + 1, subIdentifiedEntityCountMap);
                    subIdentifiedEntityCountMap.get(authorSequenceNumber)[0]++;
                }

                for (int j = 0; j < topMs.length; j++) {
                    if (i < topMs[j]) {
                        MiscUtil.initArrayMapNotExist(authorSequenceNumber, topMs.length + 1, subIdentifiedEntityCountMap);
                        subIdentifiedEntityCountMap.get(authorSequenceNumber)[j + 1]++;
                    }
                }
            }

            if (i >= topMs[topMs.length - 1]) {
                break;
            }
        }
    }

    private static double[] calcAverages(List<List<Integer>> listOfList) {
        return new double[] {MathUtil.calcAverage(listOfList.get(0)), MathUtil.calcAverage(listOfList.get(1)),
                MathUtil.calcAverage(listOfList.get(2))};
    }

    private static void writeFiles(Map<Integer, Integer> subEntityCountMap,
                                   Map<Integer, Integer[]> subIdentifiedEntityCountMap,
                                   Map<Integer, List<List<Integer>>> subIndicatorListMap, int blindPaperSize,
                                   int guessablePaperSize, int authorSize, int halThr, int[] topMs,
                                   String outputDirPath) {
        FileUtil.makeDirIfNotExist(outputDirPath);
        try {
            String halThrStr = halThr != HALX_LABEL ? String.valueOf(halThr) : "x";
            int trueEntitySize = subEntityCountMap.get(subEntityCountMap.keySet().toArray()[0]);
            for (int i = 0; i < topMs.length + 1; i++) {
                String topStr = i == 0 ? "x" : String.valueOf(topMs[i - 1]);
                String outputFilePath = outputDirPath + "/hal" + halThrStr + "-top" + topStr + ".csv";
                BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilePath)));
                bw.write("Blind Paper Count" + Config.FIRST_DELIMITER + String.valueOf(blindPaperSize)
                        + Config.FIRST_DELIMITER + "Blind Paper Count (# True Authors = " + String.valueOf(authorSize)
                        + ")" + Config.FIRST_DELIMITER + String.valueOf(trueEntitySize) + "Guessable Paper Count"
                        + Config.FIRST_DELIMITER + String.valueOf(guessablePaperSize) + Config.FIRST_DELIMITER);
                bw.newLine();
                bw.write("Author Sequence Number" + Config.FIRST_DELIMITER + "Identified Entity Count"
                        + Config.FIRST_DELIMITER + "Identification Rate"
                        + Config.FIRST_DELIMITER + "Identified Entity Coverage"
                        + Config.FIRST_DELIMITER + "Average # Publications"
                        + Config.FIRST_DELIMITER + "Average # Unique Citations"
                        + Config.FIRST_DELIMITER + "Average # Citations");
                bw.newLine();
                int totalIdentifiedEntitySize = 0;
                for (int sequenceNumber : subEntityCountMap.keySet()) {
                    int identifiedEntitySize = subIdentifiedEntityCountMap.containsKey(sequenceNumber) ?
                            subIdentifiedEntityCountMap.get(sequenceNumber)[i] : 0;
                    totalIdentifiedEntitySize += identifiedEntitySize;
                }

                for (int sequenceNumber : subEntityCountMap.keySet()) {
                    int identifiedEntitySize = subIdentifiedEntityCountMap.containsKey(sequenceNumber) ?
                            subIdentifiedEntityCountMap.get(sequenceNumber)[i] : 0;
                    double[] indicators = calcAverages(subIndicatorListMap.get(sequenceNumber));
                    bw.write(String.valueOf(sequenceNumber) + Config.FIRST_DELIMITER
                            + String.valueOf(identifiedEntitySize) + Config.FIRST_DELIMITER
                            + String.valueOf(((double) identifiedEntitySize / (double) trueEntitySize))
                            + Config.FIRST_DELIMITER
                            + String.valueOf(((double) identifiedEntitySize / (double) totalIdentifiedEntitySize))
                            + Config.FIRST_DELIMITER
                            + String.valueOf(indicators[0])
                            + Config.FIRST_DELIMITER
                            + String.valueOf(indicators[1])
                            + Config.FIRST_DELIMITER
                            + String.valueOf(indicators[2]));
                    bw.newLine();
                }
                bw.close();
            }
        } catch (Exception e) {
            System.err.println("Exception @ writeFiles");
            e.printStackTrace();
        }
    }

    private static void analyze(String inputDirPath, String affilsFilePath, String modelDirPath, String topMsStr,
                                int halThr, String outputDirPath) {
        try {
            int[] topMs = MiscUtil.convertToIntArray(topMsStr, Config.OPTION_DELIMITER);
            List<File> inputDirList = FileUtil.getDirList(inputDirPath);
            if (inputDirList.size() == 0) {
                inputDirList.add(new File(inputDirPath));
            }

            Map<String, Map<String, Integer>> sequentialNumberMap
                    = buildSequentialNumberMap(affilsFilePath, inputDirList);
            Map<String, Integer[]> modelIndicatorsMap = buildModelIndicatorsMap(modelDirPath);
            Map<Integer, Map<Integer, Integer>> entityCountMap = new TreeMap<>();
            Map<Integer, Map<Integer, Integer[]>> identifiedEntityCountMap = new TreeMap<>();
            Map<Integer, Map<Integer, List<List<Integer>>>> indicatorListMap = new TreeMap<>();
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
                    if (paper.getAuthorSize() < threshold) {
                        blindPaperSize--;
                    }

                    boolean guessable = resultList.size() > 0;
                    if (guessable) {
                        guessablePaperSize++;
                    }

                    evaluate(resultList, topMs, paper, guessable, sequentialNumberMap, modelIndicatorsMap,
                            entityCountMap, identifiedEntityCountMap, indicatorListMap);
                }
            }

            for (int authorSize : entityCountMap.keySet()) {
                writeFiles(entityCountMap.get(authorSize), identifiedEntityCountMap.get(authorSize),
                        indicatorListMap.get(authorSize), blindPaperSize, guessablePaperSize, authorSize, halThr,
                        topMs, outputDirPath + FILE_NAME_PREFIX + String.valueOf(authorSize) + "/");
            }
        } catch (Exception e) {
            System.err.println("Exception @ analyze");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("SequentialResultAnalyzer", options, args);
        String inputDirPath = cl.getOptionValue(Config.INPUT_DIR_OPTION);
        String affilsFilePath = cl.getOptionValue(AFFILS_FILE_OPTION);
        String modelDirPath = cl.hasOption(MODEL_DIR_OPTION) ? cl.getOptionValue(MODEL_DIR_OPTION) : null;
        String topMsStr = cl.getOptionValue(TOP_M_OPTION);
        int halThr = cl.hasOption(HAL_OPTION) ? Integer.parseInt(cl.getOptionValue(HAL_OPTION)) : DEFAULT_HAL_THRESHOLD;
        halThr = cl.hasOption(HALX_OPTION) ? HALX_LABEL : halThr;
        String outputDirPath = cl.getOptionValue(Config.OUTPUT_DIR_OPTION);
        analyze(inputDirPath, affilsFilePath, modelDirPath, topMsStr, halThr, outputDirPath);
    }
}
