package structure;

import common.Config;

import java.util.*;

public class Paper {
    public final String id, year, venueId;
    public final String[] refPaperIds;
    private final Set<String> authorIdSet;

    public Paper(String inputLine) {
        this.authorIdSet = new HashSet<>();
        String[] elements = inputLine.split(Config.FIRST_DELIMITER);
        this.id = elements[0];
        this.year = elements[1];
        this.venueId = elements[2];
        String[] authorIds = elements[3].split(Config.SECOND_DELIMITER);
        for (String authorId : authorIds) {
            this.authorIdSet.add(authorId);
        }
        this.refPaperIds = elements[4].split(Config.SECOND_DELIMITER);
    }

    public Set<String> getAuthorSet() {
        return this.authorIdSet;
    }

    public int getAuthorSize() {
        return this.authorIdSet.size();
    }

    public boolean checkIfAuthor(String authorId) {
        return this.authorIdSet.contains(authorId);
    }
}
