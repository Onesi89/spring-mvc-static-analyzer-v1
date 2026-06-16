package com.onesi.smsa.graph;

import com.onesi.smsa.model.MethodRef;
import java.util.List;
import java.util.Map;

public record CallGraph(Map<MethodRef, List<CallEdge>> edges) {
    public CallGraph {
        edges = Map.copyOf(edges);
    }

    public List<CallEdge> outgoing(MethodRef methodRef) {
        return edges.getOrDefault(methodRef, List.of());
    }
}
