package structure;

import common.Config;

import java.util.HashSet;
import java.util.Set;

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

    public Set<String> getAuthorIdSet() {
        return this.authorIdSet;
    }

    public int getAuthorSize() {
        return this.authorIdSet.size();
    }

    public boolean checkIfAuthor(String authorId) {
        return this.authorIdSet.contains(authorId);
    }

    public String toString() {
        StringBuilder authorIdSetSb = new StringBuilder();
        for (String authorId : this.authorIdSet) {
            String str = authorIdSetSb.length() == 0 ? authorId : Config.SECOND_DELIMITER + authorId;
            authorIdSetSb.append(str);
        }

        StringBuilder refPaperIdsSb = new StringBuilder();
        for (String refPaperId : this.refPaperIds) {
            String str = refPaperIdsSb.length() == 0 ? refPaperId : Config.SECOND_DELIMITER + refPaperId;
            refPaperIdsSb.append(str);
        }

        return this.id + Config.FIRST_DELIMITER + this.year + Config.FIRST_DELIMITER + this.venueId
                + Config.FIRST_DELIMITER + authorIdSetSb.toString() + Config.FIRST_DELIMITER + refPaperIdsSb.toString();
    }
}
