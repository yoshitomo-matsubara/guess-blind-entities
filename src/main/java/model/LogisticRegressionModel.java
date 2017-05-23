package model;

import common.Config;
import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Paper;

public class LogisticRegressionModel extends BaseModel {
    public static final String TYPE = "lr";
    public static final String NAME = "Logistic Regression Model";
    public static final int PARAM_SIZE = 7;
    private static final String PARAM_OPTION = "param";
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

    public static double[] extractPairValues(BaseModel model, Paper paper) {
        int[] counts = model.calcCounts(paper);
        int selfCiteCount = 0;
        double countUpScore = (double) counts[0] / (double) model.getTotalCitationCount();
        double authorRefCoverage = (double) counts[1] / (double) model.getCitationIdSize();
        for (String refPaperId : paper.refPaperIds) {
            if (model.checkIfPaper(refPaperId)) {
                selfCiteCount++;
            }
        }
        return new double[]{(double) selfCiteCount, countUpScore, (double) model.getTotalCitationCount(),
                authorRefCoverage, (double) paper.refPaperIds.length, (double) model.paperIds.length};
    }

    public static double[] extractFeatureValues(BaseModel model, Paper paper) {
        double[] featureValues = new double[PARAM_SIZE];
        featureValues[0] = 1.0d;
        // attributes from a pair of author and paper
        double[] pairValues = extractPairValues(model, paper);
        for (int i = 0; i < pairValues.length; i++) {
            featureValues[i + 1] = Math.log(pairValues[i] + 1.0d);
        }
        return featureValues;
    }

    @Override
    public double estimate(Paper paper) {
        int[] counts = calcCounts(paper);
        double[] featureValues = extractFeatureValues(this, paper);
        return counts[1] > 0 ? logisticFunction(featureValues) : INVALID_VALUE;
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
