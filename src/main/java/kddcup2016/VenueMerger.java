package kddcup2016;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.*;
import java.util.*;

public class VenueMerger {
    private static final String PAPERS_FILE_OPTION = "p";
    private static final String PAPER_KEYWORDS_FILE_OPTION = "k";
    private static final String TMP_PAPERS_FILE_PREFIX = "tmp-p-";
    private static final String TMP_PAPER_KEYWORDS_FILE_PREFIX = "tmp-k-";
    private static final int PAPER_ID_INDEX = 0;
    private static final int PUBLISHER_ID_INDEX = 1;
    private static final int FIELD_ID_INDEX = 2;
    private static final int PREFIX_SIZE = 2;
    private static final int BUFFER_SIZE = 5000000;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(PAPERS_FILE_OPTION, true, true, "[input] min-Papers file", options);
        MiscUtil.setOption(PAPER_KEYWORDS_FILE_OPTION, true, true, "[input] min-PaperKeywords file", options);
        MiscUtil.setOption(Config.TMP_DIR_OPTION, true, false, "[output, optional] temporary output dir", options);
        MiscUtil.setOption(Config.OUTPUT_FILE_OPTION, true, true, "[output] output file", options);
        return options;
    }

    private static File[] getTmpFiles(String tmpDirPath, String prefix) {
        File tmpFileP = new File(tmpDirPath + "/" + TMP_PAPERS_FILE_PREFIX + prefix);
        File tmpFileK = new File(tmpDirPath + "/" + TMP_PAPER_KEYWORDS_FILE_PREFIX + prefix);
        File[] tmpFiles = {tmpFileP, tmpFileK};
        return tmpFiles;
    }

    private static void merge(String tmpDirPath, boolean first, String prefix, String outputFilePath) {
        try {
            File[] tmpFiles = getTmpFiles(tmpDirPath, prefix);
            Map<String, List<String>> mergedMap = new HashMap<>();
            for (int i = 0; i < tmpFiles.length; i++) {
                BufferedReader br = new BufferedReader(new FileReader(tmpFiles[i]));
                String line;
                while ((line = br.readLine()) != null) {
                    int index = line.indexOf(Config.FIRST_DELIMITER);
                    String key = line.substring(0, index);
                    String value = line.substring(index + 1);
                    if (key.length() == 0 || value.length() == 0) {
                        continue;
                    }

                    if (!mergedMap.containsKey(key)) {
                        mergedMap.put(key, new ArrayList<>());
                    }
                    mergedMap.get(key).add(value);
                }

                br.close();
                tmpFiles[i].delete();
            }

            FileUtil.makeParentDir(outputFilePath);
            File outputFile = new File(outputFilePath);
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, !first));
            for (String paperId : mergedMap.keySet()) {
                List<String> valueList = mergedMap.get(paperId);
                if (valueList.size() != 2) {
                    continue;
                }

                StringBuilder sb = new StringBuilder(valueList.get(0));
                for (int i = 1; i < valueList.size(); i++) {
                    sb.append(Config.FIRST_DELIMITER + valueList.get(i));
                }

                bw.write(paperId + Config.FIRST_DELIMITER + sb.toString());
                bw.newLine();
            }
            bw.close();
        } catch (Exception e) {
            System.err.println("Exception @ merge");
            e.printStackTrace();
        }
    }

    private static void deleteUnusedFiles(String tmpDirPath, String fileNamePrefix, Set<String> prefixSet) {
        for (String prefix : prefixSet) {
            File file = new File(tmpDirPath + "/" + fileNamePrefix + prefix);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private static void merge(String papersFilePath, String paperKeysFilePath,
                              String tmpDirPath, String outputFilePath) {
        String tmpOutputDirPath = tmpDirPath == null ? (new File(outputFilePath)).getParent() : tmpDirPath;
        if (tmpOutputDirPath == null) {
            tmpOutputDirPath = "./";
        }

        Set<String> prefixSetP = FileUtil.splitFile(papersFilePath, Config.FIRST_DELIMITER, PAPER_ID_INDEX,
                FIELD_ID_INDEX, PREFIX_SIZE, BUFFER_SIZE, TMP_PAPERS_FILE_PREFIX, tmpOutputDirPath);
        Set<String> prefixSetK = FileUtil.splitFile(paperKeysFilePath, Config.FIRST_DELIMITER, PAPER_ID_INDEX,
                PUBLISHER_ID_INDEX, PREFIX_SIZE, BUFFER_SIZE, TMP_PAPER_KEYWORDS_FILE_PREFIX, tmpOutputDirPath);
        boolean first = true;
        for (String prefix : prefixSetP) {
            if (prefixSetK.contains(prefix)) {
                merge(tmpOutputDirPath, first, prefix, outputFilePath);
                first = false;
            }
        }

        deleteUnusedFiles(tmpOutputDirPath, TMP_PAPERS_FILE_PREFIX, prefixSetP);
        deleteUnusedFiles(tmpOutputDirPath, TMP_PAPER_KEYWORDS_FILE_PREFIX, prefixSetK);
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("VenueMerger for KDD Cup 2016 dataset", options, args);
        String papersFilePath = cl.getOptionValue(PAPERS_FILE_OPTION);
        String paperKeysFilePath = cl.getOptionValue(PAPER_KEYWORDS_FILE_OPTION);
        String tmpDirPath = cl.hasOption(Config.TMP_DIR_OPTION) ? cl.getOptionValue(Config.TMP_DIR_OPTION) : null;
        String outputFilePath = cl.getOptionValue(Config.OUTPUT_DIR_OPTION);
        merge(papersFilePath, paperKeysFilePath, tmpDirPath, outputFilePath);
    }
}
