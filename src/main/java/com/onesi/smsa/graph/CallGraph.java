package com.onesi.smsa.graph;

import com.onesi.smsa.model.MethodRef;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record CallGraph(Map<MethodRef, List<CallEdge>> edges) {
    public CallGraph {
        Map<MethodRef, List<CallEdge>> copiedEdges = new HashMap<>();
        edges.forEach((methodRef, outgoing) -> copiedEdges.put(methodRef, List.copyOf(outgoing)));
        edges = Map.copyOf(copiedEdges);
    }

    public List<CallEdge> outgoing(MethodRef methodRef) {
        return edges.getOrDefault(methodRef, List.of());
    }
}
