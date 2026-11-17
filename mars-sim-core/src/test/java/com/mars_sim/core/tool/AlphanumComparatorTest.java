/*
 * Mars Simulation Project
 * AlphanumComparatorTest.java
 * @date 2026-10-17
 * @author Branislav Blicha
 */
package com.mars_sim.core.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class AlphanumComparatorTest {

    private final AlphanumComparator comparator = new AlphanumComparator();

    @Test
    void testNumbersAreSortedNumerically() {
        List<String> names = new ArrayList<>(List.of("Rover 10", "Rover 2", "Rover 1"));

        names.sort(comparator);

        assertEquals(List.of("Rover 1", "Rover 2", "Rover 10"), names);
        // Plain String order would put "Rover 10" before "Rover 2"
        assertTrue("Rover 10".compareTo("Rover 2") < 0, "Plain string order is different");
    }

    @Test
    void testSameLengthNumbersAreComparedDigitByDigit() {
        assertTrue(comparator.compare("Lab 12", "Lab 13") < 0, "12 before 13");
        assertTrue(comparator.compare("Lab 21", "Lab 13") > 0, "21 after 13");
        assertEquals(0, comparator.compare("Lab 7", "Lab 7"), "Same name");
    }

    @Test
    void testTextAndLengthDecideWhenNumbersMatch() {
        assertTrue(comparator.compare("Alpha 1", "Beta 1") < 0, "Text part compared as text");
        assertTrue(comparator.compare("Base 1", "Base 1b") < 0, "Shorter name first when the start is equal");
    }

    @Test
    void testNullIsTreatedAsEqual() {
        assertEquals(0, comparator.compare(null, "Rover 1"));
        assertEquals(0, comparator.compare("Rover 1", null));
    }
}
