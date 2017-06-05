package model;

import structure.Author;
import structure.Paper;

public class SelfCitationCountUpModel extends BaseModel {
    public static final String TYPE = "scu";
    public static final String NAME = "Self-Citation Count Up Model";

    public SelfCitationCountUpModel(Author author) {
        super(author);
    }

    public SelfCitationCountUpModel(String line) {
        super(line);
    }

    @Override
    public void train() {
        super.train();
    }

    @Override
    public double estimate(Paper paper) {
        int selfCiteCount = 0;
        for (String refPaperId : paper.refPaperIds) {
            if (checkIfMyPaper(refPaperId)) {
                selfCiteCount++;
            }
        }
        int[] counts = calcCounts(paper);
        return counts[1] > 0 ? (double) selfCiteCount : INVALID_VALUE;
    }

    public static boolean checkIfValid(String modelType) {
        return modelType.equals(TYPE);
    }
}
