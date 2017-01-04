package kddcup2016;

import java.util.ArrayList;
import java.util.List;

public class FieldOfStudy {
    public static final String TOP_LEVEL = "L0";
    public final String id, level;
    private List<FieldOfStudy> upperFosList;

    public FieldOfStudy(String id, String level) {
        this.id = id;
        this.level = level;
        this.upperFosList = new ArrayList<>();
    }

    public void addUpperFos(FieldOfStudy upperFos) {
        this.upperFosList.add(upperFos);
    }

    public List<FieldOfStudy> getUpperFosList() {
        return this.upperFosList;
    }

    public boolean isTopLevel() {
        return this.level.equals(TOP_LEVEL);
    }
}
