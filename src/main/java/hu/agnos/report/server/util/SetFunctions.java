package hu.agnos.report.server.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SetFunctions {

    public static <E> List<E> limitList(List<E> list, int limit) {
        if (list.size() <= limit) {
            return list;
        } else {
            return list.subList(0, limit);
        }
    }

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

    /**
     * Tests if two arrays have the same elemets (as sets).
     *
     * @param a First array to check
     * @param b Second array to check
     * @return True if they have the same elements, false if not
     * @param <E> Type of the elements, must implement equals as required.
     */
    public static <E> boolean haveSameElements(List<E> a, List<E> b) {
        Set<E> setA = new HashSet<>(a);
        Set<E> setB = new HashSet<>(b);
        return Objects.equals(setA, setB);
    }

}
