package structure;

import common.Config;

public class Paper {
    public final String id, year, publisherId;
    public final String[] refPaperIds;

    public Paper(String inputLine) {
        String[] elements = inputLine.split(Config.FIRST_DELIMITER);
        this.id = elements[0];
        this.year = elements[1];
        this.publisherId = elements[2];
        this.refPaperIds = elements[4].split(Config.SECOND_DELIMITER);
    }
}
