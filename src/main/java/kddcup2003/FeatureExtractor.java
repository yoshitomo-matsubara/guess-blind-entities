package kddcup2003;

import common.Config;
import common.FileUtil;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class FeatureExtractor {
    private static final String ABSTRACTS_DIR_OPTION = "ab";
    private static final String REF_FILE_OPTION = "r";
    private static final String AUTHOR_ID_FILE_OPTION = "au";
    private static final String COMMENT_PREFIX = "#";
    private static final String PAPER_ID_PREFIX = "Paper: hep-th/";
    private static final String DATE_PREFIX = "Date: ";
    private static final String AUTHORS_PREFIX = "Authors: ";
    private static final String AUTHOR_DELIMITER_A = ", ";
    private static final String AUTHOR_DELIMITER_B = ",";
    private static final String[] ORDERS = {PAPER_ID_PREFIX, DATE_PREFIX, AUTHORS_PREFIX};
    private static final HashMap<String, String> MONTH_MAP = MiscUtil.getMonthMap();
    private static final Pattern PTN = Pattern.compile("^[a-zA-Z].*");
    private static final int FORMAT_SIZE = 7;

    private static Options getOptions() {
        Options options = new Options();
        MiscUtil.setOption(ABSTRACTS_DIR_OPTION, true, true, "[input] cit-HepTh-abstracts directory", options);
        MiscUtil.setOption(REF_FILE_OPTION, true, true, "[input] cit-HepTh file", options);
        MiscUtil.setOption(AUTHOR_ID_FILE_OPTION, true, false, "[output, optional] author id file", options);
        MiscUtil.setOption(Config.OUTPUT_FILE_OPTION, true, true, "[output] output file", options);
        return options;
    }

    private static HashMap<String, String> createRefMap(String refFilePath) {
        List<String> lineList = FileUtil.readFile(refFilePath);
        HashMap<String, List<String>> refListMap = new HashMap<>();
        for (String line : lineList) {
            if (!line.startsWith(COMMENT_PREFIX)) {
                // ids[0]: FromNodeId, ids[1]: ToNodeId
                String[] ids = line.split(Config.FIRST_DELIMITER);
                ids[0] = MiscUtil.addZero(ids[0], FORMAT_SIZE);
                ids[1] = MiscUtil.addZero(ids[1], FORMAT_SIZE);
                if (!refListMap.containsKey(ids[0])) {
                    refListMap.put(ids[0], new ArrayList<>());
                }
                refListMap.get(ids[0]).add(ids[1]);
            }
        }

        HashMap<String, String> refMap = new HashMap<>();
        for (String key : refListMap.keySet()) {
            StringBuilder sb = new StringBuilder();
            List<String> refList = refListMap.get(key);
            for (String ref : refList) {
                String str = sb.length() == 0 ? ref : Config.SECOND_DELIMITER + ref;
                sb.append(str);
            }
            refMap.put(key, sb.toString());
        }
        return refMap;
    }

    private static String cleanDateStr(String formattedStr) {
        String[] elements = formattedStr.split(" ");
        List<String> strList = new ArrayList<>();
        for (int i = 0; i < elements.length; i++) {
            if (elements[i].length() > 0 && !elements[i].contains(":")) {
                strList.add(elements[i]);
            }
        }

        elements = strList.toArray(new String[strList.size()]);
        String monthStr = elements[1].toUpperCase();
        if (!MONTH_MAP.containsKey(monthStr)) {
            if (monthStr.length() > 3) {
                monthStr = monthStr.substring(0, 3);
                if (!MONTH_MAP.containsKey(monthStr)) {
                    return "";
                }
            } else if (monthStr.length() == 3) {
                String tmp = elements[0];
                elements[0] = elements[1];
                elements[1] = tmp;
            }
            monthStr = MONTH_MAP.get(elements[1]);
        }

        String day = elements[0].length() == 1 ? "0" + elements[0] : elements[0];
        String year;
        if (elements[2].length() == 4) {
            year = elements[2];
        } else if (elements[2].length() == 1 || elements[2].length() == 2) {
            if (elements[2].startsWith("9")) {
                elements[2] = "19" + elements[2];
            } else if (elements[2].startsWith("0")) {
                elements[2] = elements[2].length() == 2 ? "20" + elements[2] : "200" + elements[2];
            } else {
                return "";
            }
            year = elements[2];
        } else {
            return "";
        }

        if (elements.length < 4) {
            return "";
        }

        if (!MONTH_MAP.containsKey(monthStr)) {
            if (PTN.matcher(elements[0]).matches()) {
                String tmp = elements[0];
                elements[0] = elements[1];
                elements[1] = tmp;
            }

            elements[1] = elements[1].substring(0, 3);
            monthStr = elements[1].toUpperCase();
            day = elements[0].length() == 1 ? "0" + elements[0] : elements[0];
        }

        String month = MONTH_MAP.get(monthStr);
        return year + "-" + month + "-" + day;
    }

    private static String formatDateStr(String dateStr) {
        int index = -1;
        String str = "";
        if ((index = dateStr.indexOf(",  ")) >= 0) {
            str = dateStr.substring(index + 3);
        } else if ((index = dateStr.indexOf(", ")) >= 0) {
            str = dateStr.substring(index + 2);
        } else if (PTN.matcher(dateStr).matches()) {
            str = dateStr.substring(4);
        } else {
            str = dateStr;
            if ((index = str.indexOf(" ")) >= 0) {
                String prefix = str.substring(0, index);
                String[] elements = prefix.split("-");
                if (elements.length == 3) {
                    prefix = elements[0] + " " + elements[1] + " " + elements[2];
                } else if (prefix.indexOf("/") >= 0){
                    elements = prefix.split("/");
                    if (elements[1].equals("01")) {
                        elements[1] = "Jan";
                    }
                    prefix = elements[0] + " " + elements[1] + " " + elements[2];
                }

                String suffix = str.substring(index + 1);
                str = prefix + " " + suffix;
            }
        }
        return cleanDateStr(str);
    }

    private static boolean checkIfInvalid(String name) {
        String[] indexOfArray = {"(", ")", " pages"};
        for (String invalidStr : indexOfArray) {
            if (name.indexOf(invalidStr) >= 0) {
                return true;
            }
        }

        if (name.length() == 0) {
            return true;
        } else if (name.startsWith(" ")) {
            return true;
        }
        return false;
    }

    private static String addPeriodIfInitial(String name) {
        if (name.length() == 1) {
            return name + ".";
        }
        return name;
    }

    private static void addAuthorToList(String author, List<String> authorList) {
        if (author.indexOf(AUTHOR_DELIMITER_B) >= 0) {
            String[] elements = author.split(AUTHOR_DELIMITER_B);
            for (String element : elements) {
                addAuthorToList(element, authorList);
            }
        } else if (author.indexOf(" & ") >= 0) {
            String[] elements = author.split(" & ");
            for (String element : elements) {
                addAuthorToList(element, authorList);
            }
        } else if (author.indexOf("  ") > 0) {
            addAuthorToList(author.substring(0, author.indexOf("  ")), authorList);
        } else if (author.endsWith(" ")) {
            addAuthorToList(author.substring(0, author.lastIndexOf(" ")), authorList);
        } else if (author.startsWith(" ")) {
            addAuthorToList(author.substring(1), authorList);
        } else if (!author.endsWith(".") && !author.contains("Universi") && author.length() > 3) {
            if (author.indexOf(".") >= 0) {
                String[] elements = author.split("\\.");
                StringBuilder sb = new StringBuilder(elements[0]);
                for (int i = 1; i < elements.length; i++) {
                    String str = elements[i].startsWith(" ") ? elements[i] : " " + elements[i];
                    sb.append("." + str);
                }
                author = sb.toString();
            }

            author = author.replaceAll("\\\\|\\{|\\}|\"|'|=", "").replaceAll(" and|- ", "").replaceAll(" - | -", " ")
                    .replaceAll("--", "-").replaceAll(" -", " ");
            if (author.endsWith(" Jr") || author.endsWith(" jr")) {
                author = author.substring(0, author.length() - 3);
            }

            String[] elements = author.split(" ");
            if (elements.length > 1) {
                StringBuilder sb = new StringBuilder(addPeriodIfInitial(elements[0]).toUpperCase());
                for (int i = 1; i < elements.length; i++) {
                    sb.append(" " + addPeriodIfInitial(elements[i]).toUpperCase());
                }
                authorList.add(sb.toString());
            }
        }
    }

    private static List<String> splitAuthors(String[] authors) {
        List<String> authorList = new ArrayList<>();
        for (int i = 0; i < authors.length; i++) {
            authors[i] = authors[i].replaceAll("\\(.*?\\)", "");
            if (checkIfInvalid(authors[i])) {
                continue;
            } else if (authors[i].indexOf(" and ") >= 0) {
                String[] elements = authors[i].split(" and ");
                for (String author : elements) {
                    addAuthorToList(author, authorList);
                }
            } else if (authors[i].startsWith("and ")) {
                addAuthorToList(authors[i].substring(4), authorList);
            } else {
                addAuthorToList(authors[i], authorList);
            }
        }
        return authorList;
    }

    private static void extract(File inputFile, HashMap<String, String> refMap,
                                TreeMap<String, Integer> authorIdMap, String outputFilePath) {
        List<String> lineList = FileUtil.readFile(inputFile);
        StringBuilder sb = new StringBuilder();
        int index = 0;
        int size = lineList.size();
        String paperId = "";
        String[] authors = new String[0];
        for (int i = 0; i < size; i++) {
            String line = lineList.remove(0);
            if (line.startsWith(ORDERS[index])) {
                String element = line.substring(line.indexOf(ORDERS[index]) + ORDERS[index].length());
                index++;
                if(index == ORDERS.length) {
                    authors = element.split(AUTHOR_DELIMITER_A);
                    break;
                }

                if (sb.length() == 0) {
                    paperId = MiscUtil.addZero(element, FORMAT_SIZE);
                    sb.append(paperId);
                } else {
                    if (!sb.toString().contains(Config.FIRST_DELIMITER)) {
                        element = formatDateStr(element);
                        if (element.length() < 4) {
                            index = -1;
                            break;
                        } else {
                            element = element.substring(0, 4);
                        }
                    }
                    sb.append(Config.FIRST_DELIMITER + element);
                }
            }
        }

        if (index < ORDERS.length || !refMap.containsKey(paperId)) {
            System.err.println("Incomplete paper ID:" + paperId);
            return;
        }

        String refListStr = refMap.get(paperId);
        StringBuilder authorSb = new StringBuilder();
        List<String> authorList = splitAuthors(authors);
        boolean first = authorIdMap.isEmpty();
        for (String author : authorList) {
            if (!authorIdMap.containsKey(author)) {
                authorIdMap.put(author, authorIdMap.size());
            }

            int authorId = authorIdMap.get(author);
            String value = authorSb.length() == 0 ? String.valueOf(authorId)
                    : Config.SECOND_DELIMITER + String.valueOf(authorId);
            authorSb.append(value);
        }

        String outputLine = sb.toString() + Config.FIRST_DELIMITER
                + authorSb.toString() + Config.FIRST_DELIMITER + refListStr;
        FileUtil.overwriteFile(outputLine, first, outputFilePath);
    }

    private static void extract(String refFilePath, String abstractsDirPath,
                                String authorIdFilePath,  String outputFilePath) {
        HashMap<String, String> refMap = createRefMap(refFilePath);
        List<File> abstractFileList = FileUtil.getFileListR(abstractsDirPath);
        FileUtil.makeParentDir(outputFilePath);
        TreeMap<String, Integer> authorIdMap = new TreeMap<>();
        int size = abstractFileList.size();
        for (int i = 0; i < size; i++) {
            extract(abstractFileList.remove(0), refMap, authorIdMap, outputFilePath);
        }

        List<String> outputLineList = new ArrayList<>();
        for (String name : authorIdMap.keySet()) {
            outputLineList.add(name + Config.FIRST_DELIMITER + String.valueOf(authorIdMap.get(name)));
        }

        if (authorIdFilePath != null) {
            FileUtil.writeFile(outputLineList, authorIdFilePath);
        }
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cl = MiscUtil.setParams("FeatureExtractor for KDD Cup 2003 dataset", options, args);
        String refFilePath = cl.getOptionValue(REF_FILE_OPTION);
        String abstractsDirPath = cl.getOptionValue(ABSTRACTS_DIR_OPTION);
        String authorIdFilePath = cl.hasOption(AUTHOR_ID_FILE_OPTION) ? cl.getOptionValue(AUTHOR_ID_FILE_OPTION) : null;
        String outputFilePath = cl.getOptionValue(Config.OUTPUT_FILE_OPTION);
        extract(refFilePath, abstractsDirPath, authorIdFilePath, outputFilePath);
    }
}
