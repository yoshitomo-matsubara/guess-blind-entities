package main;

import Model.BaseModel;
import Model.NaiveBayesModel;
import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import structure.Author;
import structure.Paper;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AuthorEstimator {
    private static final String TRAIN_DIR_OPTION = "train";
    private static final String TEST_DIR_OPTION = "test";
    private static final String MODEL_TYPE_OPTION = "mt";
    private static final String SPLIT_SIZE_OPTION = "s";
    private static final int DEFAULT_SPLIT_SIZE = 3;
    private static final double ZERO_SCORE = 0.0d;

    private static Options setOptions() {
        Options options = new Options();
        options.addOption(Option.builder(TRAIN_DIR_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[input] training dir")
                .build());
        options.addOption(Option.builder(TEST_DIR_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[input] test dir")
                .build());
        options.addOption(Option.builder(SPLIT_SIZE_OPTION)
                .hasArg(true)
                .required(false)
                .desc("[param, optional] split size")
                .build());
        options.addOption(Option.builder(MODEL_TYPE_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[param] model type")
                .build());
        options.addOption(Option.builder(Config.OUTPUT_DIR_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[output] output dir")
                .build());
        return options;
    }

    private static BaseModel selectModel(String modelType, Author author) {
        if (modelType.equals(NaiveBayesModel.TYPE)) {
            return new NaiveBayesModel(author);
        }
        return null;
    }

    public static List<BaseModel> readAuthorFiles(List<File> trainingFileList, String modelType) {
        System.out.println("\tStart:\treading author files");
        List<BaseModel> modelList = new ArrayList<>();
        try {
            for (File trainingFile : trainingFileList) {
                List<String> lineList = new ArrayList<>();
                BufferedReader br = new BufferedReader(new FileReader(trainingFile));
                String line;
                while ((line = br.readLine()) != null) {
                    lineList.add(line);
                }

                br.close();
                Author author = new Author(trainingFile.getName(), lineList);
                BaseModel model = selectModel(modelType, author);
                model.train();
                modelList.add(model);
            }
        } catch (Exception e) {
            System.err.println("Exception @ readAuthorFiles");
            e.printStackTrace();
        }

        System.out.println("\tEnd:\treading author files");
        return modelList;
    }

    private static List<File> moveToSublist(List<File> orgList, int size) {
        List<File> sublist = new ArrayList<>();
        int moveSize = size < orgList.size() ? size : orgList.size();
        for (int i = 0; i < moveSize; i++) {
            sublist.add(orgList.remove(0));
        }
        return sublist;
    }

    private static void estimate(File testFile, List<BaseModel> modelList, boolean first, String outputDirPath) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(testFile));
            String line;
            while ((line = br.readLine()) != null) {
                Paper paper = new Paper(line);
                File outputFile = new File(outputDirPath + "/" + paper.id);
                BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, !first));
                if (first) {
                    bw.write(line);
                    bw.newLine();
                }

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
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ estimate");
            e.printStackTrace();
        }
    }

    private static void estimate(String trainingDirPath, String testDirPath, int splitSize, String modelType, String outputDirPath) {
        List<File> trainingFileList = FileUtil.getFileList(trainingDirPath);
        List<File> testFileList = FileUtil.getFileList(testDirPath);
        int size = trainingFileList.size();
        int unitSize = size / splitSize;
        for (int i = 0; i < splitSize; i++) {
            System.out.println("Stage " + String.valueOf(i + 1) + "/" + String.valueOf(splitSize));
            int subSize = i < splitSize - 1 ? unitSize : trainingFileList.size();
            List<File> subTrainingFileList = moveToSublist(trainingFileList, subSize);
            List<BaseModel> modelList = readAuthorFiles(subTrainingFileList, modelType);
            FileUtil.makeIfNotExist(outputDirPath);
            boolean first = i == 0;
            for (File testFile : testFileList) {
                estimate(testFile, modelList, first, outputDirPath);
            }
        }
    }

    public static void main(String[] args) {
        Options options = setOptions();
        CommandLine cl = MiscUtil.setParams("AuthorEstimator", options, args);
        String trainingDirPath = cl.getOptionValue(TRAIN_DIR_OPTION);
        String testDirPath = cl.getOptionValue(TEST_DIR_OPTION);
        int splitSize = cl.hasOption(SPLIT_SIZE_OPTION) ? Integer.parseInt(cl.getOptionValue(SPLIT_SIZE_OPTION))
                : DEFAULT_SPLIT_SIZE;
        String modelType = cl.getOptionValue(MODEL_TYPE_OPTION);
        String outputDirPath = cl.getOptionValue(Config.OUTPUT_DIR_OPTION);
        estimate(trainingDirPath, testDirPath, splitSize, modelType, outputDirPath);
    }
}
