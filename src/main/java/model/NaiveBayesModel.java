package model;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import structure.Author;
import structure.Paper;

public class NaiveBayesModel extends BaseModel {
    public static final String TYPE = "nb";
    public static final String NAME = "Naive Bayes Based Model";
    public static final String TOTAL_OVERLAP_PAPER_SIZE_OPTION = "tops";
    private final int totalOverlapPaperSize;
    private final double pa;
    private int totalCitationCount;

    public NaiveBayesModel(Author author, CommandLine cl) {
        super(author);
        this.totalOverlapPaperSize = Integer.parseInt(cl.getOptionValue(TOTAL_OVERLAP_PAPER_SIZE_OPTION));
        this.pa = (double) this.author.papers.length / (double) this.totalOverlapPaperSize;
        this.totalCitationCount = 0;
    }

    @Override
    public void train() {
        super.train();
        for (String refPaperId : this.citeCountMap.keySet()) {
            this.totalCitationCount += this.citeCountMap.get(refPaperId);
        }
    }

    @Override
    public double estimate(Paper paper) {
        double logProb = this.pa;
        int hitCount = 0;
        for (String refPaperId : paper.refPaperIds) {
            if (this.citeCountMap.containsKey(refPaperId)) {
                double prob = (double) this.citeCountMap.get(refPaperId) / (double) this.totalCitationCount;
                logProb += Math.log(prob);
                hitCount++;
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
    }

    public static boolean checkIfValid(String modelType, CommandLine cl) {
        return modelType.equals(TYPE) && cl.hasOption(TOTAL_OVERLAP_PAPER_SIZE_OPTION);
    }
}
