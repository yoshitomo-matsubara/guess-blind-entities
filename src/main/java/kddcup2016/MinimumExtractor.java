package kddcup2016;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.*;
import java.util.*;

public class MinimumExtractor {
    private static final String PAPERS_FILE_OPTION = "p";
    private static final String AFFILS_FILE_OPTION = "a";
    private static final String REFS_FILE_OPTION = "r";
    private static final String MIN_FILE_PREFIX = "min-";
    private static final String TMP_FILE_PREFIX = "tmp-";
    private static final int PAPER_ID_INDEX = 0;
    private static final int PUB_DATE_INDEX = 4;
    private static final int AUTHOR_ID_INDEX = 1;
    private static final int PAPER_REF_ID_INDEX = 1;
    private static final int ID_MIN_LENGTH = 1;
    private static final int PUB_DATE_MIN_LENGTH = 4;
    private static final int AUTHOR_LIST_MIN_SIZE = 1;
    private static final int REF_LIST_MIN_SIZE = 1;
    private static final int PREFIX_LENGTH = 2;
    private static final int BUFFER_SIZE = 5000000;

    private static Options setOptions() {
        Options options = new Options();
        options.addOption(Option.builder(PAPERS_FILE_OPTION)
                .hasArg(true)
                .required(false)
                .desc("[input, optional] Papers file")
                .build());
        options.addOption(Option.builder(AFFILS_FILE_OPTION)
                .hasArg(true)
                .required(false)
                .desc("[input, optional] PaperAuthorAffiliations file")
                .build());
        options.addOption(Option.builder(REFS_FILE_OPTION)
                .hasArg(true)
                .required(false)
                .desc("[input, optional] PaperReferences file")
                .build());
        return options;
    }

    private static void extractFromPapersFile(String inputFilePath, String delimiter, int keyIdx, int valueIdx,
                                       int minKeyLength, int minValueLength) {
        if (inputFilePath == null) {
            return;
        }

        System.out.println("Start:\textracting from " + inputFilePath);
        File inputFile = new File(inputFilePath);
        File outputFile = new File(inputFile.getParent() + "/" + MIN_FILE_PREFIX + inputFile.getName());
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(delimiter);
                // Skip line which misses some information
                if (elements[keyIdx].length() >= minKeyLength && elements[valueIdx].length() >= minValueLength) {
                    bw.write(elements[keyIdx] + delimiter + elements[valueIdx].substring(0, minValueLength));
                    bw.newLine();
                }
            }
            br.close();
            bw.close();
        } catch (Exception e) {
            System.err.println("Exception @ extractFromPapersFile");
            e.printStackTrace();
        }
        System.out.println("End:\textracting from " + inputFilePath);
    }

    private static HashSet<String> readIdListFile(File inputFile, String delimiter,
                                                              int keyIdx, int valueIdx, int minIdLength) {
        HashSet<String> prefixSet = new HashSet<>();
        try {
            System.out.println("\tStart:\treading " + inputFile.getPath());
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            int count = 0;
            String dirPath = inputFile.getParent();
            HashSet<String> fileNameSet = new HashSet<>();
            HashMap<String, List<String>> bufferMap = new HashMap<>();
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(delimiter);
                if (elements[keyIdx].length() >= minIdLength && elements[valueIdx].length() >= minIdLength) {
                    String prefix = elements[keyIdx].substring(0, PREFIX_LENGTH);
                    prefixSet.add(prefix);
                    if (!bufferMap.containsKey(prefix)) {
                        bufferMap.put(prefix, new ArrayList<>());
                    }

                    bufferMap.get(prefix).add(elements[keyIdx] + delimiter + elements[valueIdx]);
                    count++;
                    if (count % BUFFER_SIZE == 0) {
                        FileUtil.distributeFiles(bufferMap, fileNameSet, TMP_FILE_PREFIX, dirPath);
                    }
                }
            }

            br.close();
            FileUtil.distributeFiles(bufferMap, fileNameSet, TMP_FILE_PREFIX, inputFile.getParent());
            System.out.println("\tEnd:\treading " + inputFile.getPath());
        } catch (Exception e) {
            System.err.println("Exception @ readIdListFile");
            e.printStackTrace();
        }
        return prefixSet;
    }

    private static HashMap<String, List<String>> readDistributedFiles(String inputFilePath, String delimiter) {
        HashMap<String, List<String>> hashMap = new HashMap<>();
        try {
            System.out.println("\tStart:\treading " + inputFilePath);
            File inputFile = new File(inputFilePath);
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(delimiter);
                if (!hashMap.containsKey(elements[0])) {
                    hashMap.put(elements[0], new ArrayList<>());
                }
                hashMap.get(elements[0]).add(elements[1]);
            }

            br.close();
            inputFile.delete();
            System.out.println("\tEnd:\treading " + inputFilePath);
        } catch (Exception e) {
            System.err.println("Exception @ readDistributedFiles");
            e.printStackTrace();
        }
        return hashMap;
    }

    private static void extractFromIdListFile(String inputFilePath, String delimiter, int keyIdx, int valueIdx,
                                           int minIdLength, int minValueSize) {
        if (inputFilePath == null) {
            return;
        }

        System.out.println("Start:\textracting from " + inputFilePath);
        File inputFile = new File(inputFilePath);
        HashSet<String> prefixSet = readIdListFile(inputFile, delimiter, keyIdx, valueIdx, minIdLength);
        File outputFile = new File(inputFile.getParent() + "/" + MIN_FILE_PREFIX + inputFile.getName());
        try {
            boolean first = true;
            Iterator<String> ite = prefixSet.iterator();
            while (ite.hasNext()) {
                String prefix = ite.next();
                HashMap<String, List<String>> distributedMap =
                        readDistributedFiles(inputFile.getParent() + "/" + TMP_FILE_PREFIX + prefix, delimiter);
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

    private static void extract(String papersFilePath, String affilsFilePath, String refsFilePath) {
        extractFromPapersFile(papersFilePath, Config.FIRST_DELIMITER,
                PAPER_ID_INDEX, PUB_DATE_INDEX, ID_MIN_LENGTH, PUB_DATE_MIN_LENGTH);
        extractFromIdListFile(affilsFilePath, Config.FIRST_DELIMITER,
                PAPER_ID_INDEX, AUTHOR_ID_INDEX, ID_MIN_LENGTH, AUTHOR_LIST_MIN_SIZE);
        extractFromIdListFile(refsFilePath, Config.FIRST_DELIMITER,
                PAPER_ID_INDEX, PAPER_REF_ID_INDEX, ID_MIN_LENGTH, REF_LIST_MIN_SIZE);
    }

    public static void main(String[] args) {
        Options options = setOptions();
        CommandLine cl = MiscUtil.setParams("MinimumExtractor for KDD Cup 2016 dataset", options, args);
        String papersFilePath = cl.hasOption(PAPERS_FILE_OPTION) ? cl.getOptionValue(PAPERS_FILE_OPTION) : null;
        String affilsFilePath = cl.hasOption(AFFILS_FILE_OPTION) ? cl.getOptionValue(AFFILS_FILE_OPTION) : null;
        String refsFilePath = cl.hasOption(REFS_FILE_OPTION) ? cl.getOptionValue(REFS_FILE_OPTION) : null;
        extract(papersFilePath, affilsFilePath, refsFilePath);
    }
}
