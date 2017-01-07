package structure;

import common.Config;

public class Result implements Comparable<Result> {
    public final String authorId;
    public final double score;

    public Result(String inputLine) {
        String[] elements = inputLine.split(Config.FIRST_DELIMITER);
        this.authorId = elements[0];
        this.score = Double.parseDouble(elements[1]);
    }

    @Override
    public int compareTo(Result r) {
        // descending order
        if (r.score > this.score) {
            return 1;
        } else if (r.score < this.score) {
            return -1;
        }
        return 0;
    }
}
