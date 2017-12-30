package sub;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.*;

public class Collector {
    private static final String INPUT_DIR_OPTION = "input";
    private static final String SHUFFLE_SIZE_OPTION = "shuffle";
    private static final String KEY_INDEX_OPTION = "key";
    private static final String NEGATIVE_SAMPLE_RATIO_OPTION = "negative";
    private static final String STEP_SIZE_OPTION = "step";
    private static final String OUTPUT_DIR_OPTION = "output";
    private static final String NORMALIZATION = "normalize";
    private static final String TRAINING = "train";
    private static final int INVALID_VALUE = -1;
    private static final int DEFAULT_STEP_SIZE = 4;
    private static final int LABEL_INDEX = 0;
    private static final int PAPER_ID_INDEX = 1;
    private static final int FEATURE_START_INDEX = 3;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(INPUT_DIR_OPTION, true, true, "[param] input dir", options);
        MiscUtil.setOption(SHUFFLE_SIZE_OPTION, true, false, "[param, optional] shuffle size", options);
        MiscUtil.setOption(KEY_INDEX_OPTION, true, false, "[param, optional] key index", options);
        MiscUtil.setOption(NEGATIVE_SAMPLE_RATIO_OPTION, true, false, "[param, optional] negative sample ratio", options);
        MiscUtil.setOption(STEP_SIZE_OPTION, true, false, "[param, optional] step size", options);
        MiscUtil.setOption(NORMALIZATION, true, false, "[param, optional] normalization option", options);
        MiscUtil.setOption(OUTPUT_DIR_OPTION, true, true, "[param] output dir", options);
        return options;
    }

    private static double determineNegativeRandomThreshold(List<File> inputFileList, float negSampleRatio) {
        int totalSampleCount = 0;
        int positiveSampleCount = 0;
        try {
            for (File inputFile : inputFileList) {
                BufferedReader br = new BufferedReader(new FileReader(inputFile));
                String line;
                while ((line = br.readLine()) != null) {
                    totalSampleCount++;
                    String[] elements = line.split(Config.FIRST_DELIMITER);
                    if (elements[LABEL_INDEX].equals(Config.POS_TRAIN_LABEL)) {
                        positiveSampleCount++;
                    }
                }
                br.close();
            }
        } catch (Exception e) {
            System.err.println("Exception @ calculateNegativeSampleRate");
            e.printStackTrace();
        }

        float requiredNegSampleCount = (float) positiveSampleCount / (1.0f / negSampleRatio - 1.0f);
        return requiredNegSampleCount / (float) (totalSampleCount - positiveSampleCount);
    }

    private static void shuffle(List<File> inputFileList, int shuffleSize, float negSampleRatio,
                                int stepSize, String outputDirPath) {
        Collections.shuffle(inputFileList);
        DecimalFormat decimalFormat = new DecimalFormat("0000");
        Random rand = new Random();
        double negRandThr = 0.0d <= negSampleRatio && negSampleRatio <= 1.0d ?
                determineNegativeRandomThreshold(inputFileList, negSampleRatio) : 1.0d;
        try {
            boolean first = true;
            int fileSize = inputFileList.size();
            Map<Integer, List<String>> lineListMap = new HashMap<>();
            for (int i = 0; i < fileSize; i++) {
                BufferedReader br = new BufferedReader(new FileReader(inputFileList.remove(0)));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] elements = line.split(Config.FIRST_DELIMITER);
                    if (!elements[LABEL_INDEX].equals(Config.POS_TRAIN_LABEL) && rand.nextFloat() > negRandThr) {
                        continue;
                    }

                    int randValue = rand.nextInt(shuffleSize);
                    if (!lineListMap.containsKey(randValue)) {
                        lineListMap.put(randValue, new ArrayList<>());
                    }
                    lineListMap.get(randValue).add(line);
                }

                br.close();
                if ((i > 0 && i % stepSize == 0) || i == fileSize - 1) {
                    for (int key : lineListMap.keySet()) {
                        String fileName = decimalFormat.format(key);
                        FileUtil.overwriteFile(lineListMap.get(key), first,outputDirPath + "/" + fileName);
                    }

                    lineListMap.clear();
                    first = false;
                }
            }
        } catch (Exception e) {
            System.err.println("Exception @ shuffle");
            e.printStackTrace();
        }
    }

    private static void sort(List<File> inputFileList, int keyIndex, String outputDirPath) {
        try {
            int fileSize = inputFileList.size();
            Map<String, List<String>> lineListMap = new HashMap<>();
            for (int i = 0; i < fileSize; i++) {
                BufferedReader br = new BufferedReader(new FileReader(inputFileList.remove(0)));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] elements = line.split(Config.FIRST_DELIMITER);
                    if (!lineListMap.containsKey(elements[keyIndex])) {
                        lineListMap.put(elements[keyIndex], new ArrayList<>());
                    }
                    lineListMap.get(elements[keyIndex]).add(line);
                }
                br.close();
            }

            for (String fileName : lineListMap.keySet()) {
                FileUtil.writeFile(lineListMap.get(fileName),outputDirPath + "/" + fileName);
            }
        } catch (Exception e) {
            System.err.println("Exception @ sort");
            e.printStackTrace();
        }
    }

    private static Map<String, List<Double[]>> extractMinMaxMap(List<File> inputFileList, boolean isTraining) {
        System.out.println("Start: extractMinMaxMap");
        Map<String, List<Double[]>> paramListMap = new HashMap<>();
        try {
            for (File inputFile :inputFileList) {
                BufferedReader br = new BufferedReader(new FileReader(inputFile));
                boolean first = true;
                String line;
                while ((line = br.readLine()) != null) {
                    if (first) {
                        first = false;
                        continue;
                    }
                    String[] elements = line.split(Config.FIRST_DELIMITER);
                    int startIdx = isTraining ? FEATURE_START_INDEX : FEATURE_START_INDEX - 1;
                    String paperId = isTraining ? elements[PAPER_ID_INDEX] : inputFile.getName();
                    if (!paramListMap.containsKey(paperId)) {
                        paramListMap.put(paperId, new ArrayList<>());
                    }

                    List<Double[]> paramList = paramListMap.get(paperId);
                    for (int i = startIdx; i < elements.length; i++) {
                        int index = i - startIdx;
                        if (paramList.size() == index) {
                            paramList.add(new Double[]{Double.MAX_VALUE, -Double.MAX_VALUE});
                        }

                        double value = Double.parseDouble(elements[i]);
                        Double[] params = paramList.get(index);
                        if (value < params[0]) {
                            params[0] = value;
                        }
                        if (value > params[1]) {
                            params[1] = value;
                        }
                    }
                }
                br.close();
            }
        } catch (Exception e) {
            System.err.println("Exception @ extractMinMaxMap");
            e.printStackTrace();
        }

        System.out.println("end: extractMinMaxMap");
        return paramListMap;
    }

    private static double normalize(double value, Double[] params) {
        if (value == 0.0d) {
            return value;
        } else if (params[0] == params[1] || params[0] == Double.MAX_VALUE || params[1] == -Double.MAX_VALUE) {
            return value > 0.0d ? 2.0d : 0.0d;
        }
        return (value - params[0]) / (params[1] - params[0]) + 1.0d;
    }

    private static void normalize(String inputDirPath, boolean isTraining, String outputDirPath) {
        List<File> inputFileList = isTraining ? FileUtil.getFileList(inputDirPath) : FileUtil.getFileListR(inputDirPath);
        Map<String, List<Double[]>> paramListMap = extractMinMaxMap(inputFileList, isTraining);
        try {
            for (File inputFile :inputFileList) {
                BufferedReader br = new BufferedReader(new FileReader(inputFile));
                List<String> outputLineList = new ArrayList<>();
                String line;
                while ((line = br.readLine()) != null) {
                    if (!isTraining && outputLineList.size() == 0) {
                        outputLineList.add(line);
                        continue;
                    }

                    String[] elements = line.split(Config.FIRST_DELIMITER);
                    int startIdx = isTraining ? FEATURE_START_INDEX : FEATURE_START_INDEX - 1;
                    String paperId = isTraining ? elements[PAPER_ID_INDEX] : inputFile.getName();

                    List<Double[]> paramList = paramListMap.get(paperId);
                    for (int i = startIdx; i < elements.length; i++) {
                        int index = i - startIdx;
                        double value = Double.parseDouble(elements[i]);
                        elements[i] = String.valueOf(normalize(value, paramList.get(index)));
                    }

                    StringBuilder sb = new StringBuilder(elements[0]);
                    for (int i = 1; i < elements.length; i++) {
                        sb.append(Config.FIRST_DELIMITER + elements[i]);
                    }
                    outputLineList.add(sb.toString());
                }

                br.close();
                String outputFilePath = isTraining ? outputDirPath + "/" + inputFile.getName()
                        : outputDirPath + "/" + inputFile.getParentFile().getName() + "/" + inputFile.getName();
                FileUtil.writeFile(outputLineList, outputFilePath);
            }
        } catch (Exception e) {
            System.err.println("Exception @ normalize");
            e.printStackTrace();
        }
    }

    private static void collect(String inputDirPath, int shuffleSize, int keyIndex, float negSampleRatio,
                                int stepSize, String rescaleTarget, String outputDirPath) {
        List<File> inputFileList = FileUtil.getFileList(inputDirPath);
        if (shuffleSize > 0 && keyIndex == INVALID_VALUE && rescaleTarget == null) {
            shuffle(inputFileList, shuffleSize, negSampleRatio, stepSize, outputDirPath);
        } else if (keyIndex >= 0 && shuffleSize == INVALID_VALUE
                && negSampleRatio == INVALID_VALUE && rescaleTarget == null) {
            sort(inputFileList, keyIndex, outputDirPath);
        } else if (rescaleTarget != null) {
            normalize(inputDirPath, rescaleTarget.equals(TRAINING), outputDirPath);
        }
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("Collector", options, args);
        String inputDirPath = cl.getOptionValue(INPUT_DIR_OPTION);
        int shuffleSize = cl.hasOption(SHUFFLE_SIZE_OPTION) ?
                Integer.parseInt(cl.getOptionValue(SHUFFLE_SIZE_OPTION)) : INVALID_VALUE;
        int keyIndex = cl.hasOption(KEY_INDEX_OPTION) ?
                Integer.parseInt(cl.getOptionValue(KEY_INDEX_OPTION)) : INVALID_VALUE;
        float negSampleRatio = cl.hasOption(NEGATIVE_SAMPLE_RATIO_OPTION) ?
                Float.parseFloat(cl.getOptionValue(NEGATIVE_SAMPLE_RATIO_OPTION)) : INVALID_VALUE;
        int stepSize = cl.hasOption(STEP_SIZE_OPTION) ?
                Integer.parseInt(cl.getOptionValue(STEP_SIZE_OPTION)) : DEFAULT_STEP_SIZE;
        String rescaleTarget = cl.hasOption(NORMALIZATION) ? cl.getOptionValue(NORMALIZATION) : null;
        String outputDirPath = cl.getOptionValue(OUTPUT_DIR_OPTION);
        collect(inputDirPath, shuffleSize, keyIndex, negSampleRatio, stepSize, rescaleTarget, outputDirPath);
    }
}
