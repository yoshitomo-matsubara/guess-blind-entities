package model;

import common.Config;
import structure.Author;
import structure.Paper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public abstract class BaseModel {
    public static final String TYPE = "ab";
    public static final String NAME = "Base Model";

    public static final double INVALID_VALUE = -Double.MAX_VALUE;
    public final String authorId;
    public final String[] paperIds;
    protected Author author;
    protected HashSet<String> paperIdSet;
    protected HashMap<String, Integer> citeCountMap;
    protected int totalCitationCount;

    public BaseModel(Author author) {
        this.authorId = author.id;
        this.author = author;
        this.paperIds = new String[author.papers.length];
        this.paperIdSet = new HashSet<>();
        for (int i = 0; i < this.paperIds.length; i++) {
            this.paperIds[i] = author.papers[i].id;
        }

        this.citeCountMap = new HashMap<>();
        this.totalCitationCount = 0;
    }

    public BaseModel(String line, boolean includesCommonCitationModel) {
        String[] elements = line.split(Config.FIRST_DELIMITER);
        this.authorId = elements[0];
        this.author = null;
        String[] paperIds = elements[2].split(Config.SECOND_DELIMITER);
        this.paperIds = new String[paperIds.length];
        this.paperIdSet = new HashSet<>();
        for (int i = 0; i < this.paperIds.length; i++) {
            this.paperIds[i] = paperIds[i];
            this.paperIdSet.add(paperIds[i]);
        }

        this.citeCountMap = new HashMap<>();
        if (includesCommonCitationModel) {
            return;
        }

        String[] refStrs = elements[4].split(Config.SECOND_DELIMITER);
        for (String refStr : refStrs) {
            String[] keyValue = refStr.split(Config.KEY_VALUE_DELIMITER);
            this.citeCountMap.put(keyValue[0], Integer.parseInt(keyValue[1]));
        }
        this.totalCitationCount = Integer.parseInt(elements[5]);
    }

    public BaseModel(String line) {
        this(line, false);
    }

    public void train() {
        for (Paper paper : this.author.papers) {
            for (String refPaperId : paper.refPaperIds) {
                if (!this.citeCountMap.containsKey(refPaperId)) {
                    this.citeCountMap.put(refPaperId, 1);
                } else {
                    this.citeCountMap.put(refPaperId, this.citeCountMap.get(refPaperId) + 1);
                }
                this.totalCitationCount++;
            }
        }
    }

    public void setSocialPaperIds(List<BaseModel> allModelList, HashMap<String, Integer> modelIdMap) {}

    public abstract double estimate(Paper paper);

    public boolean checkIfMyPaper(String paperId) {
        return this.paperIdSet.contains(paperId);
    }

    public int getCitationIdSize() {
        return this.citeCountMap.size();
    }

    public int getTotalCitationCount() {
        return this.totalCitationCount;
    }

    public int getSocialCitationIdSize() {
        return -1;
    }

    public void shareCitationCounts(HashMap<String, Integer> totalCitationCountMap) {
        for (String refPaperId : this.citeCountMap.keySet()) {
            int count = totalCitationCountMap.getOrDefault(refPaperId, 0);
            totalCitationCountMap.put(refPaperId, count + this.citeCountMap.get(refPaperId));
        }
    }

    public void setInverseCitationFrequencyWeights(HashMap<String, Integer> totalCitationCountMap) {}

    public int[] calcCounts(Paper paper) {
        int score = 0;
        int hitCount = 0;
        for (String refPaperId : paper.refPaperIds) {
            if (this.citeCountMap.containsKey(refPaperId)) {
                score += this.citeCountMap.get(refPaperId);
                hitCount++;
            }
        }
        return new int[]{score, hitCount};
    }

    public String toString(boolean isCommonCitationModel) {
        // author ID, # of paper IDs, paper IDs, # of ref IDs
        StringBuilder sb = new StringBuilder(this.authorId + Config.FIRST_DELIMITER
                + String.valueOf(this.author.papers.length) + Config.FIRST_DELIMITER);
        for (int i = 0; i < this.paperIds.length; i++) {
            String str = i == 0 ? this.paperIds[i] : Config.SECOND_DELIMITER + this.paperIds[i];
            sb.append(str);
        }

        sb.append(Config.FIRST_DELIMITER + String.valueOf(this.citeCountMap.size()));
        if (!isCommonCitationModel) {
            // author ID, # of paper IDs, paper IDs, # of ref IDs, [ref ID:count], # of citations
            sb.append(Config.FIRST_DELIMITER);
            boolean first = true;
            for (String refId : this.citeCountMap.keySet()) {
                if (!first) {
                    sb.append(Config.SECOND_DELIMITER);
                }

                first = false;
                int count = this.citeCountMap.get(refId);
                sb.append(refId + Config.KEY_VALUE_DELIMITER + String.valueOf(count));
            }
            sb.append(Config.FIRST_DELIMITER + String.valueOf(this.totalCitationCount));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toString(false);
    }
}
