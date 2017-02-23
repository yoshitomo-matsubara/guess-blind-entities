package model;

import structure.Author;
import structure.Paper;

public class CountUpModel extends BaseModel {
    public static final String TYPE = "cu";
    public static final String NAME = "Count Up Model";

    public CountUpModel(Author author) {
        super(author);
    }

    public CountUpModel(String line) {
        super(line);
    }

    @Override
    public void train() {
        super.train();
    }

    @Override
    public double estimate(Paper paper) {
        int score = 0;
        int hitCount = 0;
        for (String refPaperId : paper.refPaperIds) {
            if (this.citeCountMap.containsKey(refPaperId)) {
                score += this.citeCountMap.get(refPaperId);
                hitCount++;
            }
        }
        return hitCount > 0 ? (double) score / (double) this.totalCitationCount : INVALID_VALUE;
    }

    public static boolean checkIfValid(String modelType) {
        if (!modelType.equals(TYPE)) {
            return false;
        }
        return true;
    }
}
