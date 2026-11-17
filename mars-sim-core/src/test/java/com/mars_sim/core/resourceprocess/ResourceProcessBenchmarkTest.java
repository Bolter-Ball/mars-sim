/*
 * Mars Simulation Project
 * ResourceProcessBenchmarkTest.java
 * @date 2026-10-17
 * @author Branislav Blicha
 */
package com.mars_sim.core.resourceprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.mars_sim.core.building.BuildingCategory;
import com.mars_sim.core.building.function.FunctionType;
import com.mars_sim.core.equipment.EquipmentFactory;
import com.mars_sim.core.equipment.EquipmentInventory;
import com.mars_sim.core.equipment.EquipmentType;
import com.mars_sim.core.map.location.LocalPosition;
import com.mars_sim.core.resourceprocess.ResourceProcess.ProcessState;
import com.mars_sim.core.test.MarsSimUnitTest;

/**
 * Times how long the resource processes of a settlement take to run. Only runs with -Dbenchmark=true.
 * The settlement holds a lot of equipment because working out the remaining storage capacity
 * goes through all of it.
 */
@EnabledIfSystemProperty(named = "benchmark", matches = "true")
class ResourceProcessBenchmarkTest extends MarsSimUnitTest {

    private static final int EQUIPMENT = 300;
    private static final double INPUT_STOCK = 5_000D;
    private static final double PULSE_MSOLS = 0.1;
    private static final int WARMUP_ROUNDS = 2_000;
    private static final int ROUNDS = 10_000;
    private static final int RUNS = 7;

    @Test
    void benchmarkProcessResources() {
        var s = buildSettlement("Benchmark", true);
        buildFunction(s.getBuildingManager(), "Lander Hab", BuildingCategory.PROCESSING,
                FunctionType.RESOURCE_PROCESSING, LocalPosition.DEFAULT_POSITION, 0D, true);

        EquipmentType[] types = {EquipmentType.BAG, EquipmentType.BARREL, EquipmentType.GAS_CANISTER};
        for (int i = 0; i < EQUIPMENT; i++) {
            EquipmentFactory.createEquipment(types[i % types.length], s);
        }

        List<ResourceProcess> processes = s.getResourceProcesses();
        var inv = s.getEquipmentInventory();
        assertTrue(processes.size() > 1, "Settlement has resource processes");

        var pulse = createPulse(PULSE_MSOLS);
        resetStock(processes, inv);
        runRounds(processes, pulse, WARMUP_ROUNDS);

        long[] times = new long[RUNS];
        for (int run = 0; run < RUNS; run++) {
            // Same starting point for every run, done outside the timed part
            resetStock(processes, inv);

            long start = System.nanoTime();
            runRounds(processes, pulse, ROUNDS);
            times[run] = System.nanoTime() - start;

            long running = processes.stream().filter(ResourceProcess::isProcessRunning).count();
            assertEquals(processes.size(), running, "All processes kept running, so every run did the same work");
        }

        // Total of the stored outputs after the last run, to check a code change gives the same results
        double outputTotal = processes.stream()
                .flatMap(p -> p.getSpec().getOutputResources().stream())
                .distinct()
                .mapToDouble(inv::getSpecificAmountResourceStored)
                .sum();
        System.out.printf("BENCHMARK output total after last run = %.6f kg%n", outputTotal);

        Arrays.sort(times);
        System.out.printf("BENCHMARK processes=%d equipment=%d rounds=%d runs=%d%n",
                processes.size(), EQUIPMENT, ROUNDS, RUNS);
        System.out.printf("BENCHMARK ms per run: min=%.1f median=%.1f max=%.1f all=%s%n",
                times[0] / 1e6, times[RUNS / 2] / 1e6, times[RUNS - 1] / 1e6,
                Arrays.toString(Arrays.stream(times).map(t -> t / 1_000_000).toArray()));
    }

    /**
     * Empties the outputs, tops up the inputs and turns every process back on.
     */
    private static void resetStock(List<ResourceProcess> processes, EquipmentInventory inv) {
        for (var p : processes) {
            for (Integer r : p.getSpec().getOutputResources()) {
                double stored = inv.getSpecificAmountResourceStored(r);
                if (stored > 0) {
                    inv.retrieveAmountResource(r, stored);
                }
            }
        }
        for (var p : processes) {
            for (Integer r : p.getSpec().getInputResources()) {
                double stored = inv.getSpecificAmountResourceStored(r);
                if (!p.isAmbientInputResource(r) && stored < INPUT_STOCK) {
                    inv.storeAmountResource(r, INPUT_STOCK - stored);
                }
            }
            p.setProcessState(ProcessState.LOCK_ON);
        }
    }

    private static void runRounds(List<ResourceProcess> processes,
                                  com.mars_sim.core.time.ClockPulse pulse, int rounds) {
        for (int i = 0; i < rounds; i++) {
            for (var p : processes) {
                p.processResources(pulse, 1D, 0D);
            }
        }
    }
}
