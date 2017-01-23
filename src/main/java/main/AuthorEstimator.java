package main;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import model.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import structure.Author;
import structure.Paper;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AuthorEstimator {
    private static final String TRAIN_DIR_OPTION = "train";
    private static final String TEST_DIR_OPTION = "test";
    private static final String MODEL_TYPE_OPTION = "mt";
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

    private static void setModelOptions(Options options) {
        NaiveBayesModel.setOptions(options);
    }

    private static BaseModel selectModel(String modelType, Author author, CommandLine cl) {
        if (RandomModel.checkIfValid(modelType)) {
            return new RandomModel(author);
        } else if (GeometricMealModel.checkIfValid(modelType)) {
            return new GeometricMealModel(author);
        } else if (CountUpModel.checkIfValid(modelType)) {
            return new CountUpModel(author);
        } else if (NaiveBayesModel.checkIfValid(modelType, cl)) {
            return new NaiveBayesModel(author, cl);
        }
        return null;
    }

    public static List<BaseModel> readAuthorFiles(List<File> trainingFileList, String modelType, CommandLine cl) {
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
                BaseModel model = selectModel(modelType, author, cl);
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

    private static void estimate(String trainingDirPath, String testDirPath,
                                 String modelType, CommandLine cl, String outputDirPath) {
        List<File> testFileList = FileUtil.getFileList(testDirPath);
        List<File> authorDirList = FileUtil.getDirList(trainingDirPath);
        if (authorDirList.size() == 0) {
            authorDirList.add(new File(trainingDirPath));
        }

        int dirSize = authorDirList.size();
        for (int i = 0; i < dirSize; i++) {
            File authorDir = authorDirList.remove(0);
            System.out.println("Stage " + String.valueOf(i + 1) + "/" + String.valueOf(dirSize));
            List<File> trainingFileList = FileUtil.getFileListR(authorDir.getPath());
            List<BaseModel> modelList = readAuthorFiles(trainingFileList, modelType, cl);
            trainingFileList.clear();
            FileUtil.makeIfNotExist(outputDirPath);
            boolean first = i == 0;
            for (File testFile : testFileList) {
                estimate(testFile, modelList, first, outputDirPath);
            }
        }
    }

    public static void main(String[] args) {
        Options options = setOptions();
        setModelOptions(options);
        CommandLine cl = MiscUtil.setParams("AuthorEstimator", options, args);
        String trainingDirPath = cl.getOptionValue(TRAIN_DIR_OPTION);
        String testDirPath = cl.getOptionValue(TEST_DIR_OPTION);
        String modelType = cl.getOptionValue(MODEL_TYPE_OPTION);
        String outputDirPath = cl.getOptionValue(Config.OUTPUT_DIR_OPTION);
        estimate(trainingDirPath, testDirPath, modelType, cl, outputDirPath);
    }
}
