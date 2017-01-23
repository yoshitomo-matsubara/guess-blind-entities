package model;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Author;
import structure.Paper;

import java.util.Random;

public class RandomModel extends BaseModel {
    public static final String TYPE = "rand";
    public static final String NAME = "Random Model";
    private final Random rand;

    public RandomModel(Author author) {
        super(author);
        this.rand = new Random();
    }

    @Override
    public void train() {}

    @Override
    public double estimate(Paper paper) {
        return this.rand.nextDouble();
    }
}
