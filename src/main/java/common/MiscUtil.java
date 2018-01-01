package common;

import org.apache.commons.cli.*;

import java.io.File;
import java.util.*;

public class MiscUtil {
    public static void setOption(String optionArg, boolean hasArg, boolean required, String desc, Options options) {
        options.addOption(Option.builder(optionArg)
                .hasArg(hasArg)
                .required(required)
                .desc(desc)
                .build());
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

    public static Set<String> buildAuthorIdSet(List<File> fileList) {
        Set<String> authorIdSet = new HashSet<>();
        for (File file : fileList) {
            String authorId = file.getName();
            authorIdSet.add(authorId);
        }
        return authorIdSet;
    }

    public static boolean checkIfAuthorExists(Set<String> authorIdSet, Set<String> trainingAuthorIdSet) {
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

    public static void deepCopy(double[] inputArray, double[] outputArray) {
        for (int i = 0; i < outputArray.length; i++) {
            outputArray[i] = inputArray[i];
        }
    }

    public static int[] convertToIntArray(String str) {
        String[] elements = str.split(Config.OPTION_DELIMITER);
        List<Integer> list = new ArrayList<>();
        for (String element : elements) {
            list.add(Integer.parseInt(element));
        }

        Collections.sort(list);
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
