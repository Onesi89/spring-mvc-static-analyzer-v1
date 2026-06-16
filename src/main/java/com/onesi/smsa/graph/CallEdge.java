package com.onesi.smsa.graph;

import com.onesi.smsa.model.MethodRef;

public record CallEdge(MethodRef target, String markerText) {
    public static CallEdge resolved(MethodRef target) {
        return new CallEdge(target, null);
    }

    public static CallEdge marker(String markerText) {
        return new CallEdge(null, markerText);
    }

    public boolean resolved() {
        return target != null;
    }
}
