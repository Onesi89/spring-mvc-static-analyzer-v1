package com.onesi.smsa.tree;

import static org.assertj.core.api.Assertions.assertThat;

import com.onesi.smsa.graph.CallEdge;
import com.onesi.smsa.graph.CallGraph;
import com.onesi.smsa.model.MethodRef;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CallTreeBuilderTest {
    @Test
    void stopsWhenMaxDepthIsExceeded() {
        MethodRef start = new MethodRef("UserService", "a");
        MethodRef next = new MethodRef("UserService", "b");
        CallGraph graph = new CallGraph(Map.of(
                start, List.of(CallEdge.resolved(next)),
                next, List.of()));

        CallTreeNode root = new CallTreeBuilder(0).build(start, graph);

        assertThat(root.children().get(0).text())
                .isEqualTo("unsupported: max call depth exceeded");
    }

    @Test
    void stopsCircularCalls() {
        MethodRef start = new MethodRef("UserService", "a");
        MethodRef next = new MethodRef("UserService", "b");
        CallGraph graph = new CallGraph(Map.of(
                start, List.of(CallEdge.resolved(next)),
                next, List.of(CallEdge.resolved(start))));

        CallTreeNode root = new CallTreeBuilder(20).build(start, graph);

        assertThat(root.children().get(0).children().get(0).text())
                .isEqualTo("circular: UserService.a()");
    }
}
