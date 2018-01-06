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
        for (String authorId : authorIdSet) {
            if (trainingAuthorIdSet.contains(authorId)) {
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

    public static void initArrayMapNotExist(int key, int arraySize, Map<Integer, Integer[]> arrayMap) {
        if (arrayMap.containsKey(key)) {
            return;
        }

        Integer[] array = new Integer[arraySize];
        for (int i = 0; i < array.length; i++) {
            array[i] = 0;
        }
        arrayMap.put(key, array);
    }

    public static void initArrayMapIfNotExist(String key, int arraySize, Map<String, Integer[]> arrayMap) {
        if (arrayMap.containsKey(key)) {
            return;
        }

        Integer[] array = new Integer[arraySize];
        for (int i = 0; i < array.length; i++) {
            array[i] = 0;
        }
        arrayMap.put(key, array);
    }

    public static void putAndInitListIfNotExist(String key, int value, Map<String, List<Integer>> map) {
        if (!map.containsKey(key)) {
            map.put(key, new ArrayList<>());
        }
        map.get(key).add(value);
    }

    public static int[] convertToIntArray(String str, String delimiter) {
        String[] elements = str.split(delimiter);
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

    public static String convertListToString(List<?> list, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            String str = sb.length() == 0 ? String.valueOf(list.get(i)) : delimiter + String.valueOf(list.get(i));
            sb.append(str);
        }
        return sb.toString();
    }
}
