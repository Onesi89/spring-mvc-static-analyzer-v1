package com.onesi.smsa.tree;

import com.onesi.smsa.graph.CallEdge;
import com.onesi.smsa.graph.CallGraph;
import com.onesi.smsa.model.MethodRef;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CallTreeBuilder {
    private final int maxDepth;

    public CallTreeBuilder(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public CallTreeNode build(MethodRef root, CallGraph graph) {
        return build(root, graph, new LinkedHashSet<>(), 0);
    }

    private CallTreeNode build(MethodRef current, CallGraph graph, Set<MethodRef> path, int depth) {
        if (depth > maxDepth) {
            return CallTreeNode.leaf("unsupported: max call depth exceeded");
        }
        if (path.contains(current)) {
            return CallTreeNode.leaf("circular: " + current.displayName());
        }

        Set<MethodRef> nextPath = new LinkedHashSet<>(path);
        nextPath.add(current);
        List<CallTreeNode> children = new ArrayList<>();
        for (CallEdge edge : graph.outgoing(current)) {
            if (edge.resolved()) {
                children.add(build(edge.target(), graph, nextPath, depth + 1));
            } else {
                children.add(CallTreeNode.leaf(edge.markerText()));
            }
        }
        return new CallTreeNode(current.displayName(), children);
    }
}
