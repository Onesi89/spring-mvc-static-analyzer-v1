package com.onesi.smsa.graph.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.onesi.smsa.graph.CallEdge;
import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.Layer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CallResolutionPolicyTest {
    private final CallResolutionPolicy policy = new CallResolutionPolicy();

    @Test
    void suppressesUnsupportedControllerCalls() {
        assertThat(policy.shouldSuppress(
                        classInfo("UserController", Layer.CONTROLLER),
                        CallEdge.marker("unsupported: Thread.sleep(2000)")))
                .isTrue();
    }

    @Test
    void keepsUnsupportedServiceCalls() {
        assertThat(policy.shouldSuppress(
                        classInfo("UserService", Layer.SERVICE),
                        CallEdge.marker("unsupported: externalClient.send()")))
                .isFalse();
    }

    private static ClassInfo classInfo(String simpleName, Layer layer) {
        return new ClassInfo(
                "com.example",
                simpleName,
                "com.example." + simpleName,
                Set.of(),
                List.of(),
                List.of(),
                List.of(),
                layer);
    }
}
