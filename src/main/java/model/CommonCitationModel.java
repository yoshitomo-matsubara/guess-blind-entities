package model;

import common.Config;
import org.apache.commons.cli.CommandLine;
import structure.Author;
import structure.Paper;

import java.util.HashMap;

public class CommonCitationModel extends BaseModel {
    public static final String TYPE = "cc";
    public static final String NAME = "Common Citation Model";
    private static final String TRAIN_SIZE_OPTION = "trainsize";
    private HashMap<String, Double> icfWeightMap;
    private double totalTrainPaperSize;

    public CommonCitationModel(Author author, CommandLine cl) {
        super(author);
        this.totalTrainPaperSize = Double.parseDouble(cl.getOptionValue(TRAIN_SIZE_OPTION));
        this.icfWeightMap = new HashMap<>();
    }

    public CommonCitationModel(String line) {
        super(line);
        this.icfWeightMap = new HashMap<>();
        String[] elements = line.split(Config.FIRST_DELIMITER);
        if (elements.length > 6 && elements[6].length() > 0) {
            String[] icfWeightStrs = elements[6].split(Config.SECOND_DELIMITER);
            for (String icfWeightStr : icfWeightStrs) {
                String[] keyValue = icfWeightStr.split(Config.KEY_VALUE_DELIMITER);
                this.icfWeightMap.put(keyValue[0], Double.parseDouble(keyValue[1]));
            }
        }
    }

    @Override
    public void train() {
        super.train();
    }

    @Override
    public void setInverseCitationFrequencyWeights(HashMap<String, Integer> totalCitationCountMap) {
        for (String refPaperId : this.citeCountMap.keySet()) {
            int pseudoCount = totalCitationCountMap.getOrDefault(refPaperId, 0) + 1;
            double icfWeight = (double) this.citeCountMap.get(refPaperId)
                    * Math.log(this.totalTrainPaperSize / (double) pseudoCount);
            this.icfWeightMap.put(refPaperId, icfWeight);
        }
    }

    @Override
    public double estimate(Paper paper) {
        double score = 0.0d;
        int hitCount = 0;
        for (String refPaperId : paper.refPaperIds) {
            if (this.icfWeightMap.containsKey(refPaperId)) {
                score += this.icfWeightMap.get(refPaperId);
                hitCount++;
            }
        }
        return hitCount > 0 ? score : INVALID_VALUE;
    }

    public static boolean checkIfValid(String modelType, CommandLine cl) {
        return modelType.equals(TYPE) && cl.hasOption(TRAIN_SIZE_OPTION);
    }

    public static boolean checkIfValid(String modelType) {
        return modelType.equals(TYPE);
    }

    @Override
    public String toString() {
        // author ID, # of paper IDs, paper IDs, # of ref IDs, [ref ID:count], # of citations, [paper ID:icf weight]
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(Config.FIRST_DELIMITER);
        boolean first = true;
        for (String paperId : this.icfWeightMap.keySet()) {
            if (!first) {
                sb.append(Config.SECOND_DELIMITER);
            }

            first = false;
            double icfWeight = this.icfWeightMap.get(paperId);
            sb.append(paperId + Config.KEY_VALUE_DELIMITER + String.valueOf(icfWeight));
        }
        return sb.toString();
    }
}
