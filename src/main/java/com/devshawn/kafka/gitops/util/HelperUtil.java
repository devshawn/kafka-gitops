package com.devshawn.kafka.gitops.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class HelperUtil {

    public static List<String> uniqueCombine(List<String> listOne, List<String> listTwo) {
        Set<String> set = new LinkedHashSet<>(listOne);
        set.addAll(listTwo);
        return new ArrayList<>(set);
    }
}
