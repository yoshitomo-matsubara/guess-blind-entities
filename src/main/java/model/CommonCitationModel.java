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
    protected HashMap<String, Double> icfWeightMap;
    protected double totalTrainPaperSize;

    public CommonCitationModel(Author author, CommandLine cl) {
        super(author);
        this.totalTrainPaperSize = Double.parseDouble(cl.getOptionValue(TRAIN_SIZE_OPTION));
        this.icfWeightMap = new HashMap<>();
    }

    public CommonCitationModel(String line) {
        super(line, true);
        this.icfWeightMap = new HashMap<>();
        String[] elements = line.split(Config.FIRST_DELIMITER);
        String[] refStrs = elements[4].split(Config.SECOND_DELIMITER);
        for (String refStr : refStrs) {
            String[] keyValue = refStr.split(Config.KEY_VALUE_DELIMITER);
            this.citeCountMap.put(keyValue[0], Integer.parseInt(keyValue[1]));
            this.icfWeightMap.put(keyValue[0], Double.parseDouble(keyValue[2]));
        }
        this.totalCitationCount = Integer.parseInt(elements[5]);
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
        // author ID, # of paper IDs, paper IDs, # of ref IDs, [ref ID:count:icf weight], # of citations
        StringBuilder sb = new StringBuilder(super.toString(true));
        sb.append(Config.FIRST_DELIMITER);
        boolean first = true;
        for (String refPaperId : this.citeCountMap.keySet()) {
            if (!first) {
                sb.append(Config.SECOND_DELIMITER);
            }

            first = false;
            int count = this.citeCountMap.get(refPaperId);
            double icfWeight = this.icfWeightMap.get(refPaperId);
            sb.append(refPaperId + Config.KEY_VALUE_DELIMITER + String.valueOf(count)
                    + Config.KEY_VALUE_DELIMITER + String.valueOf(icfWeight));
        }

        sb.append(Config.FIRST_DELIMITER + String.valueOf(this.totalCitationCount));
        return sb.toString();
    }
}
