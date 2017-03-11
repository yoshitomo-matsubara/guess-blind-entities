package model;

import common.Config;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Author;
import structure.Paper;

public class LogisticRegressionModel extends BaseModel {
    public static final String TYPE = "lr";
    public static final String NAME = "Logistic Regression Model";
    public static final int PARAM_SIZE = 9;
    private static final String PARAM_OPTION = "param";
    private static final double LOG_OFFSET = 1.0d;
    private double[] params;

    public LogisticRegressionModel(String line, CommandLine cl) {
        super(line);
        String paramStr = cl.getOptionValue(PARAM_OPTION);
        String[] elements = paramStr.split(Config.OPTION_DELIMITER);
        this.params = new double[PARAM_SIZE];
        for (int i = 0; i < this.params.length; i++) {
            this.params[i] = Double.parseDouble(elements[i]);
        }
    }

    private double logisticFunction(double[] values) {
        double ip = 0.0d;
        for (int i = 0; i < values.length; i++) {
            ip += values[i] * this.params[i];
        }
        return 1.0d / (1.0d + Math.exp(-ip));
    }

    @Override
    public void train() {
        super.train();
    }

    public static double[] extractPairValues(BaseModel model, Paper paper) {
        int[] counts = model.calcCounts(paper);
        double countUpScore = (double) counts[0] / (double) model.getTotalCitationCount();
        double authorRefCoverage = (double) counts[1] / (double) model.getCitationIdSize();
        double paperAvgRefHitCount = (double) counts[0] / (double) paper.refPaperIds.length;
        double paperRefCoverage = (double) counts[1] / (double) paper.refPaperIds.length;
        return new double[]{countUpScore, authorRefCoverage, paperAvgRefHitCount, paperRefCoverage};
    }

    public static double[] extractFeatureValues(BaseModel model, Paper paper) {
        double[] featureValues = new double[PARAM_SIZE];
        featureValues[0] = 1.0d;
        // author's attributes
        featureValues[1] = Math.log((double) model.paperIds.length + LOG_OFFSET);
        featureValues[2] = Math.log((double) model.getCitationIdSize() + LOG_OFFSET);
        featureValues[3] = Math.log((double) model.getTotalCitationCount() + LOG_OFFSET);
        // paper's attribute
        featureValues[4] = Math.log((double) paper.refPaperIds.length + LOG_OFFSET);
        // attributes from a pair of author and paper
        double[] pairValues = extractPairValues(model, paper);
        for (int i = 0; i < pairValues.length; i++) {
            featureValues[i + 5] = Math.log(pairValues[i] + LOG_OFFSET);
        }
        return featureValues;
    }

    @Override
    public double estimate(Paper paper) {
        double[] featureValues = extractFeatureValues(this, paper);
        return logisticFunction(featureValues);
    }

    public static void setOptions(Options options) {
        MiscUtil.setOption(PARAM_OPTION, true, false,
                "[param, optional] estimated parameters for " + NAME
                        + " (can be plural, separate with comma)", options);
    }

    public static boolean checkIfValid(String modelType, CommandLine cl) {
        return  modelType.equals(TYPE) && cl.hasOption(PARAM_OPTION);
    }
}
