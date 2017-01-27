package common;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class FileUtil {
    public static List<File> getFileListR(String dirPath) {
        File[] files = (new File(dirPath)).listFiles();
        List<File> fileList = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                fileList.addAll(getFileListR(file.getPath()));
            } else {
                fileList.add(file);
            }
        }
        return fileList;
    }

    public static List<File> getFileList(String dirPath) {
        File[] files = (new File(dirPath)).listFiles();
        List<File> fileList = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                fileList.add(file);
            }
        }
        return fileList;
    }

    public static List<File> getDirList(String dirPath) {
        File[] files = (new File(dirPath)).listFiles();
        List<File> dirList = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                dirList.add(file);
            }
        }
        return dirList;
    }

    public static List<String> readFile(File file) {
        List<String> lineList = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                lineList.add(line);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lineList;
    }

    public static List<String> readFile(String filePath) {
        return readFile(new File(filePath));
    }

    public static void readFile(File file, String delimiter,
                                int keyIdx, int valueIdx, HashMap<String, String> hashMap) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(delimiter);
                if (!hashMap.containsKey(elements[keyIdx])) {
                    hashMap.put(elements[keyIdx], elements[valueIdx]);
                } else {
                    String orgValue = hashMap.get(elements[keyIdx]);
                    hashMap.put(elements[keyIdx], orgValue + delimiter + elements[valueIdx]);
                }
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ readFile");
            e.printStackTrace();
        }
    }

    public static HashMap<String, String> readFile(String filePath, String delimiter, int keyIdx, int valueIdx) {
        HashMap<String, String> hashMap = new HashMap<>();
        try {
            File file = new File(filePath);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                String[] elements = line.split(delimiter);
                if (!hashMap.containsKey(elements[keyIdx])) {
                    hashMap.put(elements[keyIdx], elements[valueIdx]);
                } else {
                    String orgValue = hashMap.get(elements[keyIdx]);
                    hashMap.put(elements[keyIdx], orgValue + delimiter + elements[valueIdx]);
                }
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Exception @ readFile");
            e.printStackTrace();
        }
        return hashMap;
    }

    public static void makeDirIfNotExist(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static void makeParentDir(String filePath) {
        File file = new File(filePath);
        String parentDirPath = file.getParent();
        if (parentDirPath == null || parentDirPath.length() == 0) {
            return;
        }
        makeDirIfNotExist(parentDirPath);
    }

    public static boolean overwriteFile(String line, boolean first, String filePath) {
        makeParentDir(filePath);
        File file = new File(filePath);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, !first));
            bw.write(line);
            bw.newLine();
            bw.close();
        } catch (Exception e) {
            System.err.println("Exception @ overwriteFile");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void writeFile(List<String> lineList, String filePath) {
        makeParentDir(filePath);
        File file = new File(filePath);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            for (String line : lineList) {
                bw.write(line);
                bw.newLine();
            }
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void distributeFiles(HashMap<String, List<String>> hashMap,
                                       HashSet<String> fileNameSet, String tmpFilePrefix, String outputDirPath) {
        try {
            for (String initial : hashMap.keySet()) {
                File outputFile = new File(outputDirPath + "/" + tmpFilePrefix + initial);
                String outputFileName = outputFile.getName();
                BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, fileNameSet.contains(outputFileName)));
                List<String> valueList = hashMap.get(initial);
                for (String value : valueList) {
                    bw.write(value);
                    bw.newLine();
                }

                bw.close();
                fileNameSet.add(outputFileName);
            }
        } catch (Exception e) {
            System.err.println("Exception @ distributeFiles");
            e.printStackTrace();
        }
        hashMap.clear();
    }

    public static HashSet<String> splitFile(String inputFilePath, int prefixLength,
                                            int bufferSize, String tmpFilePrefix, String outputDirPath) {
        HashSet<String> prefixSet = new HashSet<>();
        try {
            File outputDir = new File (outputDirPath);
            if (!outputDir.exists()) {
                outputDir.mkdir();
            }

            File inputFile = new File(inputFilePath);
            HashMap<String, List<String>> bufferMap = new HashMap<>();
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            HashSet<String> fileNameSet = new HashSet<>();
            int count = 0;
            String line;
            while ((line = br.readLine()) != null) {
                String prefix = line.substring(0, prefixLength);
                prefixSet.add(prefix);
                if (!bufferMap.containsKey(prefix)) {
                    bufferMap.put(prefix, new ArrayList<>());
                }

                bufferMap.get(prefix).add(line);
                count++;
                if (count % bufferSize == 0) {
                    distributeFiles(bufferMap, fileNameSet, tmpFilePrefix, outputDirPath);
                }
            }

            br.close();
            distributeFiles(bufferMap, fileNameSet, tmpFilePrefix, outputDirPath);
        } catch (Exception e) {
            System.err.println("Exception @ splitFile");
            e.printStackTrace();
        }
        return prefixSet;
    }

    public static HashSet<String> splitFile(String inputFilePath, String delimiter, int keyIdx, int valueIdx,
                                            int prefixLength, int bufferSize, String tmpFilePrefix, String outputDirPath) {
        HashSet<String> prefixSet = new HashSet<>();
        try {
            File outputDir = new File (outputDirPath);
            if (!outputDir.exists()) {
                outputDir.mkdir();
            }

            File inputFile = new File(inputFilePath);
            HashMap<String, List<String>> bufferMap = new HashMap<>();
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            HashSet<String> fileNameSet = new HashSet<>();
            int count = 0;
            String line;
            while ((line = br.readLine()) != null) {
                String prefix = line.substring(0, prefixLength);
                prefixSet.add(prefix);
                if (!bufferMap.containsKey(prefix)) {
                    bufferMap.put(prefix, new ArrayList<>());
                }

                String[] elements = line.split(delimiter);
                bufferMap.get(prefix).add(elements[keyIdx] + delimiter + elements[valueIdx]);
                count++;
                if (count % bufferSize == 0) {
                    distributeFiles(bufferMap, fileNameSet, tmpFilePrefix, outputDirPath);
                }
            }

            br.close();
            distributeFiles(bufferMap, fileNameSet, tmpFilePrefix, outputDirPath);
        } catch (Exception e) {
            System.err.println("Exception @ splitFile");
            e.printStackTrace();
        }
        return prefixSet;
    }

    public static void writeFile(HashMap<String, Integer> hashMap, String outputFilePath) {
        try {
            File publisherFile = new File(outputFilePath);
            BufferedWriter bw = new BufferedWriter(new FileWriter(publisherFile));
            for (String publisherId : hashMap.keySet()) {
                bw.write(publisherId + Config.FIRST_DELIMITER + String.valueOf(hashMap.get(publisherId)));
                bw.newLine();
            }
            bw.close();
        } catch (Exception e) {
            System.err.println("Exception @ writeFile");
            e.printStackTrace();
        }
    }
}
