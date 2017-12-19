package sub;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.*;

public class Collector {
    private static final String INPUT_DIR_OPTION = "input";
    private static final String SHUFFLE_SIZE_OPTION = "shuffle";
    private static final String KEY_INDEX_OPTION = "key";
    private static final String STEP_SIZE_OPTION = "step";
    private static final String OUTPUT_DIR_OPTION = "output";
    private static final int INVALID_VALUE = -1;
    private static final int DEFAULT_STEP_SIZE = 4;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(INPUT_DIR_OPTION, true, true, "[param] input dir", options);
        MiscUtil.setOption(SHUFFLE_SIZE_OPTION, true, false, "[param, optional] shuffle size", options);
        MiscUtil.setOption(KEY_INDEX_OPTION, true, false, "[param, optional] key index", options);
        MiscUtil.setOption(STEP_SIZE_OPTION, true, false, "[param, optional] step size", options);
        MiscUtil.setOption(OUTPUT_DIR_OPTION, true, true, "[param] output dir", options);
        return options;
    }

    private static void shuffle(List<File> inputFileList, int shuffleSize, int stepSize, String outputDirPath) {
        Collections.shuffle(inputFileList);
        DecimalFormat decimalFormat = new DecimalFormat("0000");
        Random rand = new Random();
        try {
            boolean first = true;
            int fileSize = inputFileList.size();
            Map<Integer, List<String>> lineListMap = new HashMap<>();
            for (int i = 0; i < fileSize; i++) {
                BufferedReader br = new BufferedReader(new FileReader(inputFileList.remove(0)));
                String line;
                while ((line = br.readLine()) != null) {
                    int randValue = rand.nextInt(shuffleSize);
                    if (!lineListMap.containsKey(randValue)) {
                        lineListMap.put(randValue, new ArrayList<>());
                    }
                    lineListMap.get(randValue).add(line);
                }

                br.close();
                if ((i > 0 && i % stepSize == 0) || i == fileSize - 1) {
                    for (int key : lineListMap.keySet()) {
                        String fileName = decimalFormat.format(key);
                        FileUtil.overwriteFile(lineListMap.get(key), first,outputDirPath + "/" + fileName);
                    }

                    lineListMap.clear();
                    first = false;
                }
            }
        } catch (Exception e) {
            System.err.println("Exception @ shuffle");
            e.printStackTrace();
        }
    }

    private static void sort(List<File> inputFileList, int keyIndex, String outputDirPath) {
        try {
            int fileSize = inputFileList.size();
            Map<String, List<String>> lineListMap = new HashMap<>();
            for (int i = 0; i < fileSize; i++) {
                BufferedReader br = new BufferedReader(new FileReader(inputFileList.remove(0)));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] elements = line.split(Config.FIRST_DELIMITER);
                    if (!lineListMap.containsKey(elements[keyIndex])) {
                        lineListMap.put(elements[keyIndex], new ArrayList<>());
                    }
                    lineListMap.get(elements[keyIndex]).add(line);
                }
                br.close();
            }

            for (String fileName : lineListMap.keySet()) {
                FileUtil.writeFile(lineListMap.get(fileName),outputDirPath + "/" + fileName);
            }
        } catch (Exception e) {
            System.err.println("Exception @ sort");
            e.printStackTrace();
        }
    }

    private static void collect(String inputDirPath, int shuffleSize, int keyIndex,
                                int stepSize, String outputDirPath) {
        List<File> inputFileList = FileUtil.getFileList(inputDirPath);
        if (shuffleSize > 0 && keyIndex == INVALID_VALUE) {
            shuffle(inputFileList, shuffleSize, stepSize, outputDirPath);
        } else if (keyIndex >= 0 && shuffleSize == INVALID_VALUE) {
            sort(inputFileList, keyIndex, outputDirPath);
        }
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("Collector", options, args);
        String inputDirPath = cl.getOptionValue(INPUT_DIR_OPTION);
        int shuffleSize = cl.hasOption(SHUFFLE_SIZE_OPTION) ?
                Integer.parseInt(cl.getOptionValue(SHUFFLE_SIZE_OPTION)) : INVALID_VALUE;
        int keyIndex = cl.hasOption(KEY_INDEX_OPTION) ?
                Integer.parseInt(cl.getOptionValue(KEY_INDEX_OPTION)) : INVALID_VALUE;
        int stepSize = cl.hasOption(STEP_SIZE_OPTION) ?
                Integer.parseInt(cl.getOptionValue(STEP_SIZE_OPTION)) : DEFAULT_STEP_SIZE;
        String outputDirPath = cl.getOptionValue(OUTPUT_DIR_OPTION);
        collect(inputDirPath, shuffleSize, keyIndex, stepSize, outputDirPath);
    }
}
