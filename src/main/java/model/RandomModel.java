package model;

import common.MiscUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import structure.Author;
import structure.Paper;

import java.util.Random;

public class RandomModel extends BaseModel {
    public static final String TYPE = "rand";
    public static final String NAME = "Random Model";
    public static final String PRUNING_RATE_OPTION = "prate";
    public static final double DEFAULT_PRUNING_RATE = 0.95d;
    private final Random rand;
    private final double pruningRate;

    public RandomModel(Author author, CommandLine cl) {
        super(author);
        this.rand = new Random();
        this.pruningRate = cl.hasOption(PRUNING_RATE_OPTION) ? Double.parseDouble(PRUNING_RATE_OPTION)
                : DEFAULT_PRUNING_RATE;
    }

    @Override
    public void train() {}

    @Override
    public double estimate(Paper paper) {
        double value = this.rand.nextDouble();
        if (value > this.pruningRate) {
            return value;
        }
        return INVALID_VALUE;
    }

    public static void setOptions(Options options) {
        MiscUtil.setOption(PRUNING_RATE_OPTION, true, false,
                "[param, optional] pruning rate for reducing the cost of evaluation" +
                        " ( (1 - this rate) should be greater than the rate of top authors you will use in evaluation," +
                        " default rate = " + String.valueOf(DEFAULT_PRUNING_RATE), options);
    }

    public static boolean checkIfValid(String modelType) {
        if (!modelType.equals(TYPE)) {
            return false;
        }
        return true;
    }
}
