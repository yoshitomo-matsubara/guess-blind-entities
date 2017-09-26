package model;

import common.Config;
import structure.Author;
import structure.Paper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class FellowCitationModel extends BaseModel {
    public static final String TYPE = "fc";
    public static final String NAME = "Fellow-citation Model";
    protected HashSet<String> coauthorIdSet;
    protected HashMap<String, Integer> fellowPaperCountMap;
    protected int totalFellowCitationCount;

    public FellowCitationModel(Author author) {
        super(author);
        this.coauthorIdSet = new HashSet<>();
        this.fellowPaperCountMap = new HashMap<>();
    }

    public FellowCitationModel(String line) {
        super(line);
        this.coauthorIdSet = new HashSet<>();
        this.fellowPaperCountMap = new HashMap<>();
        this.totalFellowCitationCount = this.totalCitationCount;
    }

    @Override
    public void train() {
        super.train();
        for (Paper paper : this.author.papers) {
            Iterator<String> ite = paper.getAuthorSet().iterator();
            while (ite.hasNext()) {
                String coauthorId = ite.next();
                if (!coauthorId.equals(this.authorId)) {
                    this.coauthorIdSet.add(coauthorId);
                }
            }
        }
    }

    @Override
    public void setFellowPaperIds(List<BaseModel> allModelList, HashMap<String, Integer> modelIdMap) {
        Iterator<String> ite = this.coauthorIdSet.iterator();
        while (ite.hasNext()) {
            String coauthorId = ite.next();
            if (modelIdMap.containsKey(coauthorId)) {
                int coauthorIndex = modelIdMap.get(coauthorId);
                BaseModel model = allModelList.get(coauthorIndex);
                for (String fellowPaperId : model.paperIds) {
                    if (this.paperIdSet.contains(fellowPaperId)) {
                        continue;
                    }

                    if (!this.fellowPaperCountMap.containsKey(fellowPaperId)) {
                        this.fellowPaperCountMap.put(fellowPaperId, 1);
                    } else {
                        this.fellowPaperCountMap.put(fellowPaperId, this.fellowPaperCountMap.get(fellowPaperId) + 1);
                    }
                    this.totalFellowCitationCount++;
                }
            }
        }
    }

    public int[] calcFellowCount(Paper paper) {
        int score = 0;
        int hitCount = 0;
        for (String refPaperId : paper.refPaperIds) {
            if (this.fellowPaperCountMap.containsKey(refPaperId)) {
                score += this.fellowPaperCountMap.get(refPaperId);
                hitCount++;
            }
        }
        return new int[]{score, hitCount};
    }

    @Override
    public double estimate(Paper paper) {
        int[] counts = calcFellowCount(paper);
        return counts[1] > 0 ? (double) counts[0] : INVALID_VALUE;
    }

    @Override
    public int getFellowCitationIdSize() {
        return this.fellowPaperCountMap.size();
    }

    public static boolean checkIfValid(String modelType) {
        return modelType.equals(TYPE);
    }

    @Override
    public String toString() {
        // author ID, # of paper IDs, paper IDs, # of fellow IDs, [fellow ID:count], # of fellow citations
        StringBuilder sb = new StringBuilder(this.authorId + Config.FIRST_DELIMITER
                + String.valueOf(this.author.papers.length) + Config.FIRST_DELIMITER);
        for (int i = 0; i < this.paperIds.length; i++) {
            String str = i == 0 ? this.paperIds[i] : Config.SECOND_DELIMITER + this.paperIds[i];
            sb.append(str);
        }

        sb.append(Config.FIRST_DELIMITER + String.valueOf(this.fellowPaperCountMap.size()) + Config.FIRST_DELIMITER);
        boolean first = true;
        for (String refId : this.fellowPaperCountMap.keySet()) {
            if (!first) {
                sb.append(Config.SECOND_DELIMITER);
            }

            first = false;
            int count = this.fellowPaperCountMap.get(refId);
            sb.append(refId + Config.KEY_VALUE_DELIMITER + String.valueOf(count));
        }

        sb.append(Config.FIRST_DELIMITER + String.valueOf(this.totalFellowCitationCount));
        return sb.toString();
    }
}
