package model;

import common.Config;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Author;
import structure.Paper;

import java.util.HashMap;

public class LogisticRegressionModel extends SocialCitationModel {
    public static final String TYPE = "lr";
    public static final String NAME = "Logistic Regression Model";
    public static final int PARAM_SIZE = 6;
    protected static final String PARAM_OPTION = "param";
    protected double[] params;

    public LogisticRegressionModel(Author author, CommandLine cl) {
        super(author, cl);
    }

    public LogisticRegressionModel(String line) {
        super(line);
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

    public static double[] extractPairValues(LogisticRegressionModel model, Paper paper) {
        int[] counts = model.calcCounts(paper);
        int[] socialCounts = model.calcSocialCount(paper);
        double paperAvgRefHitCount = (double) counts[0] / (double) paper.refPaperIds.length;
        double paperRefCoverage = (double) counts[1] / (double) paper.refPaperIds.length;
        double paperAvgSocialHitCount = (double) socialCounts[0] / (double) paper.refPaperIds.length;
        double paperSocialCoverage = (double) socialCounts[1] / (double) paper.refPaperIds.length;
        int selfCiteCount = 0;
        for (String refPaperId : paper.refPaperIds) {
            if (model.checkIfMyPaper(refPaperId)) {
                selfCiteCount++;
            }
        }
        return new double[]{paperAvgRefHitCount, paperRefCoverage, paperAvgSocialHitCount, paperSocialCoverage,
                (double) selfCiteCount};
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
        String str = super.toString(true);
        // author ID, # of paper IDs, paper IDs, # of ref IDs, [ref ID:count], # of citations, # of social IDs, [social ID:count], # of social citations
        StringBuilder sb = new StringBuilder(str);
        return sb.toString();
    }
}
