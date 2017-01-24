package model;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import structure.Author;
import structure.Paper;

public class MultinomialNaiveBayesModel extends BaseModel {
    public static final String TYPE = "mnb";
    public static final String NAME = "Multinomial Naive Bayes Based Model";
    private static final String TOTAL_OVERLAP_PAPER_SIZE_OPTION = "tops";
    private static final String TOTAL_CITATION_ID_SIZE_OPTION = "tcis";
    private static final String SMOOTHING_PRIOR_OPTION = "sp";
    private final int totalOverlapPaperSize, totalCitationIdSize;
    private final double alpha, pa;
    private int totalCitationCount;
    private double nonHitLogProb;

    public MultinomialNaiveBayesModel(Author author, CommandLine cl) {
        super(author);
        this.totalOverlapPaperSize = Integer.parseInt(cl.getOptionValue(TOTAL_OVERLAP_PAPER_SIZE_OPTION));
        this.totalCitationIdSize = Integer.parseInt(cl.getOptionValue(TOTAL_CITATION_ID_SIZE_OPTION));
        this.alpha = Double.parseDouble(cl.getOptionValue(SMOOTHING_PRIOR_OPTION));
        this.pa = (double) this.author.papers.length / (double) this.totalOverlapPaperSize;
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
        double logProb = this.pa;
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

        if (hitCount > 0) {
            return Math.exp(logProb);
        }
        return INVALID_VALUE;
    }

    public static void setOptions(Options options) {
        options.addOption(Option.builder(TOTAL_OVERLAP_PAPER_SIZE_OPTION)
                .hasArg(true)
                .required(false)
                .desc("[param, optional] total number of overlap papers in training for " + NAME)
                .build());
        options.addOption(Option.builder(TOTAL_CITATION_ID_SIZE_OPTION)
                .hasArg(true)
                .required(false)
                .desc("[param, optional] total number of unique reference IDs in training for " + NAME)
                .build());
        options.addOption(Option.builder(SMOOTHING_PRIOR_OPTION)
                .hasArg(true)
                .required(false)
                .desc("[param, optional] smoothing prior alpha(0 <= alpha <= 1) for " + NAME
                        + ", Laplace smoothing: alpha = 1, Lidstone smoothing: alpha < 1")
                .build());
    }

    public static boolean checkIfValid(String modelType, CommandLine cl) {
        return modelType.equals(TYPE) && cl.hasOption(TOTAL_OVERLAP_PAPER_SIZE_OPTION)
                && cl.hasOption(TOTAL_CITATION_ID_SIZE_OPTION) && cl.hasOption(SMOOTHING_PRIOR_OPTION);
    }
}
