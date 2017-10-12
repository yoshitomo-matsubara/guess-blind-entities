package model;

import common.Config;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Author;
import structure.Paper;

import java.util.HashMap;
import java.util.Map;

public class LogisticRegressionModel extends SocialCitationModel {
    public static final String TYPE = "lr";
    public static final String NAME = "Logistic Regression Model";
    public static final int PARAM_SIZE = 7;
    protected static final String PARAM_OPTION = "param";
    protected Map<String, Double> commonIcfWeightMap, selfIcfWeightMap;
    protected double[] params;

    public LogisticRegressionModel(Author author, CommandLine cl) {
        super(author, cl);
        this.commonIcfWeightMap = new HashMap<>();
        this.selfIcfWeightMap = new HashMap<>();
    }

    public LogisticRegressionModel(String line) {
        super(line, true);
        this.commonIcfWeightMap = new HashMap<>();
        this.selfIcfWeightMap = new HashMap<>();
        String[] elements = line.split(Config.FIRST_DELIMITER);
        String[] refStrs = elements[4].split(Config.SECOND_DELIMITER);
        for (String refStr : refStrs) {
            String[] keyValue = refStr.split(Config.KEY_VALUE_DELIMITER);
            this.citeCountMap.put(keyValue[0], Integer.parseInt(keyValue[1]));
            this.commonIcfWeightMap.put(keyValue[0], Double.parseDouble(keyValue[2]));
        }

        this.totalCitationCount = Integer.parseInt(elements[5]);
        if (elements[7] != null && elements[7].length() > 0) {
            String[] socialStrs = elements[7].split(Config.SECOND_DELIMITER);
            for (String socialStr : socialStrs) {
                String[] keyValue = socialStr.split(Config.KEY_VALUE_DELIMITER);
                this.socialPaperCountMap.put(keyValue[0], Integer.parseInt(keyValue[1]));
                this.socialWeightMap.put(keyValue[0], Double.parseDouble(keyValue[2]));
            }
        }

        this.totalSocialCitationCount = Integer.parseInt(elements[8]);
        String[] icfWeightStrs = elements[9].split(Config.SECOND_DELIMITER);
        for (String icfWeightStr : icfWeightStrs) {
            String[] keyValue = icfWeightStr.split(Config.KEY_VALUE_DELIMITER);
            this.selfIcfWeightMap.put(keyValue[0], Double.parseDouble(keyValue[1]));
        }
    }

    public LogisticRegressionModel(String line, CommandLine cl) {
        this(line);
        String paramStr = cl.getOptionValue(PARAM_OPTION);
        String[] paramElements = paramStr.split(Config.OPTION_DELIMITER);
        this.params = new double[PARAM_SIZE];
        for (int i = 0; i < this.params.length; i++) {
            this.params[i] = Double.parseDouble(paramElements[i]);
        }
    }

    public static double logisticFunction(double[] values, double[] params) {
        double ip = 0.0d;
        for (int i = 0; i < values.length; i++) {
            ip += values[i] * params[i];
        }
        return 1.0d / (1.0d + Math.exp(-ip));
    }

    private double logisticFunction(double[] values) {
        return logisticFunction(values, this.params);
    }

    @Override
    public void train() {
        super.train();
    }

    public double[] calcCommonScores(Paper paper) {
        int[] commonScores = calcCounts(paper);
        double score = 0.0d;
        for (String refPaperId : paper.refPaperIds) {
            if (this.commonIcfWeightMap.containsKey(refPaperId)) {
                score += this.commonIcfWeightMap.get(refPaperId);
            }
        }
        return new double[]{score, (double) commonScores[1]};
    }

    public double[] calcSocialScores(Paper paper) {
        int[] socialScores = calcSocialCount(paper);
        double score = 0.0d;
        for (String refPaperId : paper.refPaperIds) {
            if (this.socialWeightMap.containsKey(refPaperId)) {
                score += this.socialWeightMap.get(refPaperId);
            }
        }
        return new double[]{score, (double) socialScores[1]};
    }

    public double[] calcSelfScores(Paper paper) {
        double score = 0.0d;
        int hitCount = 0;
        for (String refPaperId : paper.refPaperIds) {
            if (this.selfIcfWeightMap.containsKey(refPaperId)) {
                score += this.selfIcfWeightMap.get(refPaperId);
                hitCount++;
            }
        }
        return new double[]{score, (double) hitCount};
    }

    public static double[] extractPairValues(LogisticRegressionModel model, Paper paper) {
        double refPaperIdSize = (double) paper.refPaperIds.length;
        double[] commonScores = model.calcCommonScores(paper);
        double[] socialScores = model.calcSocialScores(paper);
        double[] selfScores = model.calcSelfScores(paper);
        double commonAveWeight = commonScores[0] / refPaperIdSize;
        double commonRefCoverage = commonScores[1] / refPaperIdSize;
        double socialAveWeight = socialScores[0] / refPaperIdSize;
        double socialRefCoverage = socialScores[1] / refPaperIdSize;
        return new double[]{commonAveWeight, commonRefCoverage, socialAveWeight, socialRefCoverage,
                selfScores[0], selfScores[1]};
    }

    public static double[] extractFeatureValues(LogisticRegressionModel model, Paper paper) {
        double[] featureValues = new double[PARAM_SIZE];
        featureValues[0] = 1.0d;
        // attributes from a pair of author and paper
        double[] pairValues = extractPairValues(model, paper);
        for (int i = 0; i < pairValues.length; i++) {
            featureValues[i + 1] = pairValues[i];
        }
        return featureValues;
    }

    @Override
    public void setInverseCitationFrequencyWeights(Map<String, Integer> totalCitationCountMap) {
        super.setInverseCitationFrequencyWeights(totalCitationCountMap);
        for (String commonPaperId : this.citeCountMap.keySet()) {
            int pseudoCount = totalCitationCountMap.getOrDefault(commonPaperId, 0) + 1;
            double icfWeight = (double) this.citeCountMap.get(commonPaperId)
                    * Math.log(this.totalTrainPaperSize / (double) pseudoCount);
            this.commonIcfWeightMap.put(commonPaperId, icfWeight);
        }

        for (String paperId : this.paperIds) {
            int pseudoCount = totalCitationCountMap.getOrDefault(paperId, 0) + 1;
            double icfWeight = Math.log(this.totalTrainPaperSize / (double) pseudoCount);
            this.selfIcfWeightMap.put(paperId, icfWeight);
        }
    }

    private boolean checkIfValidValue(double[] featureValues) {
        return featureValues[1] > 0.0d || featureValues[3] > 0.0d || featureValues[5] > 0.0d;
    }

    @Override
    public double estimate(Paper paper) {
        double[] featureValues = extractFeatureValues(this, paper);
        return checkIfValidValue(featureValues) ? logisticFunction(featureValues) : INVALID_VALUE;
    }

    public static void setOptions(Options options) {
        MiscUtil.setOption(PARAM_OPTION, true, false,
                "[param, optional] estimated parameters for " + NAME
                        + " (can be plural, separate with comma)", options);
    }

    public static boolean checkIfValid(String modelType, CommandLine cl) {
        return  modelType.equals(TYPE) && cl.hasOption(PARAM_OPTION);
    }

    public static boolean checkIfValid(String modelType) {
        return  modelType.equals(TYPE);
    }

    @Override
    public String toString() {
        // author ID, # of paper IDs, paper IDs, # of ref IDs, [ref ID:count:weight], # of citations, # of social IDs, [social ID:count:weight], # of social citations, [paper ID:weight]
        StringBuilder sb = new StringBuilder(super.toString(false));
        sb.append(Config.FIRST_DELIMITER + String.valueOf(this.citeCountMap.size()) + Config.FIRST_DELIMITER);
        boolean commonFirst = true;
        for (String refPaperId : this.citeCountMap.keySet()) {
            if (!commonFirst) {
                sb.append(Config.SECOND_DELIMITER);
            }

            commonFirst = false;
            int count = this.citeCountMap.get(refPaperId);
            double icfWeight = this.commonIcfWeightMap.get(refPaperId);
            sb.append(refPaperId + Config.KEY_VALUE_DELIMITER + String.valueOf(count)
                    + Config.KEY_VALUE_DELIMITER + String.valueOf(icfWeight));
        }

        sb.append(Config.FIRST_DELIMITER + String.valueOf(this.totalCitationCount) + Config.FIRST_DELIMITER
                + String.valueOf(this.socialWeightMap.size()) + Config.FIRST_DELIMITER);
        boolean socialFirst = true;
        for (String refPaperId : this.socialPaperCountMap.keySet()) {
            if (!socialFirst) {
                sb.append(Config.SECOND_DELIMITER);
            }

            socialFirst = false;
            int socialCount = this.socialPaperCountMap.get(refPaperId);
            double socialIcfWeight = this.socialWeightMap.get(refPaperId);
            sb.append(refPaperId + Config.KEY_VALUE_DELIMITER + String.valueOf(socialCount)
                    + Config.KEY_VALUE_DELIMITER + String.valueOf(socialIcfWeight));
        }

        sb.append(Config.FIRST_DELIMITER + String.valueOf(this.totalSocialCitationCount) + Config.FIRST_DELIMITER);
        boolean selfFirst = true;
        for (String paperId : this.selfIcfWeightMap.keySet()) {
            if (!selfFirst) {
                sb.append(Config.SECOND_DELIMITER);
            }

            selfFirst = false;
            double icfWeight = this.selfIcfWeightMap.get(paperId);
            sb.append(paperId + Config.KEY_VALUE_DELIMITER + String.valueOf(icfWeight));
        }
        return sb.toString();
    }
}
