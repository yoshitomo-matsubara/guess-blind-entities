package structure;

import java.util.List;

public class Author {
    public final String id;
    public final Paper[] papers;

    public Author(String id, List<String> inputLineList) {
        this.id = id;
        this.papers = new Paper[inputLineList.size()];
        for (int i = 0; i < this.papers.length; i++) {
            this.papers[i] = new Paper(inputLineList.get(i));
        }
    }
}
