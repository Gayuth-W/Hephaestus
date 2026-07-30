package com.gayuth.hephaestus.latency;

import com.gayuth.hephaestus.TestTraces;
import com.gayuth.hephaestus.dto.LatencyResultDTO;
import com.gayuth.hephaestus.enums.Confidence;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatencyAnalysisEngineTest {

    private final LatencyAnalysisEngine engine = new LatencyAnalysisEngine();

    @Test
    void dominantSinkIsIdentifiedWithHighConfidence() {
        LatencyResultDTO r = engine.analyze(TestTraces.graph("parallel-latency.json"));
        assertEquals("payment-service", r.latencySink());
        assertEquals(Confidence.HIGH, r.confidence());
    }

    @Test
    void overlappingChildrenAreUnionedNotSummed() {
        // payment window 6200ms; children fraud[300,1500] + card[800,2000] overlap.
        // union = [300,2000] = 1700ms, so exclusive = 6200 - 1700 = 4500ms.
        LatencyResultDTO r = engine.analyze(TestTraces.graph("parallel-latency.json"));
        long paymentSelf = r.breakdown().stream()
                .filter(s -> s.service().equals("payment-service"))
                .findFirst().orElseThrow().exclusiveTime();
        assertEquals(4500L, paymentSelf);

        // gateway 8000ms with children order[100,800] + payment[200,6400] -> union
        // [100,6400]=6300
        long gatewaySelf = r.breakdown().stream()
                .filter(s -> s.service().equals("api-gateway"))
                .findFirst().orElseThrow().exclusiveTime();
        assertEquals(1700L, gatewaySelf);
    }

    @Test
    void noExclusiveTimeIsNegative() {
        LatencyResultDTO r = engine.analyze(TestTraces.graph("parallel-latency.json"));
        assertTrue(r.breakdown().stream().allMatch(s -> s.exclusiveTime() >= 0));
    }

    @Test
    void unionLengthCountsOverlapsOnce() {
        assertEquals(100L, LatencyAnalysisEngine.unionLength(
                new ArrayList<>(List.of(new long[] { 0, 100 }, new long[] { 20, 40 })))); // nested
        assertEquals(90L, LatencyAnalysisEngine.unionLength(
                new ArrayList<>(List.of(new long[] { 0, 50 }, new long[] { 50, 90 })))); // touching
        assertEquals(50L, LatencyAnalysisEngine.unionLength(
                new ArrayList<>(List.of(new long[] { 0, 30 }, new long[] { 60, 80 })))); // disjoint
    }

    @Test
    void ambiguousTraceReportsLowConfidence() {
        LatencyResultDTO r = engine.analyze(TestTraces.graph("ambiguous-latency.json"));
        assertEquals(Confidence.LOW, r.confidence());
    }
}
