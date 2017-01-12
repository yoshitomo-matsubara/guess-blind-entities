package model;

import structure.Author;
import structure.Paper;

public class NaiveBayesModel extends BaseModel {
    public static final String TYPE = "nb";
    public static final String NAME = "Naive Bayes Based Model";

    public NaiveBayesModel(Author author) {
        super(author);
    }

    @Override
    public void train() {
        super.train();
    }

    @Override
    public double estimate(Paper paper) {
        double logScore = 0.0d;
        int hitCount = 0;
        for (String refPaperId : paper.refPaperIds) {
            if (this.citeCountMap.containsKey(refPaperId)) {
                double prob = (double) this.citeCountMap.get(refPaperId) / (double) this.author.papers.length;
                logScore += Math.log(prob);
                hitCount++;
            }
        }

        if (hitCount > 0) {
            logScore /= (double) hitCount;
            double prob = (double) hitCount / (double) paper.refPaperIds.length;
            logScore += Math.log(prob);
            return Math.exp(logScore);
        }
        return INVALID_VALUE;
    }
}
