package main;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import model.MultinomialNaiveBayesModel;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Pair;
import structure.Paper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class LogLikelihoodEstimator {
    private static final String MODEL_DIR_OPTION = "model";
    private static final String TEST_DIR_OPTION = "test";
    private static final String MIN_PAPER_SIZE_OPTION = "mps";
    private static final int DEFAULT_MIN_PAPER_SIZE = 1;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(MODEL_DIR_OPTION, true, true, "[input] model dir", options);
        MiscUtil.setOption(TEST_DIR_OPTION, true, true, "[input] test dir", options);
        MiscUtil.setOption(MIN_PAPER_SIZE_OPTION, true, false,
                "[param, optional] minimum number of papers each author requires to have, default = "
                        + String.valueOf(DEFAULT_MIN_PAPER_SIZE), options);
        MiscUtil.setOption(Config.OUTPUT_DIR_OPTION, true, true, "[output] output dir", options);
        return options;
    }

    private static void setModelOptions(Options options) {
        MultinomialNaiveBayesModel.setOptions(options);
    }

    public static Pair<Integer, List<MultinomialNaiveBayesModel>> readModelFile(File modelFile,
                                                                                CommandLine cl, int minPaperSize) {
        System.out.println("\tStart:\treading author files");
        List<MultinomialNaiveBayesModel> modelList = new ArrayList<>();
        int modelCount = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(modelFile));
            String line;
            while ((line = br.readLine()) != null) {
                modelCount++;
                MultinomialNaiveBayesModel model = new MultinomialNaiveBayesModel(line, cl);
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

    private static void buildMaps(File testFile, List<MultinomialNaiveBayesModel> modelList,
                                  HashMap<String, Double> totalProbMap,
                                  HashMap<String, MultinomialNaiveBayesModel> modelMap) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(testFile));
            String line;
            while ((line = br.readLine()) != null) {
                Paper paper = new Paper(line);
                for (MultinomialNaiveBayesModel model : modelList) {
                    double score = model.estimate(paper, true);
                    double totalProb = !totalProbMap.containsKey(paper.id) ? score : score + totalProbMap.get(paper.id);
                    totalProbMap.put(paper.id, totalProb);
                    modelMap.put(model.authorId, model);
                }
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ score");
            e.printStackTrace();
        }
    }

    private static double calcLogLikelihood(File testFile, HashMap<String, Double> totalProbMap,
                              HashMap<String, MultinomialNaiveBayesModel> modelMap) {
        double logLikelihood = 0.0d;
        try {
            BufferedReader br = new BufferedReader(new FileReader(testFile));
            String line;
            while ((line = br.readLine()) != null) {
                Paper paper = new Paper(line);
                HashSet<String> authorIdSet = paper.getAuthorSet();
                Iterator<String> ite = authorIdSet.iterator();
                while (ite.hasNext()) {
                    String authorId = ite.next();
                    if (modelMap.containsKey(authorId)) {
                        MultinomialNaiveBayesModel model = modelMap.get(authorId);
                        logLikelihood += model.estimate(paper) / totalProbMap.get(paper.id);
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ score");
            e.printStackTrace();
        }
        return logLikelihood;
    }

    private static void estimate(String modelDirPath, String testDirPath, CommandLine cl,
                                 int minPaperSize, String outputDirPath) {
        List<File> testFileList = FileUtil.getFileList(testDirPath);
        List<File> modelFileList = FileUtil.getFileList(modelDirPath);
        Collections.sort(modelFileList);
        HashMap<String, Double> totalProbMap = new HashMap<>();
        HashMap<String, MultinomialNaiveBayesModel> modelMap = new HashMap<>();
        int listSize = modelFileList.size();
        for (int i = 0; i < listSize; i++) {
            File modelFile = modelFileList.get(i);
            System.out.println("Stage A " + String.valueOf(i + 1) + "/" + String.valueOf(listSize));
            Pair<Integer, List<MultinomialNaiveBayesModel>> pair = readModelFile(modelFile, cl, minPaperSize);
            List<MultinomialNaiveBayesModel> modelList = pair.value;
            for (File testFile : testFileList) {
                buildMaps(testFile, modelList, totalProbMap, modelMap);
            }
        }

        int modelCount = 0;
        int availableCount = 0;
        double logLikelihood = 0.0d;
        for (int i = 0; i < listSize; i++) {
            File modelFile = modelFileList.remove(0);
            System.out.println("Stage B " + String.valueOf(i + 1) + "/" + String.valueOf(listSize));
            Pair<Integer, List<MultinomialNaiveBayesModel>> pair = readModelFile(modelFile, cl, minPaperSize);
            List<MultinomialNaiveBayesModel> modelList = pair.value;
            modelCount += pair.key;
            availableCount += modelList.size();
            FileUtil.makeDirIfNotExist(outputDirPath);
            for (File testFile : testFileList) {
                logLikelihood += calcLogLikelihood(testFile, totalProbMap, modelMap);
            }
        }

        System.out.println("log-likelihood: " + String.valueOf(logLikelihood));
        System.out.println(String.valueOf(availableCount) + " available authors");
        System.out.println(String.valueOf(modelCount - availableCount) + " ignored authors");
    }

    public static void main(String[] args) {
        Options options = getOptions();
        setModelOptions(options);
        CommandLine cl = MiscUtil.setParams("LogLikelihoodEstimator", options, args);
        String modelDirPath = cl.getOptionValue(MODEL_DIR_OPTION);
        String testDirPath = cl.getOptionValue(TEST_DIR_OPTION);
        int minPaperSize = cl.hasOption(MIN_PAPER_SIZE_OPTION) ?
                Integer.parseInt(cl.getOptionValue(MIN_PAPER_SIZE_OPTION)) : DEFAULT_MIN_PAPER_SIZE;
        String outputDirPath = cl.getOptionValue(Config.OUTPUT_DIR_OPTION);
        estimate(modelDirPath, testDirPath, cl, minPaperSize, outputDirPath);
    }
}
