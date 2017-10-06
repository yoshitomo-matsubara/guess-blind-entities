package model;

import common.Config;
import org.apache.commons.cli.CommandLine;
import structure.Author;
import structure.Paper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class SocialCitationModel extends BaseModel {
    public static final String TYPE = "sc";
    public static final String NAME = "Social Citation Model";
    protected static final String TRAIN_SIZE_OPTION = "trainsize";
    protected HashSet<String> coauthorIdSet;
    protected HashMap<String, Integer> socialPaperCountMap;
    protected HashMap<String, Double> socialIcfWeightMap;
    protected double totalTrainPaperSize;
    protected int totalSocialCitationCount;

    public SocialCitationModel(Author author, CommandLine cl) {
        super(author);
        this.totalTrainPaperSize = Double.parseDouble(cl.getOptionValue(TRAIN_SIZE_OPTION));
        this.coauthorIdSet = new HashSet<>();
        this.socialPaperCountMap = new HashMap<>();
        this.socialIcfWeightMap = new HashMap<>();
    }

    public SocialCitationModel(String line) {
        super(line);
        this.coauthorIdSet = new HashSet<>();
        this.socialPaperCountMap = new HashMap<>();
        this.socialIcfWeightMap = new HashMap<>();
        String[] elements = line.split(Config.FIRST_DELIMITER);
        String[] socialStrs = elements[7].split(Config.SECOND_DELIMITER);
        for (String socialStr : socialStrs) {
            String[] keyValue = socialStr.split(Config.KEY_VALUE_DELIMITER);
            this.socialPaperCountMap.put(keyValue[0], Integer.parseInt(keyValue[1]));
            this.socialIcfWeightMap.put(keyValue[0], Double.parseDouble(keyValue[2]));
        }
        this.totalSocialCitationCount = Integer.parseInt(elements[8]);
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
    public void setSocialPaperIds(List<BaseModel> allModelList, HashMap<String, Integer> modelIdMap) {
        Iterator<String> ite = this.coauthorIdSet.iterator();
        while (ite.hasNext()) {
            String coauthorId = ite.next();
            if (modelIdMap.containsKey(coauthorId)) {
                int coauthorIndex = modelIdMap.get(coauthorId);
                BaseModel model = allModelList.get(coauthorIndex);
                for (String socialPaperId : model.paperIds) {
                    if (this.paperIdSet.contains(socialPaperId)) {
                        continue;
                    }

                    if (!this.socialPaperCountMap.containsKey(socialPaperId)) {
                        this.socialPaperCountMap.put(socialPaperId, 1);
                    } else {
                        this.socialPaperCountMap.put(socialPaperId, this.socialPaperCountMap.get(socialPaperId) + 1);
                    }
                    this.totalSocialCitationCount++;
                }
            }
        }
    }

    @Override
    public void setInverseCitationFrequencyWeights(HashMap<String, Integer> totalCitationCountMap) {
        for (String socialPaperId : this.socialPaperCountMap.keySet()) {
            int pseudoCount = totalCitationCountMap.getOrDefault(socialPaperId, 0) + 1;
            double icfWeight = (double) this.socialPaperCountMap.get(socialPaperId)
                    * Math.log(this.totalTrainPaperSize / (double) pseudoCount);
            this.socialIcfWeightMap.put(socialPaperId, icfWeight);
        }
    }

    public int[] calcSocialCount(Paper paper) {
        int score = 0;
        int hitCount = 0;
        for (String refPaperId : paper.refPaperIds) {
            if (this.socialPaperCountMap.containsKey(refPaperId)) {
                score += this.socialPaperCountMap.get(refPaperId);
                hitCount++;
            }
        }
        return new int[]{score, hitCount};
    }

    @Override
    public double estimate(Paper paper) {
        double score = 0.0d;
        int hitCount = 0;
        for (String refPaperId : paper.refPaperIds) {
            if (this.socialIcfWeightMap.containsKey(refPaperId)) {
                score += this.socialIcfWeightMap.get(refPaperId);
                hitCount++;
            }
        }
        return hitCount > 0 ? score : INVALID_VALUE;
    }

    @Override
    public int getSocialCitationIdSize() {
        return this.socialPaperCountMap.size();
    }

    public static boolean checkIfValid(String modelType, CommandLine cl) {
        return modelType.equals(TYPE) && cl.hasOption(TRAIN_SIZE_OPTION);
    }

    public static boolean checkIfValid(String modelType) {
        return modelType.equals(TYPE);
    }

    public String toString(boolean includeCommon) {
        StringBuilder sb = new StringBuilder();
        if (includeCommon) {
            sb.append(super.toString());
        } else {
            sb.append(this.authorId + Config.FIRST_DELIMITER + String.valueOf(this.author.papers.length)
                    + Config.FIRST_DELIMITER);
            for (int i = 0; i < this.paperIds.length; i++) {
                String str = i == 0 ? this.paperIds[i] : Config.SECOND_DELIMITER + this.paperIds[i];
                sb.append(str);
            }
        }

        sb.append(Config.FIRST_DELIMITER + String.valueOf(this.socialIcfWeightMap.size()) + Config.FIRST_DELIMITER);
        boolean first = true;
        for (String refId : this.socialPaperCountMap.keySet()) {
            if (!first) {
                sb.append(Config.SECOND_DELIMITER);
            }

            first = false;
            int socialCount = this.socialPaperCountMap.get(refId);
            double socialIcfWeight = this.socialIcfWeightMap.get(refId);
            sb.append(refId + Config.KEY_VALUE_DELIMITER + String.valueOf(socialCount)
                    + Config.KEY_VALUE_DELIMITER + String.valueOf(socialIcfWeight));
        }

        sb.append(Config.FIRST_DELIMITER + String.valueOf(this.totalSocialCitationCount));
        return sb.toString();
    }

    @Override
    public String toString() {
        // author ID, # of paper IDs, paper IDs, # of ref IDs, [ref ID:count], # of citations, # of social IDs, [social ID:count:icf weight], # of social citations
        return toString(true);
    }
}
