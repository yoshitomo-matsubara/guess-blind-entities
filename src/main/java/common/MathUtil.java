package common;

import java.util.List;
import java.util.Map;

public class MathUtil {
    public static double calcAverage(List<Integer> valueList) {
        double sum = 0.0d;
        if (valueList.size() == 0) {
            return 0.0d;
        }
        for (int value : valueList) {
            sum += (double) value;
        }
        return sum / (double) valueList.size();
    }

    public static int calcTotalValue(Map<String, Integer> map) {
        int total = 0;
        for (String key : map.keySet()) {
            total += map.get(key);
        }
        return total;
    }
}
