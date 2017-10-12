package structure;

import common.Config;

import java.util.HashSet;
import java.util.Set;

public class Paper {
    public final String id, year, venueId;
    public final String[] refPaperIds;
    private final Set<String> authorIdSet;

    public Paper(String inputLine) {
        String[] elements = inputLine.split(Config.FIRST_DELIMITER);
        this.id = elements[0];
        this.year = elements[1];
        this.venueId = elements[2];
        this.authorIdSet = new HashSet<>();
        this.refPaperIds = elements[4].split(Config.SECOND_DELIMITER);
        String[] authorIds = elements[3].split(Config.SECOND_DELIMITER);
        for (String authorId : authorIds) {
            this.authorIdSet.add(authorId);
        }
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
