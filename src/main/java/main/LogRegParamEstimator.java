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
    private static final String REGULATION_PARAM_OPTION = "rparam";
    private static final String LEARNING_RATE_OPTION = "lrate";
    private static final int PARAM_SIZE = LogisticRegressionModel.PARAM_SIZE;
    private static final int OPTION_PARAM_SIZE = 5;
    private static final int DEFAULT_EPOCH_SIZE = 50;
    private static final int DEFAULT_START_IDX_SIZE = 0;
    private static final int DEFAULT_BATCH_SIZE = 5000;
    private static final double DEFAULT_RANDOM_VALUE_SCALE = 1e-5d;
    private static final double DEFAULT_REGULATION_PARAM = 1e-3d;
    private static final double DEFAULT_LEARNING_RATE = 1e-10d;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(TRAIN_DIR_OPTION, true, true, "[input] train dir", options);
        MiscUtil.setOption(MODEL_DIR_OPTION, true, true, "[input] model dir", options);
        MiscUtil.setOption(RANDOM_VALUE_SCALE_OPTION, true, false, "[param, optional] random value scale", options);
        MiscUtil.setOption(EPOCH_SIZE_OPTION, true, false, "[param, optional] epoch size", options);
        MiscUtil.setOption(START_IDX_OPTION, true, false, "[param, optional] start index (epoch)", options);
        MiscUtil.setOption(BATCH_SIZE_OPTION, true, false, "[param, optional] batch size", options);
        MiscUtil.setOption(REGULATION_PARAM_OPTION, true, false, "[param, optional] regulation parameter", options);
        MiscUtil.setOption(LEARNING_RATE_OPTION, true, false, "[param, optional] learning rate", options);
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
            while ((line = br.readLine()) != null) {
                startIdx++;
            }

            optionParamList.add(String.valueOf(startIdx));
            String[] elements = line.split(Config.FIRST_DELIMITER);
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
        Pair<List<Double>, List<String>> paramListPair = file.exists() && file.isFile()? loadParams(file) : null;
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

    private static HashMap<String, CountUpModel> readModelFiles(String modelDirPath) {
        HashMap<String, CountUpModel> modelMap = new HashMap<>();
        List<File> modelFileList = FileUtil.getFileList(modelDirPath);
        System.out.println("Start:\treading model files");
        try {
            for (File modelFile : modelFileList) {
                BufferedReader br = new BufferedReader(new FileReader(modelFile));
                String line;
                while ((line = br.readLine()) != null) {
                    CountUpModel model = new CountUpModel(line);
                    modelMap.put(model.authorId, model);
                }
                br.close();
            }
        } catch (Exception e) {
            System.err.println("Exception @ readModelFiles");
            e.printStackTrace();
        }

        System.out.println("End:\treading model files");
        return modelMap;
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

    private static double[] updateParams(double[] params, List<Paper> batchPaperList,
                                         HashMap<String, CountUpModel> modelMap, double regParam, double learnRate) {
        double[] updatedParams = MiscUtil.initDoubleArray(params.length, 0.0d);
        double[] gradParams = MiscUtil.initDoubleArray(params.length, 0.0d);
        int count = 0;
        while (batchPaperList.size() > 0) {
            Paper paper = batchPaperList.remove(0);
            double denominator = 0.0d;
            double[] numerators = MiscUtil.initDoubleArray(params.length, 0.0d);
            for (String trainAuthorId : modelMap.keySet()) {
                double[] featureValues = LogisticRegressionModel.extractFeatureValues(modelMap.get(trainAuthorId), paper);
                double ip = calcInnerProduct(params, featureValues);
                double expVal = Math.exp(ip);
                denominator += expVal;
                for (int i = 0; i < numerators.length; i++) {
                    numerators[i] += featureValues[i] * expVal;
                }
            }

            Iterator<String> ite = paper.getAuthorSet().iterator();
            while (ite.hasNext()) {
                String authorId = ite.next();
                if (!modelMap.containsKey(authorId)) {
                    continue;
                }

                double[] featureValues = LogisticRegressionModel.extractFeatureValues(modelMap.get(authorId), paper);
                for (int i = 0; i < gradParams.length; i++) {
                    gradParams[i] += featureValues[i] - numerators[i] / denominator;
                }
                count++;
            }
        }

        for (int i = 0; i < params.length; i++) {
            gradParams[i] = gradParams[i] / (double) count - 2.0d * regParam * params[i];
            updatedParams[i] = params[i] + learnRate * gradParams[i];
        }
        return updatedParams;
    }

    private static void writeUpdatedParams(double[] params, int epochSize, int batchSize, double regParam,
                                           double learnRate, String outputFilePath) {
        try {
            FileUtil.makeParentDir(outputFilePath);
            File outputFile = new File(outputFilePath);
            boolean appendMode = outputFile.exists();
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, appendMode));
            if (!appendMode) {
                bw.write("Epoch size\t" + String.valueOf(epochSize) + "\tBatch size\t" + String.valueOf(batchSize)
                        + "\tRegulation param\t" + String.valueOf(regParam)
                        + "\tLearning rate\t" + String.valueOf(learnRate));
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

    private static void estimate(String trainDirPath, String modelDirPath, double randomValueScale,
                                 String[] optionParams, String outputFilePath) {
        double[] params = new double[PARAM_SIZE];
        initParams(outputFilePath, params, randomValueScale, optionParams);
        int epochSize = Integer.parseInt(optionParams[0]);
        int batchSize = Integer.parseInt(optionParams[1]);
        double regParam = Double.parseDouble(optionParams[2]);
        double learnRate = Double.parseDouble(optionParams[3]);
        int startIdx = Integer.parseInt(optionParams[4]);
        List<Paper> trainPaperList = readPaperFiles(trainDirPath);
        HashMap<String, CountUpModel> modelMap = readModelFiles(modelDirPath);
        int t = 0;
        System.out.println("Start:\testimating parameters");
        for (int i = startIdx; i < epochSize; i++) {
            System.out.println("\tEpoch " + String.valueOf(i + 1) + "/" + String.valueOf(epochSize));
            List<Paper> copyTrainPaperList = deepCopyInRandomOrder(trainPaperList);
            while (copyTrainPaperList.size() > 0) {
                t++;
                List<Paper> batchPaperList = new ArrayList<>();
                int size = copyTrainPaperList.size() > batchSize ? batchSize : copyTrainPaperList.size();
                for (int j = 0; j < size; j++) {
                    batchPaperList.add(copyTrainPaperList.remove(0));
                }
                params = updateParams(params, batchPaperList, modelMap, regParam, learnRate / (double) t);
            }

            writeUpdatedParams(params, epochSize, batchSize, regParam, learnRate, outputFilePath);
            System.out.println("\t\tWrote updated parameters");
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
        optionParams[1] = cl.hasOption(BATCH_SIZE_OPTION) ?
                cl.getOptionValue(BATCH_SIZE_OPTION) : String.valueOf(DEFAULT_BATCH_SIZE);
        optionParams[2] = cl.hasOption(REGULATION_PARAM_OPTION) ?
               cl.getOptionValue(REGULATION_PARAM_OPTION) : String.valueOf(DEFAULT_REGULATION_PARAM);
        optionParams[3] = cl.hasOption(LEARNING_RATE_OPTION) ?
                cl.getOptionValue(LEARNING_RATE_OPTION) : String.valueOf(DEFAULT_LEARNING_RATE);
        optionParams[4] = cl.hasOption(START_IDX_OPTION) ?
                cl.getOptionValue(START_IDX_OPTION) : String.valueOf(DEFAULT_START_IDX_SIZE);
        String outputFilePath = cl.getOptionValue(Config.OUTPUT_FILE_OPTION);
        estimate(trainDirPath, modelDirPath, randomValueScale, optionParams, outputFilePath);
    }
}
