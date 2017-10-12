package model;

import common.Config;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Author;
import structure.Paper;

import java.util.HashMap;
import java.util.Map;

public class HillProvostBestModel extends BaseModel {
    public static final String TYPE = "hpb";
    public static final String NAME = "Hill & Provost's Best Model";
    protected static final String TRAIN_SIZE_OPTION = "trainsize";
    protected Map<String, Double> weightMap;
    protected double totalTrainPaperSize;

    public HillProvostBestModel(Author author, CommandLine cl) {
        super(author);
        this.totalTrainPaperSize = Double.parseDouble(cl.getOptionValue(TRAIN_SIZE_OPTION));
        this.weightMap = new HashMap<>();
    }

    public HillProvostBestModel(String line) {
        super(line);
        this.weightMap = new HashMap<>();
        String[] elements = line.split(Config.FIRST_DELIMITER);
        String[] icfWeightStrs = elements[6].split(Config.SECOND_DELIMITER);
        for (String icfWeightStr : icfWeightStrs) {
            String[] keyValue = icfWeightStr.split(Config.KEY_VALUE_DELIMITER);
            this.weightMap.put(keyValue[0], Double.parseDouble(keyValue[1]));
        }
    }

    @Override
    public void train() {
        super.train();
    }

    public void setInverseCitationFrequencyWeights(Map<String, Integer> totalCitationCountMap) {
        for (String paperId : this.paperIds) {
            int pseudoCount = totalCitationCountMap.getOrDefault(paperId, 0) + 1;
            double icfWeight = Math.log(this.totalTrainPaperSize / (double) pseudoCount);
            this.weightMap.put(paperId, icfWeight);
        }
    }

    @Override
    public double estimate(Paper paper) {
        double score = 0.0d;
        int hitCount = 0;
        for (String refPaperId : paper.refPaperIds) {
            if (this.weightMap.containsKey(refPaperId)) {
                score += this.weightMap.get(refPaperId);
                hitCount++;
            }
        }
        return hitCount > 0 ? score : INVALID_VALUE;
    }

    public static void setOptions(Options options) {
        MiscUtil.setOption(TRAIN_SIZE_OPTION, true, false,
                "[param] training paper size for " + NAME, options);
    }

    public static boolean checkIfValid(String modelType, CommandLine cl) {
        return modelType.equals(TYPE) && cl.hasOption(TRAIN_SIZE_OPTION);
    }

    public static boolean checkIfValid(String modelType) {
        return modelType.equals(TYPE);
    }

    @Override
    public String toString() {
        // author ID, # of paper IDs, paper IDs, # of ref IDs, [ref ID:count], # of citations, [paper ID:weight]
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(Config.FIRST_DELIMITER);
        boolean first = true;
        for (String paperId : this.weightMap.keySet()) {
            if (!first) {
                sb.append(Config.SECOND_DELIMITER);
            }

            first = false;
            double icfWeight = this.weightMap.get(paperId);
            sb.append(paperId + Config.KEY_VALUE_DELIMITER + String.valueOf(icfWeight));
        }
        return sb.toString();
    }
}
