package model;

import structure.Author;
import structure.Paper;

public class GeometricMealModel extends BaseModel {
    public static final String TYPE = "gm";
    public static final String NAME = "Geometric Mean Based Model";

    public GeometricMealModel(Author author) {
        super(author);
    }

    public GeometricMealModel(String line) {
        super(line);
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
                double prob = (double) this.citeCountMap.get(refPaperId) / (double) this.paperIds.length;
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

    public static boolean checkIfValid(String modelType) {
        if (!modelType.equals(TYPE)) {
            return false;
        }
        return true;
    }
}
