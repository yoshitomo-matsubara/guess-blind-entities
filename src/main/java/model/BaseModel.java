package model;

import structure.Author;
import structure.Paper;

import java.util.HashMap;

public abstract class BaseModel {
    public static final String TYPE = "ab";
    public static final String NAME = "Abstract Model";

    public static final double INVALID_VALUE = -Double.MAX_VALUE;
    public final String authorId;
    protected Author author;
    protected HashMap<String, Integer> citeCountMap;

    public BaseModel(Author author) {
        this.authorId = author.id;
        this.author = author;
        this.citeCountMap = new HashMap<>();
    }

    public void train() {
        for (Paper paper : this.author.papers) {
            for (String refPaperId : paper.refPaperIds) {
                if (!this.citeCountMap.containsKey(refPaperId)) {
                    this.citeCountMap.put(refPaperId, 1);
                } else {
                    this.citeCountMap.put(refPaperId, this.citeCountMap.get(refPaperId) + 1);
                }
            }
        }
    }

    public abstract double estimate(Paper paper);

    public static boolean checkIfValid(String modelType) {
        if (!modelType.equals(TYPE)) {
            return false;
        }
        return true;
    }
}
