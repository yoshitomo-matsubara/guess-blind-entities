package sub;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.*;
import java.util.HashSet;

public class ColumnFilter {
    private static final String FILTER_FILE_OPTION = "f";
    private static final String COLUMN_INDEX_OPTION = "c";

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(Config.INPUT_FILE_OPTION, true, true, "[input] input file", options);
        MiscUtil.setOption(FILTER_FILE_OPTION, true, true, "[param] filter list file", options);
        MiscUtil.setOption(COLUMN_INDEX_OPTION, true, true, "[param] column index", options);
        MiscUtil.setOption(Config.OUTPUT_FILE_OPTION, true, true, "[output] output file", options);
        return options;
    }

    private static HashSet<String> readFilterFile(String filterFilePath) {
        HashSet<String> filterSet = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(filterFilePath)));
            String line;
            while ((line = br.readLine()) != null) {
                filterSet.add(line);
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ readFilterFile");
            e.printStackTrace();
        }
        return filterSet;
    }

    private static void filter(String inputFilePath, String filterFilePath, int columnIndex, String outputFilePath) {
        HashSet<String> filterSet = readFilterFile(filterFilePath);
        if (filterSet.size() == 0 || columnIndex < 0) {
            return;
        }

        try {
            FileUtil.makeParentDir(outputFilePath);
            File outputFile = new File(outputFilePath);
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
            BufferedReader br = new BufferedReader(new FileReader(new File(inputFilePath)));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(Config.FIRST_DELIMITER);
                if (filterSet.contains(elements[columnIndex])) {
                    bw.write(line);
                    bw.newLine();
                }
            }
            br.close();
            bw.close();
        } catch (Exception e) {
            System.err.println("Exception @ filter");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("ColumnFilter", options, args);
        String inputFilePath = cl.getOptionValue(Config.INPUT_FILE_OPTION);
        String filterFilePath = cl.getOptionValue(FILTER_FILE_OPTION);
        int columnIndex = Integer.parseInt(cl.getOptionValue(COLUMN_INDEX_OPTION)) - 1;
        String outputFilePath = cl.getOptionValue(Config.OUTPUT_FILE_OPTION);
        filter(inputFilePath, filterFilePath, columnIndex, outputFilePath);
    }
}
