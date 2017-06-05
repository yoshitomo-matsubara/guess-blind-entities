package model;

import structure.Author;
import structure.Paper;

public class CommonCitationModel extends BaseModel {
    public static final String TYPE = "cc";
    public static final String NAME = "Common Citation Model";

    public CommonCitationModel(Author author) {
        super(author);
    }

    public CommonCitationModel(String line) {
        super(line);
    }

    @Override
    public void train() {
        super.train();
    }

    @Override
    public double estimate(Paper paper) {
        int[] counts = calcCounts(paper);
        return counts[1] > 0 ? (double) counts[0] : INVALID_VALUE;
    }

    public static boolean checkIfValid(String modelType) {
        return modelType.equals(TYPE);
    }
}
