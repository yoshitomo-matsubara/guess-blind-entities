package model;

import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import structure.Author;
import structure.Paper;

import java.util.Random;

public class RandomModel extends BaseModel {
    public static final String TYPE = "rand";
    public static final String NAME = "Random Model";
    private static final String GUESSABLE_ONLY_OPTION = "go";
    private static final String PRUNING_RATE_OPTION = "prate";
    private static final double DEFAULT_PRUNING_RATE = 0.95d;
    private final Random rand;
    private final boolean guessableOnly;
    private final double pruningRate;

    public RandomModel(Author author, CommandLine cl) {
        super(author);
        this.rand = new Random();
        this.guessableOnly = cl.hasOption(GUESSABLE_ONLY_OPTION);
        this.pruningRate = cl.hasOption(PRUNING_RATE_OPTION) ?
                Double.parseDouble(cl.getOptionValue(PRUNING_RATE_OPTION)) : DEFAULT_PRUNING_RATE;
    }

    public RandomModel(String line, CommandLine cl) {
        super(line);
        this.rand = new Random();
        this.guessableOnly = cl.hasOption(GUESSABLE_ONLY_OPTION);
        this.pruningRate = cl.hasOption(PRUNING_RATE_OPTION) ?
                Double.parseDouble(cl.getOptionValue(PRUNING_RATE_OPTION)) : DEFAULT_PRUNING_RATE;
    }

    @Override
    public void train() {}

    @Override
    public double estimate(Paper paper) {
        if (this.guessableOnly) {
            int[] counts = calcCounts(paper);
            if (counts[1] == 0) {
                return INVALID_VALUE;
            }
        }

        double value = this.rand.nextDouble();
        if (value >= this.pruningRate) {
            return value;
        }
        return INVALID_VALUE;
    }

    public static void setOptions(Options options) {
        MiscUtil.setOption(GUESSABLE_ONLY_OPTION, false, false,
                "[param, optional] scoring guessable papers only)", options);
        MiscUtil.setOption(PRUNING_RATE_OPTION, true, false,
                "[param, optional] pruning rate for reducing the cost of evaluation" +
                        " ( (1 - this rate) should be greater than the rate of top authors you will use in evaluation," +
                        " default rate = " + String.valueOf(DEFAULT_PRUNING_RATE) + " )", options);
    }

    public static boolean checkIfValid(String modelType) {
        return modelType.equals(TYPE);
    }
}
