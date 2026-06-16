package com.onesi.smsa.extract;

import static org.assertj.core.api.Assertions.assertThat;

import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.Layer;
import com.onesi.smsa.model.MethodInfo;
import com.onesi.smsa.model.MethodRef;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ControllerEntryPointFinderTest {
    @Test
    void findsOnlyPublicControllerMethods() {
        MethodInfo publicMethod = new MethodInfo(new MethodRef("UserController", "createUser"), true, null, List.of());
        MethodInfo privateMethod = new MethodInfo(new MethodRef("UserController", "helper"), false, null, List.of());
        ClassInfo controller = new ClassInfo("", "UserController", "UserController", Set.of("Controller"),
                List.of(), List.of(), List.of(publicMethod, privateMethod), Layer.CONTROLLER);
        ClassInfo service = new ClassInfo("", "UserService", "UserService", Set.of("Service"),
                List.of(), List.of(), List.of(publicMethod), Layer.SERVICE);

        List<MethodRef> entryPoints = new ControllerEntryPointFinder().find(List.of(controller, service));

        assertThat(entryPoints).containsExactly(new MethodRef("UserController", "createUser"));
    }
}
