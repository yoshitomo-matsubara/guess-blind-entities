package sub;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import model.HillProvostBestModel;
import model.LogisticRegressionModel;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Paper;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeatureExtractor {
    public static final String INPUT_PAPER_OPTION = "ip";
    public static final String INPUT_MODEL_OPTION = "im";
    public static final String TRAIN_OPTION = "tr";
    public static final String OUTPUT_DIR_OPTION = "o";
    private static final int SUFFIX_SIZE = 3;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(INPUT_MODEL_OPTION, true, true,
                "[input] input model dir", options);
        MiscUtil.setOption(INPUT_PAPER_OPTION, true, true,
                "[input] input paper dir", options);
        MiscUtil.setOption(TRAIN_OPTION, false, false,
                "[param, optional] training flag", options);
        MiscUtil.setOption(OUTPUT_DIR_OPTION, true, true,
                "[output] output dir", options);
        return options;
    }

    private static void setModelOptions(Options options) {
        HillProvostBestModel.setOptions(options);
    }

    private static List<Paper> getPaperList(String inputPaperDirPath) {
        List<File> paperFileList = FileUtil.getFileList(inputPaperDirPath);
        List<Paper> paperList = new ArrayList<>();
        try {
            for (File paperFile : paperFileList) {
                BufferedReader br = new BufferedReader(new FileReader(paperFile));
                String line;
                while ((line = br.readLine()) != null) {
                    paperList.add(new Paper(line));
                }
                br.close();
            }
        } catch (Exception e) {
            System.err.println("Exception @ getPaperList");
            e.printStackTrace();
        }
        return paperList;
    }

    private static String convertFeatureToString(Paper paper, LogisticRegressionModel model, boolean trainingMode) {
        double[] features = LogisticRegressionModel.extractFeatureValues(model, paper);
        if (!LogisticRegressionModel.checkIfValidValues(features)) {
            return null;
        }

        String label = trainingMode ? (model.checkIfMyPaper(paper.id)? Config.POS_TRAIN_LABEL : Config.NEG_TRAIN_LABEL)
                : Config.TEST_LABEL;
        String str = trainingMode ? label + Config.FIRST_DELIMITER + paper.id + Config.FIRST_DELIMITER + model.authorId
                : label + Config.FIRST_DELIMITER + model.authorId;
        StringBuilder sb = new StringBuilder(str);
        for (int i = 1; i < features.length; i++) {
            sb.append(Config.FIRST_DELIMITER + String.valueOf(features[i]));
        }

        sb.append(Config.FIRST_DELIMITER + String.valueOf(model.paperIds.length) + Config.FIRST_DELIMITER
                + String.valueOf(model.getCitationIdSize()) + Config.FIRST_DELIMITER
                + String.valueOf(model.getTotalCitationCount()) + Config.FIRST_DELIMITER
                + String.valueOf(model.getCoauthorIdSize()) + Config.FIRST_DELIMITER
                + String.valueOf(model.getSocialCitationIdSize()));
        return sb.toString();
    }

    private static void extractTrainFeature(File modelFile, List<Paper> paperList, String outputDirPath) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(modelFile));
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputDirPath + modelFile.getName())));
            String line;
            while ((line = br.readLine()) != null) {
                LogisticRegressionModel model = new LogisticRegressionModel(line);
                for (Paper paper : paperList) {
                    String vecStr = convertFeatureToString(paper, model, true);
                    if (vecStr != null) {
                        bw.write(vecStr);
                        bw.newLine();
                    }
                }
            }

            bw.close();
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ extractTrainFeature");
            e.printStackTrace();
        }
    }

    private static void extractTestFeature(File modelFile, List<Paper> paperList, boolean first, String outputDirPath) {
        try {
            Map<String, List<String>> outputLineListMap = new HashMap<>();
            BufferedReader br = new BufferedReader(new FileReader(modelFile));
            String line;
            while ((line = br.readLine()) != null) {
                LogisticRegressionModel model = new LogisticRegressionModel(line);
                for (Paper paper : paperList) {
                    if (!outputLineListMap.containsKey(paper.id)) {
                        List<String> outputLineList = new ArrayList<>();
                        if (first) {
                            outputLineList.add(paper.toString());
                        }
                        outputLineListMap.put(paper.id, outputLineList);
                    }

                    String vecStr = convertFeatureToString(paper, model, false);
                    if (vecStr != null) {
                        outputLineListMap.get(paper.id).add(vecStr);
                    }
                }
            }

            br.close();
            for (String paperId : outputLineListMap.keySet()) {
                String suffix = paperId.substring(paperId.length() - SUFFIX_SIZE);
                List<String> outputLineList = outputLineListMap.get(paperId);
                FileUtil.overwriteFile(outputLineList, first, outputDirPath + "/" + suffix + "/" + paperId);
            }
        } catch (Exception e) {
            System.err.println("Exception @ extractTestFeature");
            e.printStackTrace();
        }
    }

    private static void extract(String inputPaperDirPath, String inputModelDirPath, boolean trainingMode,
                                String outputDirPath) {
        List<Paper> paperList = getPaperList(inputPaperDirPath);
        List<File> modelFileList = FileUtil.getFileList(inputModelDirPath);
        FileUtil.makeDirIfNotExist(outputDirPath);
        int modelFileSize = modelFileList.size();
        for (int i = 0; i < modelFileSize; i++) {
            File modelFile = modelFileList.remove(0);
            System.out.println("Stage " + String.valueOf(i + 1) + "/" + String.valueOf(modelFileSize));
            if (trainingMode) {
                extractTrainFeature(modelFile, paperList, outputDirPath);
            } else {
                extractTestFeature(modelFile, paperList, i == 0, outputDirPath);
            }
        }
    }

    public static void main(String[] args) {
        Options options = getOptions();
        setModelOptions(options);
        CommandLine cl = MiscUtil.setParams("FeatureExtractor", options, args);
        String inputPaperDirPath = cl.getOptionValue(INPUT_PAPER_OPTION);
        String inputModelDirPath = cl.getOptionValue(INPUT_MODEL_OPTION);
        boolean trainingMode = cl.hasOption(TRAIN_OPTION);
        String outputDirPath = cl.getOptionValue(OUTPUT_DIR_OPTION);
        extract(inputPaperDirPath, inputModelDirPath, trainingMode, outputDirPath);
    }
}
