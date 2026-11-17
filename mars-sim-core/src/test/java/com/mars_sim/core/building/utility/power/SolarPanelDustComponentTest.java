/*
 * Mars Simulation Project
 * SolarPanelDustComponentTest.java
 * @date 2026-10-17
 * @author Branislav Blicha
 */
package com.mars_sim.core.building.utility.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SolarPanelDustComponentTest {

    private static final double DELTA = 1e-9;

    @Test
    void testDustSettlesOnlyInDaylight() {
        var panel = new SolarPanelDustComponent(20);

        // Night, no sunlight so no dust settles
        panel.update(1.2, 0, 0, 60);
        assertEquals(0, panel.getDust(), DELTA);
        assertEquals(1.0, panel.efficiency(), DELTA);

        // Day, tau 1.2 gives 0.003 + 0.004 * (1.2 - 0.2) = 0.007 per minute
        panel.update(1.2, 0, 500, 60);
        assertEquals(0.42, panel.getDust(), DELTA);
        assertEquals(Math.pow(0.58, 1.6), panel.efficiency(), DELTA);
    }

    @Test
    void testStrongWindCleansButWeakWindDoesNot() {
        var panel = new SolarPanelDustComponent(20);
        panel.setDust(0.5);

        // Below the 15 m/s gust threshold nothing is blown away
        panel.update(0.2, 10, 0, 100);
        assertEquals(0.5, panel.getDust(), DELTA);

        // 35 m/s is well above the threshold so the max self clean of 0.003 per minute applies
        panel.update(0.2, 35, 0, 100);
        assertEquals(0.2, panel.getDust(), DELTA);
    }

    @Test
    void testManualCleaningAndLimits() {
        var panel = new SolarPanelDustComponent(0.2);
        assertEquals(1.0, panel.getPanelAreaM2(), DELTA, "Panel area is at least 1 m2");

        panel.setDust(0.3);
        panel.manualClean(10, 1.0); // 0.015 per minute for 10 minutes
        assertEquals(0.15, panel.getDust(), DELTA);

        panel.manualClean(100, 1.0);
        assertEquals(0, panel.getDust(), DELTA, "Dust never goes below clean");

        panel.setDust(2.0);
        assertEquals(1.0, panel.getDust(), DELTA, "Dust never goes above fully covered");
        assertEquals(0, panel.efficiency(), DELTA);

        // No time passed so nothing changes
        panel.update(3.0, 30, 600, 0);
        assertEquals(1.0, panel.getDust(), DELTA);
    }
}
