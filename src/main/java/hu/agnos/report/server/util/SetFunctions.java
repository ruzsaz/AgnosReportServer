package hu.agnos.report.server.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Collection of simple set functions.
 */
public class SetFunctions {

    public static <E> List<E> limitList(List<E> list, int limit) {
        if (list.size() <= limit) {
            return list;
        } else {
            return list.subList(0, limit);
        }
    }

    public static <C> List<List<C>> cartesianProduct(List<Set<C>> sets) {
        List<List<C>> newSets = new ArrayList<>(sets.size());
        for(Set<C> set : sets) {
            newSets.add(List.copyOf(set));
        }
        return cartesianProductFromList(newSets);
    }

    public static <C> List<List<C>> cartesianProductFromList(List<List<C>> sets) {
        List<List<C>> result = new ArrayList<>(sets.size());
        getCartesianProductHelper(sets, 0, new ArrayList<>(10), result);
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
     * Tests if two lists have the same elements (as sets).
     *
     * @param list1 First list to check
     * @param list2 Second list to check
     * @return True if they have the same elements, false if not
     * @param <E> Type of the elements, must implement equals as required.
     */
    public static <E> boolean isHaveSameElements(List<E> list1, List<E> list2) {
        Set<E> setA = new HashSet<>(list1);
        Set<E> setB = new HashSet<>(list2);
        return Objects.equals(setA, setB);
    }

}
