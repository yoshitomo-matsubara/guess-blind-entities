package model;

import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Author;
import structure.Paper;

public class NaiveBayesModel extends BaseModel {
    public static final String TYPE = "nb";
    public static final String NAME = "Naive Bayes Based Model";
    private static final String TOTAL_OVERLAP_PAPER_ID_SIZE_OPTION = "topis";
    private final int totalOverlapPaperSize;
    private final double logPa;

    public NaiveBayesModel(Author author, CommandLine cl) {
        super(author);
        this.totalOverlapPaperSize = Integer.parseInt(cl.getOptionValue(TOTAL_OVERLAP_PAPER_ID_SIZE_OPTION));
        this.logPa = Math.log((double) this.paperIds.length / (double) this.totalOverlapPaperSize);
    }

    public NaiveBayesModel(String line, CommandLine cl) {
        super(line);
        this.totalOverlapPaperSize = Integer.parseInt(cl.getOptionValue(TOTAL_OVERLAP_PAPER_ID_SIZE_OPTION));
        this.logPa = Math.log((double) this.paperIds.length / (double) this.totalOverlapPaperSize);
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
        double logProb = this.logPa;
        int hitCount = 0;
        for (String refPaperId : paper.refPaperIds) {
            if (this.citeCountMap.containsKey(refPaperId)) {
                double prob = (double) this.citeCountMap.get(refPaperId) / (double) this.totalCitationCount;
                logProb += Math.log(prob);
                hitCount++;
            }
        }
        return hitCount > 0 ? Math.exp(logProb) : INVALID_VALUE;
    }

    public static void setOptions(Options options) {
        MiscUtil.setOption(TOTAL_OVERLAP_PAPER_ID_SIZE_OPTION, true, false,
                "[param, optional] total number of overlap papers in training for " + NAME, options);
    }

    public static boolean checkIfValid(String modelType, CommandLine cl) {
        return modelType.equals(TYPE) && cl.hasOption(TOTAL_OVERLAP_PAPER_ID_SIZE_OPTION);
    }
}
