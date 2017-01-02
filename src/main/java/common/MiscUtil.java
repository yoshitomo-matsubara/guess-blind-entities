package common;

import org.apache.commons.cli.*;

import java.util.HashMap;

public class MiscUtil {
    public static HashMap<String, String> getMonthMap() {
        HashMap<String, String> monthMap = new HashMap<>();
        monthMap.put("JAN", "01");
        monthMap.put("FEB", "02");
        monthMap.put("MAR", "03");
        monthMap.put("APR", "04");
        monthMap.put("MAY", "05");
        monthMap.put("JUN", "06");
        monthMap.put("JUL", "07");
        monthMap.put("AUG", "08");
        monthMap.put("SEP", "09");
        monthMap.put("OCT", "10");
        monthMap.put("NOV", "11");
        monthMap.put("DEC", "12");
        return monthMap;
    }

    public static CommandLine setParams(String className, Options options, String[] args) {
        CommandLineParser clp = new DefaultParser();
        CommandLine cl = null;
        try {
            cl = clp.parse(options, args);
        } catch (ParseException pe) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("Blindness Evaluation: " + className, options, true);
        }
        return cl;
    }

    public static String addZero(String paperId, int formatSize) {
        if (paperId.length() == formatSize) {
            return paperId;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = paperId.length(); i < formatSize; i++) {
            sb.append("0");
        }

        sb.append(paperId);
        return sb.toString();
    }
}
