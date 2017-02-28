package kddcup2016;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.*;
import java.util.HashMap;

public class BagOfFieldsMaker {
    private static final int VENUE_ID_INDEX = 1;
    private static final int FIELD_ID_INDEX = 2;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(Config.INPUT_FILE_OPTION, true, true, "[input] input file", options);
        MiscUtil.setOption(Config.OUTPUT_FILE_OPTION, true, true, "[output] output file", options);
        return options;
    }

    private static HashMap<String, BagOfFields> makeBagOfFieldsMap(String inputFilePath) {
        HashMap<String, BagOfFields> bofMap = new HashMap<>();
        System.out.println("Start:\treading " + inputFilePath);
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(inputFilePath)));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(Config.FIRST_DELIMITER);
                if (!bofMap.containsKey(elements[VENUE_ID_INDEX])) {
                    if (!bofMap.containsKey(elements[VENUE_ID_INDEX])) {
                        bofMap.put(elements[VENUE_ID_INDEX], new BagOfFields(elements[VENUE_ID_INDEX]));
                    }

                    String[] fieldIds = elements[FIELD_ID_INDEX].split(Config.SECOND_DELIMITER);
                    for (String fieldId : fieldIds) {
                        bofMap.get(elements[VENUE_ID_INDEX]).countUp(fieldId);
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ makeBagOfFieldsMap");
            e.printStackTrace();
        }

        System.out.println("End:\treading " + inputFilePath);
        return bofMap;
    }

    private static void writeFile(HashMap<String, BagOfFields> bofMap, String outputFilePath) {
        try {
            FileUtil.makeParentDir(outputFilePath);
            File outputFile = new File(outputFilePath);
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
            for (String venueId : bofMap.keySet()) {
                BagOfFields bof = bofMap.get(venueId);
                bw.write(bof.toString());
                bw.newLine();
            }
            bw.close();
        } catch (Exception e) {
            System.err.println("Exception @ writeFile");
            e.printStackTrace();
        }
    }

    private static void make(String inputFilePath, String outputFilePath) {
        HashMap<String, BagOfFields> bofMap = makeBagOfFieldsMap(inputFilePath);
        writeFile(bofMap, outputFilePath);
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("BagOfFieldsMaker", options, args);
        String inputFilePath = cl.getOptionValue(Config.INPUT_FILE_OPTION);
        String outputFilePath = cl.getOptionValue(Config.OUTPUT_FILE_OPTION);
        make(inputFilePath, outputFilePath);
    }
}
