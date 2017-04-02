package main;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import model.CountUpModel;
import model.LogisticRegressionModel;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Pair;
import structure.Paper;

import java.io.*;
import java.util.*;

public class LogRegParamEstimator {
    private static final String TRAIN_DIR_OPTION = "train";
    private static final String MODEL_DIR_OPTION = "model";
    private static final String RANDOM_VALUE_SCALE_OPTION = "rvscale";
    private static final String EPOCH_SIZE_OPTION = "epoch";
    private static final String START_IDX_OPTION = "sidx";
    private static final String BATCH_SIZE_OPTION = "bsize";
    private static final String NEGATIVE_SAMPLE_SIZE_OPTION = "nssize";
    private static final String REGULATION_PARAM_OPTION = "rparam";
    private static final String LEARNING_RATE_OPTION = "lrate";
    private static final String THRESHOLD_OPTION = "thr";
    private static final int PARAM_SIZE = LogisticRegressionModel.PARAM_SIZE;
    private static final int OPTION_PARAM_SIZE = 7;
    private static final int DEFAULT_EPOCH_SIZE = 50;
    private static final int DEFAULT_START_IDX_SIZE = 0;
    private static final int DEFAULT_BATCH_SIZE = 5000;
    private static final int DEFAULT_NEGATIVE_SAMPLE_SIZE = 100;
    private static final double DEFAULT_RANDOM_VALUE_SCALE = 1e-2d;
    private static final double DEFAULT_REGULATION_PARAM = 1e-2d;
    private static final double DEFAULT_LEARNING_RATE = 1e-2d;
    private static final double DEFAULT_THRESHOLD = 1e-5d;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(TRAIN_DIR_OPTION, true, true, "[input] train dir", options);
        MiscUtil.setOption(MODEL_DIR_OPTION, true, true, "[input] model dir", options);
        MiscUtil.setOption(RANDOM_VALUE_SCALE_OPTION, true, false, "[param, optional] random value scale", options);
        MiscUtil.setOption(EPOCH_SIZE_OPTION, true, false, "[param, optional] epoch size", options);
        MiscUtil.setOption(START_IDX_OPTION, true, false, "[param, optional] start index (epoch)", options);
        MiscUtil.setOption(BATCH_SIZE_OPTION, true, false, "[param, optional] batch size", options);
        MiscUtil.setOption(NEGATIVE_SAMPLE_SIZE_OPTION, true, false, "[param, optional] negative sample size", options);
        MiscUtil.setOption(REGULATION_PARAM_OPTION, true, false, "[param, optional] regulation parameter", options);
        MiscUtil.setOption(LEARNING_RATE_OPTION, true, false, "[param, optional] learning rate", options);
        MiscUtil.setOption(THRESHOLD_OPTION, true, false, "[param, optional] convergence threshold", options);
        MiscUtil.setOption(Config.OUTPUT_FILE_OPTION, true, true, "[output] output file", options);
        return options;
    }

    private static Pair<List<Double>, List<String>> loadParams(File file) {
        double[] params = new double[PARAM_SIZE];
        List<Double> paramList = new ArrayList<>();
        List<String> optionParamList = new ArrayList<>();
        Pair<List<Double>, List<String>> paramListPair = new Pair<>(paramList, optionParamList);
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            String[] optionParams = line.split(Config.FIRST_DELIMITER);
            for (int i = 1; i < optionParams.length; i += 2) {
                optionParamList.add(optionParams[i]);
            }

            int startIdx = 0;
            String preLine = null;
            while ((line = br.readLine()) != null) {
                preLine = line;
                startIdx++;
            }

            optionParamList.add(String.valueOf(startIdx));
            String[] elements = preLine.split(Config.FIRST_DELIMITER);
            for (int i = 0; i < params.length; i++) {
                paramList.add(Double.parseDouble(elements[i]));
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ loadParams");
            e.printStackTrace();
        }
        return paramListPair;
    }

    private static void initParams(String filePath, double[] params, double randomValueScale, String[] optionParams) {
        File file = new File(filePath);
        Pair<List<Double>, List<String>> paramListPair = file.exists() && file.isFile() ? loadParams(file) : null;
        if (paramListPair == null) {
            Random rand = new Random();
            for (int i = 0; i < params.length; i++) {
                params[i] = (rand.nextDouble() - 0.5d) * randomValueScale;
            }
        } else {
            List<Double> paramList = paramListPair.first;
            List<String> optionParamList = paramListPair.second;
            for (int i = 0; i < params.length; i++) {
                params[i] = paramList.get(i);
            }

            for (int i = 0; i < optionParams.length; i++) {
                optionParams[i] = optionParamList.get(i);
            }
        }
    }

    private static List<Paper> readPaperFiles(String trainDirPath) {
        List<File> trainPaperFileList = FileUtil.getFileList(trainDirPath);
        List<Paper> trainPaperList = new ArrayList<>();
        try {
            for (File trainPaperFile : trainPaperFileList) {
                BufferedReader br = new BufferedReader(new FileReader(trainPaperFile));
                String line;
                while ((line = br.readLine()) != null) {
                    trainPaperList.add(new Paper(line));
                }
                br.close();
            }
        } catch (Exception e) {
            System.err.println("Exception @ readPaperFiles");
            e.printStackTrace();
        }
        return trainPaperList;
    }

    private static Pair<HashMap<String, CountUpModel>, List<String>> readModelFiles(String modelDirPath) {
        HashMap<String, CountUpModel> modelMap = new HashMap<>();
        List<String> trainAuthorIdList = new ArrayList<>();
        List<File> modelFileList = FileUtil.getFileList(modelDirPath);
        System.out.println("Start:\treading model files");
        try {
            for (File modelFile : modelFileList) {
                BufferedReader br = new BufferedReader(new FileReader(modelFile));
                String line;
                while ((line = br.readLine()) != null) {
                    CountUpModel model = new CountUpModel(line);
                    modelMap.put(model.authorId, model);
                    trainAuthorIdList.add(model.authorId);
                }
                br.close();
            }
        } catch (Exception e) {
            System.err.println("Exception @ readModelFiles");
            e.printStackTrace();
        }

        System.out.println("End:\treading model files");
        return new Pair(modelMap, trainAuthorIdList);
    }

    private static List<Paper> deepCopyInRandomOrder(List<Paper> paperList) {
        List<Paper> tmpPaperList = new ArrayList<>();
        for (Paper paper : paperList) {
            tmpPaperList.add(paper);
        }

        List<Paper> copyPaperList = new ArrayList<>();
        Random rand = new Random();
        while (tmpPaperList.size() > 0) {
            Paper paper = tmpPaperList.remove(rand.nextInt(tmpPaperList.size()));
            copyPaperList.add(paper);
        }
        return copyPaperList;
    }

    private static double calcInnerProduct(double[] params, double[] featureValues) {
        double ip = 0.0d;
        for (int i = 0; i < params.length; i++) {
            ip += params[i] * featureValues[i];
        }
        return ip;
    }

    private static double[] calcDifferentiatedLogLogReg(double[] params, double[] featureValues) {
        double[] values = MiscUtil.initDoubleArray(params.length, 0.0d);
        double ip = calcInnerProduct(params, featureValues);
        double expVal = Math.exp(-ip);
        double commonTerm = expVal / (1.0d + expVal);
        for (int i = 0; i < values.length; i++) {
            values[i] = featureValues[i] * commonTerm;
        }
        return values;
    }

    private static void updateParams(double[] params, List<Paper> batchPaperList,
                                         HashMap<String, CountUpModel> modelMap, List<String> trainAuthorIdList,
                                         int negativeSampleSize, double regParam, double learnRate) {
        double[] gradParams = MiscUtil.initDoubleArray(params.length, 0.0d);
        int count = 0;
        Random rand = new Random();
        while (batchPaperList.size() > 0) {
            Paper paper = batchPaperList.remove(0);
            Iterator<String> ite = paper.getAuthorSet().iterator();
            while (ite.hasNext()) {
                String authorId = ite.next();
                if (!modelMap.containsKey(authorId)) {
                    continue;
                }

                int sampleCount = 0;
                double[] negGradParams = MiscUtil.initDoubleArray(params.length, 0.0d);
                while (sampleCount < negativeSampleSize) {
                    int idx = rand.nextInt(negativeSampleSize);
                    String id = trainAuthorIdList.get(idx);
                    if (paper.checkIfAuthor(id)) {
                        continue;
                    }

                    double[] featureValues = LogisticRegressionModel.extractFeatureValues(modelMap.get(id), paper);
                    double[] subParams = calcDifferentiatedLogLogReg(params, featureValues);
                    for (int i = 0; i < subParams.length; i++) {
                        negGradParams[i] += subParams[i];
                    }
                    sampleCount++;
                }

                double[] featureValues = LogisticRegressionModel.extractFeatureValues(modelMap.get(authorId), paper);
                double[] posGradParams = calcDifferentiatedLogLogReg(params, featureValues);
                for (int i = 0; i < gradParams.length; i++) {
                    gradParams[i] += posGradParams[i] - negGradParams[i] / (double) negativeSampleSize;
                }
                count++;
            }
        }

        for (int i = 0; i < params.length; i++) {
            gradParams[i] = gradParams[i] / (double) count - 2.0d * regParam * params[i];
            params[i] +=  learnRate * gradParams[i];
        }
    }

    private static void writeUpdatedParams(double[] params, int epochSize, int batchSize, int negativeSampleSize,
                                           double regParam, double learnRate, double threshold, String outputFilePath) {
        try {
            FileUtil.makeParentDir(outputFilePath);
            File outputFile = new File(outputFilePath);
            boolean appendMode = outputFile.exists();
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, appendMode));
            if (!appendMode) {
                bw.write("Epoch size\t" + String.valueOf(epochSize) + "\tBatch size\t" + String.valueOf(batchSize)
                        + "\tNegative sample size\t" + String.valueOf(negativeSampleSize)
                        + "\tRegulation param\t" + String.valueOf(regParam)
                        + "\tLearning rate\t" + String.valueOf(learnRate)
                        + "\tThreshold\t" + String.valueOf(threshold));
                bw.newLine();
            }

            StringBuilder sb = new StringBuilder(String.valueOf(params[0]));
            for (int i = 1; i < params.length; i++) {
                sb.append(Config.FIRST_DELIMITER + String.valueOf(params[i]));
            }

            bw.write(sb.toString());
            bw.newLine();
            bw.close();
        } catch (Exception e) {
            System.err.println("Exception @ writeUpdatedParams");
            e.printStackTrace();
        }
    }

    private static void showLogLikelihood(double[] params, List<Paper> paperList, HashMap<String, CountUpModel> modelMap,
                                          List<String> trainAuthorIdList, int negativeSampleSize, double regParam) {
        double logLikelihood = 0.0d;
        int count = 0;
        Random rand = new Random();
        while (paperList.size() > 0) {
            Paper paper = paperList.remove(0);
            Iterator<String> ite = paper.getAuthorSet().iterator();
            while (ite.hasNext()) {
                String authorId = ite.next();
                if (!modelMap.containsKey(authorId)) {
                    continue;
                }

                int sampleCount = 0;
                double negLogLikelihood = 0.0d;
                while (sampleCount < negativeSampleSize) {
                    int idx = rand.nextInt(negativeSampleSize);
                    String id = trainAuthorIdList.get(idx);
                    if (paper.checkIfAuthor(id)) {
                        continue;
                    }

                    double[] featureValues = LogisticRegressionModel.extractFeatureValues(modelMap.get(id), paper);
                    negLogLikelihood += Math.log(LogisticRegressionModel.logisticFunction(featureValues, params));
                    sampleCount++;
                }

                double[] featureValues = LogisticRegressionModel.extractFeatureValues(modelMap.get(authorId), paper);
                double posLogLikelihood = Math.log(LogisticRegressionModel.logisticFunction(featureValues, params));
                logLikelihood += posLogLikelihood - negLogLikelihood / (double) negativeSampleSize;
                count++;
            }
        }

        double norm = 0.0d;
        for (double value : params) {
            norm += Math.pow(value, 2.0d);
        }

        logLikelihood = logLikelihood / (double) count - regParam * norm;
        System.out.println("\t\tLog-Likelihood: " + String.valueOf(logLikelihood));
    }

    private static boolean checkIfConverged(double[] params, double[] preParams, double threshold) {
        double norm = 0.0d;
        for (int i = 0; i < params.length; i++) {
            norm += Math.pow(params[i] - preParams[i], 2.0d);
        }
        return Math.sqrt(norm) < threshold;
    }

    private static void estimate(String trainDirPath, String modelDirPath, double randomValueScale,
                                 String[] optionParams, String outputFilePath) {
        double[] params = new double[PARAM_SIZE];
        double[] preParams = new double[PARAM_SIZE];
        initParams(outputFilePath, params, randomValueScale, optionParams);
        int epochSize = Integer.parseInt(optionParams[0]);
        int startIdx = Integer.parseInt(optionParams[1]);
        int negativeSampleSize = Integer.parseInt(optionParams[2]);
        int batchSize = Integer.parseInt(optionParams[3]);
        double regParam = Double.parseDouble(optionParams[4]);
        double learnRate = Double.parseDouble(optionParams[5]);
        double threshold = Double.parseDouble(optionParams[6]);
        List<Paper> trainPaperList = readPaperFiles(trainDirPath);
        Pair<HashMap<String, CountUpModel>, List<String>> pair = readModelFiles(modelDirPath);
        HashMap<String, CountUpModel> modelMap = pair.first;
        List<String> trainAuthorIdList = pair.second;
        int t = 0;
        System.out.println("Start:\testimating parameters");
        for (int i = startIdx; i < epochSize; i++) {
            System.out.println("\tEpoch " + String.valueOf(i + 1) + "/" + String.valueOf(epochSize));
            List<Paper> copyTrainPaperList = deepCopyInRandomOrder(trainPaperList);
            MiscUtil.deepCopy(params, preParams);
            while (copyTrainPaperList.size() > 0) {
                t++;
                List<Paper> batchPaperList = new ArrayList<>();
                int size = copyTrainPaperList.size() > batchSize ? batchSize : copyTrainPaperList.size();
                for (int j = 0; j < size; j++) {
                    batchPaperList.add(copyTrainPaperList.remove(0));
                }
                updateParams(params, batchPaperList, modelMap, trainAuthorIdList, negativeSampleSize,
                        regParam, learnRate / (double) t);
            }

            writeUpdatedParams(params, epochSize, batchSize, negativeSampleSize, regParam, learnRate, threshold, outputFilePath);
            System.out.println("\t\tWrote updated parameters");
            copyTrainPaperList = deepCopyInRandomOrder(trainPaperList);
            showLogLikelihood(params, copyTrainPaperList, modelMap, trainAuthorIdList, negativeSampleSize, regParam);
            if (checkIfConverged(params, preParams, threshold)) {
                System.out.println("\t\tConverged");
                break;
            }
        }
        System.out.println("End:\testimating parameters");
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("LogRegParamEstimator", options, args);
        String trainDirPath = cl.getOptionValue(TRAIN_DIR_OPTION);
        String modelDirPath = cl.getOptionValue(MODEL_DIR_OPTION);
        double randomValueScale = cl.hasOption(RANDOM_VALUE_SCALE_OPTION) ?
                Double.parseDouble(cl.getOptionValue(RANDOM_VALUE_SCALE_OPTION)) : DEFAULT_RANDOM_VALUE_SCALE;
        String[] optionParams = new String[OPTION_PARAM_SIZE];
        optionParams[0] = cl.hasOption(EPOCH_SIZE_OPTION) ?
                cl.getOptionValue(EPOCH_SIZE_OPTION) : String.valueOf(DEFAULT_EPOCH_SIZE);
        optionParams[1] = cl.hasOption(START_IDX_OPTION) ?
                cl.getOptionValue(START_IDX_OPTION) : String.valueOf(DEFAULT_START_IDX_SIZE);
        optionParams[2] = cl.hasOption(NEGATIVE_SAMPLE_SIZE_OPTION) ?
                cl.getOptionValue(NEGATIVE_SAMPLE_SIZE_OPTION) : String.valueOf(DEFAULT_NEGATIVE_SAMPLE_SIZE);
        optionParams[3] = cl.hasOption(BATCH_SIZE_OPTION) ?
                cl.getOptionValue(BATCH_SIZE_OPTION) : String.valueOf(DEFAULT_BATCH_SIZE);
        optionParams[4] = cl.hasOption(REGULATION_PARAM_OPTION) ?
               cl.getOptionValue(REGULATION_PARAM_OPTION) : String.valueOf(DEFAULT_REGULATION_PARAM);
        optionParams[5] = cl.hasOption(LEARNING_RATE_OPTION) ?
                cl.getOptionValue(LEARNING_RATE_OPTION) : String.valueOf(DEFAULT_LEARNING_RATE);
        optionParams[6] = cl.hasOption(THRESHOLD_OPTION) ?
                cl.getOptionValue(THRESHOLD_OPTION) : String.valueOf(DEFAULT_THRESHOLD);
        String outputFilePath = cl.getOptionValue(Config.OUTPUT_FILE_OPTION);
        estimate(trainDirPath, modelDirPath, randomValueScale, optionParams, outputFilePath);
    }
}
