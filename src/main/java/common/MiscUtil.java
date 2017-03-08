package common;

import org.apache.commons.cli.*;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class MiscUtil {
    public static void setOption(String optionArg, boolean hasArg, boolean required, String desc, Options options) {
        options.addOption(Option.builder(optionArg)
                .hasArg(hasArg)
                .required(required)
                .desc(desc)
                .build());
    }

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
            StringBuilder sb = new StringBuilder("args:");
            for (int i = 0; i < args.length; i++) {
                sb.append(" " + args[i]);
            }
            System.out.println(sb.toString());
        } catch (ParseException pe) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp(Config.PROJECT_NAME + ": " + className, options, true);
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

    public static HashSet<String> buildAuthorIdSet(List<File> fileList) {
        HashSet<String> authorIdSet = new HashSet<>();
        for (File file : fileList) {
            String authorId = file.getName();
            authorIdSet.add(authorId);
        }
        return authorIdSet;
    }

    public static boolean checkIfAuthorExists(HashSet<String> authorIdSet, HashSet<String> trainingAuthorIdSet) {
        Iterator<String> ite = authorIdSet.iterator();
        while (ite.hasNext()) {
            if (trainingAuthorIdSet.contains(ite.next())) {
                return true;
            }
        }
        return false;
    }

    public static int[] initIntArray(int size, int init) {
        int[] array = new int[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = init;
        }
        return array;
    }

    public static double[] initDoubleArray(int size, double init) {
        double[] array = new double[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = init;
        }
        return array;
    }
}
