package kddcup2016;

import common.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class BagOfFields {
    public final String venueId;
    private int totalCount;
    private HashMap<String, Integer> countMap;

    public BagOfFields(String venueId) {
        this.venueId = venueId;
        this.totalCount = 0;
        this.countMap = new HashMap<>();
    }

    public void countUp(String fieldId, int count) {
        if (!this.countMap.containsKey(fieldId)) {
            this.countMap.put(fieldId, count);
        } else {
            this.countMap.put(fieldId, this.countMap.get(fieldId) + count);
        }
        this.totalCount += count;
    }

    public void countUp(String fieldId) {
        countUp(fieldId, 1);
    }

    public int getTotalCount() {
        return this.totalCount;
    }

    private String formatField(String fieldId, String countStr) {
        return fieldId + Config.KEY_VALUE_DELIMITER + countStr;
    }

    @Override
    public String toString() {
        TreeMap<Integer, List<String>> treeMap = new TreeMap<>();
        for (String fieldId : this.countMap.keySet()) {
            int count = this.countMap.get(fieldId);
            if (!treeMap.containsKey(count)) {
                treeMap.put(count, new ArrayList<>());
            }
            treeMap.get(count).add(fieldId);
        }

        StringBuilder sb = new StringBuilder();
        for (int count : treeMap.descendingKeySet()) {
            String countStr = String.valueOf(count);
            List<String> fieldIdList = treeMap.get(count);
            String str = sb.length() == 0 ? formatField(fieldIdList.get(0), countStr)
                    : Config.SECOND_DELIMITER + formatField(fieldIdList.get(0), countStr);
            sb.append(str);
            int size = fieldIdList.size();
            for (int i = 1; i < size; i++) {
                sb.append(Config.SECOND_DELIMITER + formatField(fieldIdList.get(i), countStr));
            }
        }
        return this.venueId + Config.FIRST_DELIMITER + sb.toString();
    }
}
