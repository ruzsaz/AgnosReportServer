package hu.agnos.report.server.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SetFunctions {

    public static <C> List<List<C>> cartesianProduct(List<Set<C>> sets) {
        List<List<C>> newSets = new ArrayList<>();
        for(Set<C> set : sets) {
            newSets.add(List.copyOf(set));
        }
        return cartesianProductFromList(newSets);
    }

    public static <C> List<List<C>> cartesianProductFromList(List<List<C>> sets) {
        List<List<C>> result = new ArrayList<>();
        getCartesianProductHelper(sets, 0, new ArrayList<>(), result);
        return result;
    }

    private static <C> void getCartesianProductHelper(List<List<C>> sets, int index, List<C> current, List<List<C>> result) {
        if (index == sets.size()) {
            result.add(new ArrayList<>(current));
            return;
        }
        List<C> currentSet = sets.get(index);
        for (C element: currentSet) {
            current.add(element);
            getCartesianProductHelper(sets, index+1, current, result);
            current.remove(current.size() - 1);
        }
    }

}
