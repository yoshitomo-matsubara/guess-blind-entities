package model;

import common.Config;
import structure.Author;
import structure.Paper;

import java.util.HashMap;

public abstract class BaseModel {
    public static final String TYPE = "ab";
    public static final String NAME = "Abstract Model";

    public static final double INVALID_VALUE = -Double.MAX_VALUE;
    public final String authorId;
    protected Author author;
    protected String[] paperIds;
    protected HashMap<String, Integer> citeCountMap;

    public BaseModel(Author author) {
        this.authorId = author.id;
        this.author = author;
        this.paperIds = new String[author.papers.length];
        for (int i = 0; i < this.paperIds.length; i++) {
            this.paperIds[i] = author.papers[i].id;
        }
        this.citeCountMap = new HashMap<>();
    }

    public BaseModel(String line) {
        String[] elements = line.split(Config.FIRST_DELIMITER);
        this.authorId = elements[0];
        this.author = null;
        String[] paperIds = elements[2].split(Config.SECOND_DELIMITER);
        this.paperIds = new String[paperIds.length];
        for (int i = 0; i < this.paperIds.length; i++) {
            this.paperIds[i] = paperIds[i];
        }

        this.citeCountMap = new HashMap<>();
        String[] refStrs = elements[3].split(Config.SECOND_DELIMITER);
        for (String refStr : refStrs) {
            String[] keyValue = refStr.split(Config.KEY_VALUE_DELIMITER);
            this.citeCountMap.put(keyValue[0], Integer.parseInt(keyValue[1]));
        }
    }

    public void train() {
        for (Paper paper : this.author.papers) {
            for (String refPaperId : paper.refPaperIds) {
                if (!this.citeCountMap.containsKey(refPaperId)) {
                    this.citeCountMap.put(refPaperId, 1);
                } else {
                    this.citeCountMap.put(refPaperId, this.citeCountMap.get(refPaperId) + 1);
                }
            }
        }
    }

    public abstract double estimate(Paper paper);

    public String toString() {
        // author ID, # of papers, paper IDs, [refID:count]
        StringBuilder sb = new StringBuilder(this.authorId + Config.FIRST_DELIMITER
                + String.valueOf(this.author.papers.length) + Config.FIRST_DELIMITER);
        for (int i = 0; i < this.paperIds.length; i++) {
            String str = i == 0 ? this.paperIds[i] : Config.SECOND_DELIMITER + this.paperIds[i];
            sb.append(str);
        }

        int sbSize = sb.length();
        for (String refId : this.citeCountMap.keySet()) {
            if (sb.length() != sbSize) {
                sb.append(Config.SECOND_DELIMITER);
            }
            sb.append(Config.FIRST_DELIMITER + refId + Config.KEY_VALUE_DELIMITER
                    + String.valueOf(this.citeCountMap.get(refId)));
        }
        return sb.toString();
    }
}
