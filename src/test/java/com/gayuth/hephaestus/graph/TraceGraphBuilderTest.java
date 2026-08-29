package com.gayuth.hephaestus.graph;

import com.gayuth.hephaestus.TestTraces;
import com.gayuth.hephaestus.dto.SpanDTO;
import com.gayuth.hephaestus.model.SpanStatus;
import com.gayuth.hephaestus.model.TraceGraph;
import org.junit.jupiter.api.Test;
import com.gayuth.hephaestus.exception.InvalidTraceException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceGraphBuilderTest {

    private final TraceGraphBuilder builder = new TraceGraphBuilder();

    @Test
    void buildsLinearChain() {
        TraceGraph g = TestTraces.graph("linear.json");
        assertEquals(4, g.size());
        assertEquals("api-gateway", g.root().serviceName());
        assertEquals(1, g.root().children().size());
        assertEquals("order-service", g.root().children().get(0).serviceName());
    }

    @Test
    void buildsFanOutWithChildrenSortedByStartTime() {
        TraceGraph g = TestTraces.graph("fanout.json");
        assertEquals(2, g.root().children().size());
        assertEquals("order-service", g.root().children().get(0).serviceName()); // start 50
        assertEquals("inventory-service", g.root().children().get(1).serviceName()); // start 60
    }

    @Test
    void rejectsUnknownParent() {
        InvalidTraceException ex = assertThrows(InvalidTraceException.class,
                () -> TestTraces.graph("invalid-dangling-parent.json"));
        assertTrue(ex.getMessage().contains("unknown parent"));
    }

    @Test
    void rejectsMultipleRoots() {
        List<SpanDTO> spans = List.of(
                new SpanDTO("a", null, "svc-a", 0, 10, SpanStatus.OK, null),
                new SpanDTO("b", null, "svc-b", 0, 10, SpanStatus.OK, null));
        assertThrows(InvalidTraceException.class, () -> builder.build("x", spans));
    }

    @Test
    void rejectsDuplicateSpanId() {
        List<SpanDTO> spans = List.of(
                new SpanDTO("a", null, "svc-a", 0, 10, SpanStatus.OK, null),
                new SpanDTO("a", "a", "svc-b", 0, 10, SpanStatus.OK, null));
        assertThrows(InvalidTraceException.class, () -> builder.build("x", spans));
    }
}
