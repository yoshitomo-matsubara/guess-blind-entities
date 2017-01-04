package kddcup2016;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.*;
import java.util.*;

public class MinimumMerger {
    private static final String PAPERS_FILE_OPTION = "p";
    private static final String AFFILS_FILE_OPTION = "a";
    private static final String REFS_FILE_OPTION = "r";
    private static final String TMP_PAPERS_FILE_PREFIX = "tmp-p-";
    private static final String TMP_AFFILS_FILE_PREFIX = "tmp-a-";
    private static final String TMP_REFS_FILE_PREFIX = "tmp-r-";
    private static final int PREFIX_SIZE = 2;
    private static final int BUFFER_SIZE = 5000000;

    private static Options setOptions() {
        Options options = new Options();
        options.addOption(Option.builder(PAPERS_FILE_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[input] min-Papers file")
                .build());
        options.addOption(Option.builder(AFFILS_FILE_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[input] min-PaperAuthorAffiliations file")
                .build());
        options.addOption(Option.builder(REFS_FILE_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[input] min-PaperReferences file")
                .build());
        options.addOption(Option.builder(Config.OUTPUT_FILE_OPTION)
                .hasArg(true)
                .required(true)
                .desc("[output] output file")
                .build());
        return options;
    }

    private static File[] getTmpFiles(String tmpDirPath, String prefix) {
        File tmpFileP = new File(tmpDirPath + "/" + TMP_PAPERS_FILE_PREFIX + prefix);
        File tmpFileA = new File(tmpDirPath + "/" + TMP_AFFILS_FILE_PREFIX + prefix);
        File tmpFileR = new File(tmpDirPath + "/" + TMP_REFS_FILE_PREFIX + prefix);
        File[] tmpFiles = {tmpFileP, tmpFileA, tmpFileR};
        return tmpFiles;
    }

    private static void merge(String tmpDirPath, boolean first, String prefix, String outputFilePath) {
        try {
            File[] tmpFiles = getTmpFiles(tmpDirPath, prefix);
            HashMap<String, List<String>> mergedMap = new HashMap<>();
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

            File outputFile = new File(outputFilePath);
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, !first));
            for (String paperId : mergedMap.keySet()) {
                List<String> valueList = mergedMap.get(paperId);
                if (valueList.size() != 3) {
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

    private static void deleteUnusedFiles(String tmpDirPath, String fileNamePrefix, HashSet<String> prefixSet) {
        Iterator<String> ite = prefixSet.iterator();
        while (ite.hasNext()) {
            File file = new File(tmpDirPath + "/" + fileNamePrefix + ite.next());
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private static void merge(String papersFilePath, String affilsFilePath, String refsFilePath, String outputFilePath) {
        String tmpDirPath = (new File(outputFilePath)).getParent();
        if (tmpDirPath == null) {
            tmpDirPath = "./";
        }

        HashSet<String> prefixSetP = FileUtil.splitFile(papersFilePath,
                PREFIX_SIZE, BUFFER_SIZE, TMP_PAPERS_FILE_PREFIX, tmpDirPath);
        HashSet<String> prefixSetA = FileUtil.splitFile(affilsFilePath,
                PREFIX_SIZE, BUFFER_SIZE, TMP_AFFILS_FILE_PREFIX, tmpDirPath);
        HashSet<String> prefixSetR = FileUtil.splitFile(refsFilePath,
                PREFIX_SIZE, BUFFER_SIZE, TMP_REFS_FILE_PREFIX, tmpDirPath);
        Iterator<String> ite = prefixSetP.iterator();
        boolean first = true;
        while (ite.hasNext()) {
            String prefix = ite.next();
            if (prefixSetA.contains(prefix) && prefixSetR.contains(prefix)) {
                merge(tmpDirPath, first, prefix, outputFilePath);
                first = false;
            }
        }

        deleteUnusedFiles(tmpDirPath, TMP_PAPERS_FILE_PREFIX, prefixSetP);
        deleteUnusedFiles(tmpDirPath, TMP_AFFILS_FILE_PREFIX, prefixSetA);
        deleteUnusedFiles(tmpDirPath, TMP_REFS_FILE_PREFIX, prefixSetR);
    }

    public static void main(String[] args) {
        Options options = setOptions();
        CommandLine cl = MiscUtil.setParams("MinimumMerger for KDD Cup 2016 dataset", options, args);
        String papersFilePath = cl.getOptionValue(PAPERS_FILE_OPTION);
        String affilsFilePath = cl.getOptionValue(AFFILS_FILE_OPTION);
        String refsFilePath = cl.getOptionValue(REFS_FILE_OPTION);
        String outputFilePath = cl.getOptionValue(Config.OUTPUT_DIR_OPTION);
        merge(papersFilePath, affilsFilePath, refsFilePath, outputFilePath);
    }
}
