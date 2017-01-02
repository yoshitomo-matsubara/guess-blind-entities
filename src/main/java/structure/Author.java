package structure;

import java.util.List;

public class Author {
    public final String id, name;
    public final Paper[] papers;
    public Author(String id, String name, List<String> inputLineList) {
        this.id = id;
        this.name = name;
        this.papers = new Paper[inputLineList.size()];
        for (int i = 0; i < this.papers.length; i++) {
            this.papers[i] = new Paper(inputLineList.get(i));
        }
    }
}
