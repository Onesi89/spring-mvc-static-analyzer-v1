package com.onesi.smsa.graph;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.onesi.smsa.extract.MethodCallExtractor;
import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.Layer;
import com.onesi.smsa.model.MethodInfo;
import com.onesi.smsa.model.MethodRef;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CallGraphBuilderTest {
    @Test
    void returnsEmptyOutgoingEdgesWhenMethodHasNoEdges() {
        CallGraph graph = new CallGraph(Map.of());

        assertThat(graph.outgoing(new MethodRef("UserService", "missing"))).isEmpty();
    }

    @Test
    void resolvesFirstPassCallTargetsAndMarkers() {
        ClassInfo controller = classInfo("UserController", Layer.CONTROLLER, List.of(
                method("UserController", "createUser", """
                        public void createUser() {
                            validate();
                            this.audit();
                            userService.createUser();
                            userService.missing();
                            logger.info();
                        }
                        """),
                method("UserController", "validate", "private void validate() {}"),
                method("UserController", "audit", "private void audit() {}")));
        ClassInfo service = classInfo("UserService", Layer.SERVICE, List.of(
                method("UserService", "createUser", "public void createUser() {}")));

        CallGraph graph = new CallGraphBuilder(new MethodCallExtractor()).build(
                List.of(controller, service),
                Map.of("UserController", Map.of("userService", "UserService")));

        assertThat(graph.outgoing(new MethodRef("UserController", "createUser")))
                .containsExactly(
                        CallEdge.resolved(new MethodRef("UserController", "validate")),
                        CallEdge.resolved(new MethodRef("UserController", "audit")),
                        CallEdge.resolved(new MethodRef("UserService", "createUser")),
                        CallEdge.marker("unresolved: userService.missing()"),
                        CallEdge.marker("unsupported: logger.info()"));
    }

    private static ClassInfo classInfo(String simpleName, Layer layer, List<MethodInfo> methods) {
        return new ClassInfo(
                "com.example",
                simpleName,
                "com.example." + simpleName,
                Set.of(),
                List.of(),
                List.of(),
                methods,
                layer);
    }

    private static MethodInfo method(String className, String methodName, String source) {
        MethodDeclaration declaration = StaticJavaParser.parseMethodDeclaration(source);
        return new MethodInfo(new MethodRef(className, methodName), declaration.isPublic(), declaration, List.of());
    }
}
