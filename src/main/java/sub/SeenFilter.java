package sub;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Paper;

import java.io.*;
import java.util.List;
import java.util.Set;

public class SeenFilter {
    private static final String TRAIN_DIR_OPTION = "train";
    private static final String TEST_DIR_OPTION = "test";

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(TRAIN_DIR_OPTION, true, true, "[input] training dir", options);
        MiscUtil.setOption(TEST_DIR_OPTION, true, true, "[input] test dir", options);
        MiscUtil.setOption(Config.OUTPUT_DIR_OPTION, true, true, "[output] output dir", options);
        return options;
    }

    private static void filter(File testFile, Set<String> authorIdSet, String outputDirPath) {
        try {
            File outputFile = new File(outputDirPath + "/" + testFile.getName());
            BufferedReader br = new BufferedReader(new FileReader(testFile));
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
            String line;
            while ((line = br.readLine()) != null) {
                Paper paper = new Paper(line);
                if (MiscUtil.checkIfAuthorExists(paper.getAuthorSet(), authorIdSet)) {
                    bw.write(line);
                    bw.newLine();
                }
            }

            bw.close();
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ filter");
            e.printStackTrace();
        }
    }

    private static void filter(String trainingDirPath, String testDirPath, String outputDirPath) {
        List<File> trainingFileList = FileUtil.getFileListR(trainingDirPath);
        Set<String> authorIdSet = MiscUtil.buildAuthorIdSet(trainingFileList);
        List<File> testFileList = FileUtil.getFileList(testDirPath);
        FileUtil.makeDirIfNotExist(outputDirPath);
        for (File testFile : testFileList) {
            filter(testFile, authorIdSet, outputDirPath);
        }
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("SeenFilter", options, args);
        String trainingDirPath = cl.getOptionValue(TRAIN_DIR_OPTION);
        String testDirPath = cl.getOptionValue(TEST_DIR_OPTION);
        String outputDirPath = cl.getOptionValue(Config.OUTPUT_DIR_OPTION);
        filter(trainingDirPath, testDirPath, outputDirPath);
    }
}
