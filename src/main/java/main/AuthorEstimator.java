package main;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import model.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Pair;
import structure.Paper;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuthorEstimator {
    private static final String MODEL_DIR_OPTION = "model";
    private static final String TEST_DIR_OPTION = "test";
    private static final String MODEL_TYPE_OPTION = "mt";
    private static final String MIN_PAPER_SIZE_OPTION = "mps";
    private static final String START_INDEX_OPTION = "sidx";
    private static final String END_INDEX_OPTION = "eidx";
    private static final int DEFAULT_MIN_PAPER_SIZE = 1;
    private static final int PRINT_UNIT_SIZE = 1000;
    private static final int SUFFIX_SIZE = 3;
    private static final int DEFAULT_START_INDEX = 0;
    private static final int INVALID_INDEX = -1;
    private static final double ZERO_SCORE = 0.0d;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(MODEL_DIR_OPTION, true, true, "[input] model dir", options);
        MiscUtil.setOption(TEST_DIR_OPTION, true, true, "[input] test dir", options);
        MiscUtil.setOption(MODEL_TYPE_OPTION, true, true, "[param] model type", options);
        MiscUtil.setOption(MIN_PAPER_SIZE_OPTION, true, false,
                "[param, optional] minimum number of papers each author requires to have, default = "
                + String.valueOf(DEFAULT_MIN_PAPER_SIZE), options);
        MiscUtil.setOption(START_INDEX_OPTION, true, false,
                "[param, optional] start index (0 - (# of model files - 1))," +
                        "default = " + String.valueOf(DEFAULT_START_INDEX), options);
        MiscUtil.setOption(END_INDEX_OPTION, true, false,
                "[param, optional] end index (0 - # of model files), default = # of model files", options);
        MiscUtil.setOption(Config.OUTPUT_DIR_OPTION, true, true, "[output] output dir", options);
        return options;
    }

    private static void setModelOptions(Options options) {
        RandomModel.setOptions(options);
        NaiveBayesModel.setOptions(options);
        MultiNaiveBayesModel.setOptions(options);
        LogisticRegressionModel.setOptions(options);
    }

    private static BaseModel selectModel(String modelType, String line, CommandLine cl) {
        if (RandomModel.checkIfValid(modelType)) {
            return new RandomModel(line, cl);
        } else if (GeometricMealModel.checkIfValid(modelType)) {
            return new GeometricMealModel(line);
        } else if (CountUpModel.checkIfValid(modelType)) {
            return new CountUpModel(line);
        } else if (NaiveBayesModel.checkIfValid(modelType, cl)) {
            return new NaiveBayesModel(line, cl);
        } else if (MultiNaiveBayesModel.checkIfValid(modelType, cl)) {
            return new MultiNaiveBayesModel(line, cl);
        } else if (LogisticRegressionModel.checkIfValid(modelType, cl)) {
            return new LogisticRegressionModel(line, cl);
        }
        return null;
    }

    public static Pair<Integer, List<BaseModel>> readModelFile(File modelFile, String modelType,
                                                               CommandLine cl, int minPaperSize) {
        System.out.println("\tStart:\treading author files");
        List<BaseModel> modelList = new ArrayList<>();
        int modelCount = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(modelFile));
            String line;
            while ((line = br.readLine()) != null) {
                modelCount++;
                BaseModel model = selectModel(modelType, line, cl);
                if (model.paperIds.length >= minPaperSize) {
                    modelList.add(model);
                }
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ readModelFile");
            e.printStackTrace();
        }

        System.out.println("\tEnd:\treading author files");
        return new Pair<>(modelCount, modelList);
    }

    private static void score(List<String> testPaperLineList, List<BaseModel> modelList,
                              boolean first, String outputDirPath) {
        System.out.println("\tStart:\tscoring");
        try {
            int count = 0;
            for (String testPaperLine : testPaperLineList) {
                Paper paper = new Paper(testPaperLine);
                String suffix = paper.id.substring(paper.id.length() - SUFFIX_SIZE);
                File outputFile = new File(outputDirPath + "/" + suffix + "/" + paper.id);
                FileUtil.makeParentDir(outputFile.getPath());
                BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, !first));
                bw.write(testPaperLine);
                bw.newLine();
                for (BaseModel model : modelList) {
                    double score = model.estimate(paper);
                    if (paper.checkIfAuthor(model.authorId) && score == model.INVALID_VALUE) {
                        score = ZERO_SCORE;
                    }

                    if (score != model.INVALID_VALUE) {
                        bw.write(model.authorId + Config.FIRST_DELIMITER + String.valueOf(score));
                        bw.newLine();
                    }
                }

                bw.close();
                count++;
                if (count % PRINT_UNIT_SIZE == 0) {
                    System.out.println("\t\tscored for " + String.valueOf(count) + " test papers in total");
                }
            }
        } catch (Exception e) {
            System.err.println("Exception @ score");
            e.printStackTrace();
        }
        System.out.println("\tEnd:\tscoring");
    }

    private static void estimate(String modelDirPath, String testDirPath, String modelType,
                                 CommandLine cl, int minPaperSize, int startIdx, int endIdx, String outputDirPath) {
        List<File> testFileList = FileUtil.getFileList(testDirPath);
        List<File> modelFileList = FileUtil.getFileList(modelDirPath);
        Collections.sort(modelFileList);
        List<String> testPaperLineList = new ArrayList<>();
        for (File testFile : testFileList) {
            testPaperLineList.addAll(FileUtil.readFile(testFile));
        }

        int modelCount = 0;
        int availableCount = 0;
        int listSize = modelFileList.size();
        endIdx = endIdx != INVALID_INDEX && endIdx <= listSize ? endIdx : listSize;
        for (int i = startIdx; i < endIdx; i++) {
            File modelFile = modelFileList.remove(startIdx);
            System.out.println("Stage " + String.valueOf(i + 1) + "/" + String.valueOf(listSize));
            Pair<Integer, List<BaseModel>> pair = readModelFile(modelFile, modelType, cl, minPaperSize);
            List<BaseModel> modelList = pair.second;
            modelCount += pair.first;
            availableCount += modelList.size();
            FileUtil.makeDirIfNotExist(outputDirPath);
            boolean first = i == 0;
            score(testPaperLineList, modelList, first, outputDirPath);
        }

        System.out.println(String.valueOf(availableCount) + " available authors");
        System.out.println(String.valueOf(modelCount - availableCount) + " ignored authors");
    }

    public static void main(String[] args) {
        Options options = getOptions();
        setModelOptions(options);
        CommandLine cl = MiscUtil.setParams("AuthorEstimator", options, args);
        String modelDirPath = cl.getOptionValue(MODEL_DIR_OPTION);
        String testDirPath = cl.getOptionValue(TEST_DIR_OPTION);
        String modelType = cl.getOptionValue(MODEL_TYPE_OPTION);
        int minPaperSize = cl.hasOption(MIN_PAPER_SIZE_OPTION) ?
                Integer.parseInt(cl.getOptionValue(MIN_PAPER_SIZE_OPTION)) : DEFAULT_MIN_PAPER_SIZE;
        int startIdx = cl.hasOption(START_INDEX_OPTION) ?
                Integer.parseInt(cl.getOptionValue(START_INDEX_OPTION)) : DEFAULT_START_INDEX;
        int endIdx = cl.hasOption(END_INDEX_OPTION) ?
                Integer.parseInt(cl.getOptionValue(END_INDEX_OPTION)) : INVALID_INDEX;
        String outputDirPath = cl.getOptionValue(Config.OUTPUT_DIR_OPTION);
        estimate(modelDirPath, testDirPath, modelType, cl, minPaperSize, startIdx, endIdx, outputDirPath);
    }
}
