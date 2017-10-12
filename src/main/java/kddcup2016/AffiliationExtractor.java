package kddcup2016;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.*;
import java.util.*;

public class AffiliationExtractor {
    private static final String AFFILS_FILE_OPTION = "a";
    private static final String EXTRA_FILE_PREFIX = "extra-";
    private static final String TMP_FILE_PREFIX = "tmp-";
    private static final int PAPER_ID_INDEX = 0;
    private static final int AUTHOR_ID_INDEX = 1;
    private static final int AFFILIATION_ID_INDEX = 2;
    private static final int ID_MIN_LENGTH = 1;
    private static final int AUTHOR_LIST_MIN_SIZE = 1;
    private static final int PREFIX_LENGTH = 2;
    private static final int BUFFER_SIZE = 5000000;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(Config.INPUT_FILE_OPTION, true, true, "[input] input file", options);
        MiscUtil.setOption(AFFILS_FILE_OPTION, true, false, "[input] PaperAuthorAffiliations file", options);
        MiscUtil.setOption(Config.TMP_DIR_OPTION, true, false, "[output, optional] temporary output dir", options);
        MiscUtil.setOption(Config.OUTPUT_DIR_OPTION, true, true, "[output] output directory", options);
        return options;
    }

    private static Set<String> readValidPaperListFile(String paperListFilePath) {
        Set<String> validPaperIdSet = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(paperListFilePath)));
            String line;
            while ((line = br.readLine()) != null) {
                String paperId = line.substring(0, line.indexOf(Config.FIRST_DELIMITER));
                if (!validPaperIdSet.contains(paperId)) {
                    validPaperIdSet.add(paperId);
                }
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ readValidPaperListFile");
            e.printStackTrace();
        }
        return validPaperIdSet;
    }

    private static Set<String> readIdListFile(File inputFile, String delimiter, int keyIdx, int valueIdxA,
                                                  int valueIdxB, int minIdLength, String tmpOutputDirPath) {
        Set<String> prefixSet = new HashSet<>();
        try {
            System.out.println("\tStart:\treading " + inputFile.getPath());
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            int count = 0;
            Set<String> fileNameSet = new HashSet<>();
            Map<String, List<String>> bufferMap = new HashMap<>();
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(delimiter);
                if (elements[keyIdx].length() >= minIdLength && elements[valueIdxA].length() >= minIdLength
                        && elements[valueIdxB].length() >= minIdLength) {
                    String prefix = elements[keyIdx].substring(0, PREFIX_LENGTH);
                    prefixSet.add(prefix);
                    if (!bufferMap.containsKey(prefix)) {
                        bufferMap.put(prefix, new ArrayList<>());
                    }

                    bufferMap.get(prefix).add(elements[keyIdx] + delimiter + elements[valueIdxA]
                            + Config.KEY_VALUE_DELIMITER + elements[valueIdxB]);
                    count++;
                    if (count % BUFFER_SIZE == 0) {
                        FileUtil.distributeFiles(bufferMap, fileNameSet, TMP_FILE_PREFIX, tmpOutputDirPath);
                    }
                }
            }

            br.close();
            FileUtil.distributeFiles(bufferMap, fileNameSet, TMP_FILE_PREFIX, tmpOutputDirPath);
            System.out.println("\tEnd:\treading " + inputFile.getPath());
        } catch (Exception e) {
            System.err.println("Exception @ readIdListFile");
            e.printStackTrace();
        }
        return prefixSet;
    }

    private static Map<String, List<String>> readDistributedFiles(String inputFilePath, String delimiter,
                                                                      Set<String> validPaperIdSet) {
        Map<String, List<String>> Map = new HashMap<>();
        try {
            System.out.println("\tStart:\treading " + inputFilePath);
            File inputFile = new File(inputFilePath);
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(delimiter);
                if (!validPaperIdSet.contains(elements[0])) {
                    continue;
                } else if (!Map.containsKey(elements[0])) {
                    Map.put(elements[0], new ArrayList<>());
                }
                Map.get(elements[0]).add(elements[1]);
            }

            br.close();
            inputFile.delete();
            System.out.println("\tEnd:\treading " + inputFilePath);
        } catch (Exception e) {
            System.err.println("Exception @ readDistributedFiles");
            e.printStackTrace();
        }
        return Map;
    }

    private static void extractFromIdListFile(String inputFilePath, String delimiter, int keyIdx, int valueIdxA,
                                              int valueIdxB, int minIdLength, int minValueSize,
                                              Set<String> validPaperIdSet, String tmpDirPath, String outputDirPath) {
        if (inputFilePath == null) {
            return;
        }

        System.out.println("Start:\textracting from " + inputFilePath);
        try {
            File inputFile = new File(inputFilePath);
            String tmpOutputDirPath = tmpDirPath != null ? tmpDirPath : inputFile.getParent();
            Set<String> prefixSet =
                    readIdListFile(inputFile, delimiter, keyIdx,valueIdxA, valueIdxB,minIdLength, tmpOutputDirPath);
            File outputFile = new File(outputDirPath + "/" + EXTRA_FILE_PREFIX + inputFile.getName());
            boolean first = true;
            Iterator<String> ite = prefixSet.iterator();
            while (ite.hasNext()) {
                String prefix = ite.next();
                Map<String, List<String>> distributedMap =
                        readDistributedFiles(tmpOutputDirPath + "/" + TMP_FILE_PREFIX + prefix, delimiter, validPaperIdSet);
                BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, !first));
                for (String key : distributedMap.keySet()) {
                    List<String> valueList = distributedMap.get(key);
                    if (valueList.size() >= minValueSize) {
                        StringBuilder sb = new StringBuilder(valueList.get(0));
                        int size = valueList.size();
                        for (int i = 1; i < size; i++) {
                            sb.append(Config.SECOND_DELIMITER + valueList.get(i));
                        }

                        bw.write(key + Config.FIRST_DELIMITER + sb.toString());
                        bw.newLine();
                    }
                }

                bw.close();
                first = false;
            }
        } catch (Exception e) {
            System.err.println("Exception @ extractFromIdListFile");
            e.printStackTrace();
        }
        System.out.println("End:\textracting from " + inputFilePath);
    }

    private static void extract(String inputFilePath, String affilsFilePath, String tmpDirPath, String outputDirPath) {
        Set<String> validPaperIdSet = readValidPaperListFile(inputFilePath);
        extractFromIdListFile(affilsFilePath, Config.FIRST_DELIMITER, PAPER_ID_INDEX, AUTHOR_ID_INDEX,
                AFFILIATION_ID_INDEX, ID_MIN_LENGTH, AUTHOR_LIST_MIN_SIZE, validPaperIdSet, tmpDirPath, outputDirPath);
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("AffiliationExtractor for KDD Cup 2016 dataset", options, args);
        String inputFilePath = cl.getOptionValue(Config.INPUT_FILE_OPTION);
        String affilsFilePath = cl.getOptionValue(AFFILS_FILE_OPTION);
        String tmpDirPath = cl.hasOption(Config.TMP_DIR_OPTION) ? cl.getOptionValue(Config.TMP_DIR_OPTION) : null;
        String outputDirPath = cl.getOptionValue(Config.OUTPUT_DIR_OPTION);
        extract(inputFilePath, affilsFilePath, tmpDirPath, outputDirPath);
    }
}
