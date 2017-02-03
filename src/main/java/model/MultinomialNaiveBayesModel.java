package model;

import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import structure.Author;
import structure.Paper;

public class MultinomialNaiveBayesModel extends BaseModel {
    public static final String TYPE = "mnb";
    public static final String NAME = "Multinomial Naive Bayes Based Model";
    private static final String TOTAL_OVERLAP_PAPER_ID_SIZE_OPTION = "topis";
    private static final String TOTAL_UNIQUE_CITATION_SIZE_OPTION = "tucs";
    private static final String SMOOTHING_PRIOR_OPTION = "sp";
    private final int totalOverlapPaperSize, totalCitationIdSize;
    private final double alpha, logPa;
    private int totalCitationCount;
    private double nonHitLogProb;

    public MultinomialNaiveBayesModel(Author author, CommandLine cl) {
        super(author);
        this.totalOverlapPaperSize = Integer.parseInt(cl.getOptionValue(TOTAL_OVERLAP_PAPER_ID_SIZE_OPTION));
        this.totalCitationIdSize = Integer.parseInt(cl.getOptionValue(TOTAL_UNIQUE_CITATION_SIZE_OPTION));
        this.alpha = Double.parseDouble(cl.getOptionValue(SMOOTHING_PRIOR_OPTION));
        this.logPa = Math.log((double) this.paperIds.length / (double) this.totalOverlapPaperSize);
        this.totalCitationCount = 0;
        this.nonHitLogProb = 0.0d;
    }

    private double calcProb(int count) {
        double numerator = (double) count + this.alpha;
        double denominator = (double) this.totalCitationCount + this.alpha * (double) this.totalCitationIdSize;
        return numerator / denominator;
    }

    @Override
    public void train() {
        super.train();
        for (String refPaperId : this.citeCountMap.keySet()) {
            this.totalCitationCount += this.citeCountMap.get(refPaperId);
        }
        this.nonHitLogProb = Math.log(calcProb(0));
    }

    @Override
    public double estimate(Paper paper) {
        double logProb = this.logPa;
        int hitCount = 0;
        for (String refPaperId : paper.refPaperIds) {
            if (this.citeCountMap.containsKey(refPaperId)) {
                double prob = calcProb(this.citeCountMap.get(refPaperId));
                logProb += Math.log(prob);
                hitCount++;
            } else {
                logProb += this.nonHitLogProb;
            }
        }
        return hitCount > 0 ? Math.exp(logProb) : INVALID_VALUE;
    }

    public static void setOptions(Options options) {
        MiscUtil.setOption(TOTAL_OVERLAP_PAPER_ID_SIZE_OPTION, true, false,
                "[param, optional] total number of overlap papers in training for " + NAME, options);
        MiscUtil.setOption(TOTAL_UNIQUE_CITATION_SIZE_OPTION, true, false,
                "[param, optional] total number of unique reference IDs in training for " + NAME, options);
        MiscUtil.setOption(SMOOTHING_PRIOR_OPTION, true, false,
                "[param, optional] smoothing prior alpha(0 <= alpha <= 1) for " + NAME
                        + ", Laplace smoothing: alpha = 1, Lidstone smoothing: alpha < 1", options);
    }

    public static boolean checkIfValid(String modelType, CommandLine cl) {
        return modelType.equals(TYPE) && cl.hasOption(TOTAL_OVERLAP_PAPER_ID_SIZE_OPTION)
                && cl.hasOption(TOTAL_UNIQUE_CITATION_SIZE_OPTION) && cl.hasOption(SMOOTHING_PRIOR_OPTION);
    }
}
