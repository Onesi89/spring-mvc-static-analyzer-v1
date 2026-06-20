package com.onesi.smsa.graph.policy;

import com.onesi.smsa.graph.CallEdge;
import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.Layer;

public class CallResolutionPolicy {
    public boolean shouldSuppress(ClassInfo owner, CallEdge edge) {
        return owner.layer() == Layer.CONTROLLER
                && !edge.resolved()
                && edge.markerText().startsWith("unsupported: ");
    }
}
