package structure;

import common.Config;

import java.util.HashSet;

public class Paper {
    public final String id, year, venueId;
    public final String[] refPaperIds;
    private final HashSet<String> authorSet;

    public Paper(String inputLine) {
        String[] elements = inputLine.split(Config.FIRST_DELIMITER);
        this.id = elements[0];
        this.year = elements[1];
        this.venueId = elements[2];
        this.authorSet = new HashSet<>();
        this.refPaperIds = elements[4].split(Config.SECOND_DELIMITER);
        String[] authorIds = elements[3].split(Config.SECOND_DELIMITER);
        for (String authorId : authorIds) {
            this.authorSet.add(authorId);
        }
    }

    public HashSet<String> getAuthorSet() {
        return this.authorSet;
    }

    public int getAuthorSize() {
        return this.authorSet.size();
    }

    public boolean checkIfAuthor(String authorId) {
        return this.authorSet.contains(authorId);
    }
}
