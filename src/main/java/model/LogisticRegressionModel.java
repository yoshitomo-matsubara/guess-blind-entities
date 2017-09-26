package model;

import common.Config;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Author;
import structure.Paper;

import java.util.HashMap;

public class LogisticRegressionModel extends FellowCitationModel {
    public static final String TYPE = "lr";
    public static final String NAME = "Logistic Regression Model";
    public static final int PARAM_SIZE = 6;
    private static final String PARAM_OPTION = "param";
    private double[] params;

    public LogisticRegressionModel(Author author) {
        super(author);
    }

    public LogisticRegressionModel(String line) {
        super(line);
        String[] elements = line.split(Config.FIRST_DELIMITER);
        String[] fellowStrs = elements[7].split(Config.SECOND_DELIMITER);
        for (String fellowStr : fellowStrs) {
            String[] keyValue = fellowStr.split(Config.KEY_VALUE_DELIMITER);
            this.fellowPaperCountMap.put(keyValue[0], Integer.parseInt(keyValue[1]));
        }
        this.totalCitationCount = Integer.parseInt(elements[8]);
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

    public static double[] extractPairValues(FellowCitationModel model, Paper paper) {
        int[] counts = model.calcCounts(paper);
        int[] fellowCounts = model.calcFellowCount(paper);
        double paperAvgRefHitCount = (double) counts[0] / (double) paper.refPaperIds.length;
        double paperRefCoverage = (double) counts[1] / (double) paper.refPaperIds.length;
        double paperAvgFellowHitCount = (double) fellowCounts[0] / (double) paper.refPaperIds.length;
        double paperFellowCoverage = (double) fellowCounts[1] / (double) paper.refPaperIds.length;
        int selfCiteCount = 0;
        for (String refPaperId : paper.refPaperIds) {
            if (model.checkIfMyPaper(refPaperId)) {
                selfCiteCount++;
            }
        }
        return new double[]{paperAvgRefHitCount, paperRefCoverage, paperAvgFellowHitCount, paperFellowCoverage, (double) selfCiteCount};
    }

    public static double[] extractFeatureValues(FellowCitationModel model, Paper paper) {
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
    public double estimate(Paper paper) {
        double[] featureValues = extractFeatureValues(this, paper);
        return featureValues[0] > 0.0d || featureValues[2] > 0.0d || featureValues[4] > 0.0d ? logisticFunction(featureValues) : INVALID_VALUE;
    }

    public static void setOptions(Options options) {
        MiscUtil.setOption(PARAM_OPTION, true, false,
                "[param, optional] estimated parameters for " + NAME
                        + " (can be plural, separate with comma)", options);
    }

    public static boolean checkIfValid(String modelType, CommandLine cl) {
        return  modelType.equals(TYPE) && cl.hasOption(PARAM_OPTION);
    }

    @Override
    public String toString() {
        String str = super.toString();
        // author ID, # of paper IDs, paper IDs, # of ref IDs, [ref ID:count], # of citations, # of fellow IDs, [fellow ID:count], # of fellow citations
        StringBuilder sb = new StringBuilder(str);
        sb.append(Config.FIRST_DELIMITER + String.valueOf(this.fellowPaperCountMap.size()) + Config.FIRST_DELIMITER);
        boolean fellowFirst = true;
        for (String refId : this.fellowPaperCountMap.keySet()) {
            if (!fellowFirst) {
                sb.append(Config.SECOND_DELIMITER);
            }

            fellowFirst = false;
            int count = this.fellowPaperCountMap.get(refId);
            sb.append(refId + Config.KEY_VALUE_DELIMITER + String.valueOf(count));
        }

        sb.append(Config.FIRST_DELIMITER + String.valueOf(this.totalFellowCitationCount));
        return sb.toString();
    }
}
