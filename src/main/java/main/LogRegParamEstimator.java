package main;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import model.CountUpModel;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Paper;

import java.io.*;
import java.util.*;

public class LogRegParamEstimator {
    private static final String TRAIN_DIR_OPTION = "train";
    private static final String MODEL_DIR_OPTION = "model";
    private static final String EPOCH_SIZE_OPTION = "epoch";
    private static final String START_IDX_OPTION = "sidx";
    private static final String BATCH_SIZE_OPTION = "bsize";
    private static final String REGULATION_PARAM_OPTION = "rparam";
    private static final String LEARNING_RATE_OPTION = "lrate";
    private static final int PARAM_SIZE = 9;
    private static final int DEFAULT_EPOCH_SIZE = 100;
    private static final int DEFAULT_START_IDX_SIZE = 0;
    private static final int DEFAULT_BATCH_SIZE = 5000;
    private static final double RANDOM_VALUE_RANGE = 1e-100d;
    private static final double DEFAULT_REGULATION_PARAM = 1e-3d;
    private static final double DEFAULT_LEARNING_RATE = 1e-10d;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(TRAIN_DIR_OPTION, true, true, "[input] train dir", options);
        MiscUtil.setOption(MODEL_DIR_OPTION, true, true, "[input] model dir", options);
        MiscUtil.setOption(EPOCH_SIZE_OPTION, true, false, "[param, optional] epoch size", options);
        MiscUtil.setOption(START_IDX_OPTION, true, false, "[param, optional] start index (epoch)", options);
        MiscUtil.setOption(BATCH_SIZE_OPTION, true, false, "[param, optional] batch size", options);
        MiscUtil.setOption(REGULATION_PARAM_OPTION, true, false, "[param, optional] regulation parameter", options);
        MiscUtil.setOption(LEARNING_RATE_OPTION, true, false, "[param, optional] learning rate", options);
        MiscUtil.setOption(Config.OUTPUT_FILE_OPTION, true, true, "[output] output file", options);
        return options;
    }

    private static double[] loadParams(File file) {
        double[] params = new double[PARAM_SIZE];
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            int i = 0;
            while ((line = br.readLine()) != null) {
                if (i == 0) {
                    continue;
                }

                params[i - 1] = Double.parseDouble(line);
                i++;
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ loadParams");
            e.printStackTrace();
        }
        return params;
    }

    private static double[] initParams(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            return loadParams(file);
        }

        double[] params = new double[PARAM_SIZE];
        Random rand = new Random();
        for (int i = 0; i < params.length; i++) {
            params[i] = (rand.nextDouble() - 0.5d) * RANDOM_VALUE_RANGE;
        }
        return params;
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

    private static double[] calcPairValues(CountUpModel model, Paper paper) {
        int[] counts = model.calcCounts(paper);
        double countUpScore = (double) counts[0] / (double) model.getTotalCitationCount();
        double authorRefCoverage = (double) counts[1] / (double) model.getCitationIdSize();
        double paperAvgRefHitCount = (double) counts[0] / (double) paper.refPaperIds.length;
        double paperRefCoverage = (double) counts[1] / (double) paper.refPaperIds.length;
        return new double[]{countUpScore, authorRefCoverage, paperAvgRefHitCount, paperRefCoverage};
    }

    private static double[] extractFeatureValues(CountUpModel model, Paper paper) {
        double[] featureValues = new double[PARAM_SIZE];
        featureValues[0] = 1.0d;
        // author's attributes
        featureValues[1] = (double) model.paperIds.length;
        featureValues[2] = (double) model.getCitationIdSize();
        featureValues[3] = (double) model.getTotalCitationCount();
        // paper's attribute
        featureValues[4] = (double) paper.refPaperIds.length;
        // attributes from a pair of author and paper
        double[] pairValues = calcPairValues(model, paper);
        for (int i = 0; i < pairValues.length; i++) {
            featureValues[i + 5] = pairValues[i];
        }
        return featureValues;
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
                double[] featureValues = extractFeatureValues(modelMap.get(trainAuthorId), paper);
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

                double[] featureValues = extractFeatureValues(modelMap.get(authorId), paper);
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

    private static void writeUpdatedParams(double[] params, int epoch, String outputFilePath) {
        try {
            FileUtil.makeParentDir(outputFilePath);
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilePath)));
            bw.write("Epoch\t" + String.valueOf(epoch));
            bw.newLine();
            for (int i = 0; i < params.length; i++) {
                bw.write(String.valueOf(params[i]));
                bw.newLine();
            }
            bw.close();
        } catch (Exception e) {
            System.err.println("Exception @ writeUpdatedParams");
            e.printStackTrace();
        }
    }

    private static void estimate(String trainDirPath, String modelDirPath, int epochSize, int startIdx,
                                 int batchSize, double regParam, double learnRate, String outputFilePath) {
        double[] params = initParams(outputFilePath);
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

            writeUpdatedParams(params, i, outputFilePath);
            System.out.println("\t\tWrote updated parameters");
        }
        System.out.println("End:\testimating parameters");
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("LogRegParamEstimator", options, args);
        String trainDirPath = cl.getOptionValue(TRAIN_DIR_OPTION);
        String modelDirPath = cl.getOptionValue(MODEL_DIR_OPTION);
        int epochSize = cl.hasOption(EPOCH_SIZE_OPTION) ?
                Integer.parseInt(cl.getOptionValue(EPOCH_SIZE_OPTION)) : DEFAULT_EPOCH_SIZE;
        int startIdx = cl.hasOption(START_IDX_OPTION) ?
                Integer.parseInt(cl.getOptionValue(START_IDX_OPTION)) : DEFAULT_START_IDX_SIZE;
        int batchSize = cl.hasOption(BATCH_SIZE_OPTION) ?
                Integer.parseInt(cl.getOptionValue(BATCH_SIZE_OPTION)) : DEFAULT_BATCH_SIZE;
        double regParam = cl.hasOption(REGULATION_PARAM_OPTION) ?
                Double.parseDouble(cl.getOptionValue(REGULATION_PARAM_OPTION)) : DEFAULT_REGULATION_PARAM;
        double learnRate = cl.hasOption(LEARNING_RATE_OPTION) ?
                Double.parseDouble(cl.getOptionValue(LEARNING_RATE_OPTION)) : DEFAULT_LEARNING_RATE;
        String outputFilePath = cl.getOptionValue(Config.OUTPUT_FILE_OPTION);
        estimate(trainDirPath, modelDirPath, epochSize, startIdx, batchSize, regParam, learnRate, outputFilePath);
    }
}
