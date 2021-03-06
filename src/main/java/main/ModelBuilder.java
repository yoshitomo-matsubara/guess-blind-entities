package main;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import model.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Author;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class ModelBuilder {
    private static final String TRAIN_DIR_OPTION = "train";
    private static final String MODEL_TYPE_OPTION = "mt";
    private static final String MIN_PAPER_SIZE_OPTION = "mps";
    private static final int DEFAULT_MIN_PAPER_SIZE = 1;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(TRAIN_DIR_OPTION, true, true, "[input] training dir", options);
        MiscUtil.setOption(MODEL_TYPE_OPTION, true, true, "[param] model type", options);
        MiscUtil.setOption(MIN_PAPER_SIZE_OPTION, true, false,
                "[param, optional] minimum number of papers each author requires to have, default = "
                + String.valueOf(DEFAULT_MIN_PAPER_SIZE), options);
        MiscUtil.setOption(Config.OUTPUT_DIR_OPTION, true, true, "[output] output dir", options);
        return options;
    }

    private static void setModelOptions(Options options) {
        HillProvostBestModel.setOptions(options);
        MultiNaiveBayesModel.setOptions(options);
    }

    private static BaseModel selectModel(String modelType, Author author, CommandLine cl) {
        if (RandomModel.checkIfValid(modelType)) {
            return new RandomModel(author, cl);
        } else if (HillProvostBestModel.checkIfValid(modelType, cl)) {
            return new HillProvostBestModel(author, cl);
        } else if (CommonCitationModel.checkIfValid(modelType, cl)) {
            return new CommonCitationModel(author, cl);
        } else if (SocialCitationModel.checkIfValid(modelType, cl)) {
            return new SocialCitationModel(author, cl);
        } else if (MultiNaiveBayesModel.checkIfValid(modelType, cl)) {
            return new MultiNaiveBayesModel(author, cl);
        } else if (LogisticRegressionModel.checkIfValid(modelType)) {
            return new LogisticRegressionModel(author, cl);
        }
        return null;
    }

    private static List<BaseModel> readAuthorFiles(List<File> trainingFileList, String modelType,
                                                  CommandLine cl, int minPaperSize) {
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
                if (author.papers.length >= minPaperSize) {
                    BaseModel model = selectModel(modelType, author, cl);
                    model.train();
                    modelList.add(model);
                }
            }
        } catch (Exception e) {
            System.err.println("Exception @ readAuthorFiles");
            e.printStackTrace();
        }

        System.out.println("\tEnd:\treading author files");
        return modelList;
    }

    private static void writeModelFile(List<BaseModel> modelList, String outputFilePath) {
        FileUtil.makeParentDir(outputFilePath);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilePath)));
            int size = modelList.size();
            for (int i = 0; i < size; i++) {
                BaseModel model = modelList.remove(0);
                bw.write(model.toString());
                bw.newLine();
            }
            bw.close();
        } catch (Exception e) {
            System.err.println("Exception @ writeModelFile");
            e.printStackTrace();
        }
    }

    private static void build(String trainingDirPath, String modelType,
                                 CommandLine cl, int minPaperSize, String outputDirPath) {
        List<File> authorDirList = FileUtil.getDirList(trainingDirPath);
        if (authorDirList.size() == 0) {
            authorDirList.add(new File(trainingDirPath));
        }

        int fileCount = 0;
        int availableCount = 0;
        int dirSize = authorDirList.size();
        List<BaseModel> allModelList = new ArrayList<>();
        Map<String, List<BaseModel>> modelListMap = new HashMap<>();
        for (int i = 0; i < dirSize; i++) {
            File authorDir = authorDirList.remove(0);
            System.out.println("Stage " + String.valueOf(i + 1) + "/" + String.valueOf(dirSize));
            List<File> trainingFileList = FileUtil.getFileListR(authorDir.getPath());
            fileCount += trainingFileList.size();
            List<BaseModel> modelList = readAuthorFiles(trainingFileList, modelType, cl, minPaperSize);
            modelListMap.put(authorDir.getName(), modelList);
            allModelList.addAll(modelList);
            availableCount += modelList.size();
            trainingFileList.clear();
            if (!HillProvostBestModel.checkIfValid(modelType, cl) && !SocialCitationModel.checkIfValid(modelType, cl)
                    && !CommonCitationModel.checkIfValid(modelType, cl)
                    && !LogisticRegressionModel.checkIfValid(modelType)) {
                writeModelFile(modelList, outputDirPath + authorDir.getName());
            }
        }

        if (HillProvostBestModel.checkIfValid(modelType, cl) || SocialCitationModel.checkIfValid(modelType, cl)
                || CommonCitationModel.checkIfValid(modelType, cl) || LogisticRegressionModel.checkIfValid(modelType)) {
            if (SocialCitationModel.checkIfValid(modelType) || LogisticRegressionModel.checkIfValid(modelType)) {
                Map<String, Integer> modelIdMap = new HashMap<>();
                int allSize = allModelList.size();
                for (int i = 0; i < allSize; i++) {
                    BaseModel model = allModelList.get(i);
                    modelIdMap.put(model.authorId, i);
                }

                for (BaseModel model : allModelList) {
                    model.setSocialPaperIds(allModelList, modelIdMap);
                }
            }

            if (HillProvostBestModel.checkIfValid(modelType, cl) || SocialCitationModel.checkIfValid(modelType, cl)
                    || CommonCitationModel.checkIfValid(modelType, cl)
                    || LogisticRegressionModel.checkIfValid(modelType)) {
                Map<String, Integer> totalCitationCountMap = new HashMap<>();
                for (BaseModel model : allModelList) {
                    model.shareCitationCounts(totalCitationCountMap);
                }

                for (BaseModel model : allModelList) {
                    model.setInverseCitationFrequencyWeights(totalCitationCountMap);
                }
            }

            availableCount = 0;
            for (String key : modelListMap.keySet()) {
                List<BaseModel> modelList = modelListMap.get(key);
                int size = modelList.size();
                for (int i = 0; i < size; i++) {
                    BaseModel model = modelList.remove(0);
                    if ((SocialCitationModel.checkIfValid(modelType) == model.getSocialCitationIdSize() > 0)
                            || !SocialCitationModel.checkIfValid(modelType)) {
                        modelList.add(model);
                    }
                }

                availableCount += modelList.size();
                writeModelFile(modelList, outputDirPath + key);
            }
        }

        System.out.println(String.valueOf(availableCount) + " available authors");
        System.out.println(String.valueOf(fileCount - availableCount) + " ignored authors");
    }

    public static void main(String[] args) {
        Options options = getOptions();
        setModelOptions(options);
        CommandLine cl = MiscUtil.setParams("ModelBuilder", options, args);
        String trainingDirPath = cl.getOptionValue(TRAIN_DIR_OPTION);
        String modelType = cl.getOptionValue(MODEL_TYPE_OPTION);
        int minPaperSize = cl.hasOption(MIN_PAPER_SIZE_OPTION) ?
                Integer.parseInt(cl.getOptionValue(MIN_PAPER_SIZE_OPTION)) : DEFAULT_MIN_PAPER_SIZE;
        String outputDirPath = cl.getOptionValue(Config.OUTPUT_DIR_OPTION);
        build(trainingDirPath, modelType, cl, minPaperSize, outputDirPath);
    }
}
