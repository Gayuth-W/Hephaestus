package com.gayuth.hephaestus.rca;

import com.gayuth.hephaestus.TestTraces;
import com.gayuth.hephaestus.dto.RcaResultDTO;
import com.gayuth.hephaestus.enums.Confidence;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FailureRcaEngineTest {

    private final FailureRcaEngine engine = new FailureRcaEngine();

    @Test
    void cascadeBlamesDeepestDependency() {
        RcaResultDTO r = engine.analyze(TestTraces.graph("failure-cascade.json"));
        assertEquals(List.of("database"), r.rootCauses());
        assertEquals(Confidence.HIGH, r.confidence());
        assertEquals(Set.of("api-gateway", "order-service", "payment-service"),
                Set.copyOf(r.affectedServices()));
    }

    @Test
    void internalFailureBlamesTheServiceItself() {
        RcaResultDTO r = engine.analyze(TestTraces.graph("failure-internal.json"));
        assertEquals(List.of("payment-service"), r.rootCauses());
        assertTrue(r.affectedServices().isEmpty());
    }

    @Test
    void independentFailuresAreAllReported() {
        RcaResultDTO r = engine.analyze(TestTraces.graph("failure-fanout.json"));
        assertEquals(Set.of("payment-service", "inventory-service"), Set.copyOf(r.rootCauses()));
        assertEquals(Confidence.MEDIUM, r.confidence());
        assertEquals(Set.of("api-gateway", "order-service"), Set.copyOf(r.affectedServices()));
    }

    @Test
    void healthyTraceHasNoFailure() {
        RcaResultDTO r = engine.analyze(TestTraces.graph("linear.json"));
        assertFalse(r.hasFailure());
        assertEquals(Confidence.HIGH, r.confidence());
    }
}
