package hu.agnos.report.server.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SetFunctionsTest {

    @org.junit.jupiter.api.Test
    void get() {
        System.out.println("Test: getCartesianProduct");
        List<Set<String>> sets = new ArrayList<>();
        sets.add(new HashSet<>(Arrays.asList("okos", "erős")));
        sets.add(new HashSet<>(Arrays.asList("Zolika")));
        sets.add(new HashSet<>(Arrays.asList("okos", "szép", "ügyes")));
        List<List<String>> expected = Arrays.asList(Arrays.asList("erős", "Zolika", "ügyes"), Arrays.asList("erős", "Zolika", "okos"), Arrays.asList("erős", "Zolika", "szép"), Arrays.asList("okos", "Zolika", "ügyes"), Arrays.asList("okos", "Zolika", "okos"), Arrays.asList("okos", "Zolika", "szép"));
        List<List<String>> result = SetFunctions.cartesianProduct(sets);
        assertEquals(6, result.size());
        assertEquals(new HashSet<>(expected), new HashSet<>(result));
    }

    @org.junit.jupiter.api.Test
    void hasSameElementsTest() {
        System.out.println("Test: hasSameElements");
        List<String> a = new ArrayList<>(Arrays.asList("zolika", "okos", "nagyon"));
        List<String> b = new ArrayList<>(Arrays.asList("nagyon", "okos", "zolika"));
        List<String> c = new ArrayList<>(Arrays.asList("nagyon", "okos", "a", "zolika"));
        List<String> d = new ArrayList<>(Arrays.asList("nagyon", "okos", "nagyon", "okos", "zolika"));
        List<String> e = new ArrayList<>(Arrays.asList());
        List<String> f = new ArrayList<>(Arrays.asList());
        assertTrue(SetFunctions.isHaveSameElements(a, a));
        assertTrue(SetFunctions.isHaveSameElements(a, b));
        assertTrue(SetFunctions.isHaveSameElements(a, d));
        assertFalse(SetFunctions.isHaveSameElements(a, c));
        assertFalse(SetFunctions.isHaveSameElements(b, c));
        assertTrue(SetFunctions.isHaveSameElements(e, f));
    }

}