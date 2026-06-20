package com.onesi.smsa.graph;

import com.onesi.smsa.extract.ExtractedCall;
import com.onesi.smsa.extract.MethodCallExtractor;
import com.onesi.smsa.graph.policy.CallResolutionPolicy;
import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.MethodInfo;
import com.onesi.smsa.model.MethodRef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CallGraphBuilder {
    private final MethodCallExtractor methodCallExtractor;
    private final CallResolutionPolicy policy;

    public CallGraphBuilder(MethodCallExtractor methodCallExtractor) {
        this(methodCallExtractor, new CallResolutionPolicy());
    }

    public CallGraphBuilder(MethodCallExtractor methodCallExtractor, CallResolutionPolicy policy) {
        this.methodCallExtractor = methodCallExtractor;
        this.policy = policy;
    }

    public CallGraph build(List<ClassInfo> classes, Map<String, Map<String, String>> injections) {
        Map<String, ClassInfo> bySimpleName = classes.stream()
                .collect(Collectors.toMap(ClassInfo::simpleName, classInfo -> classInfo, (left, right) -> left));
        Map<MethodRef, List<CallEdge>> edges = new HashMap<>();

        for (ClassInfo owner : classes) {
            Set<String> ownerMethodNames = owner.methods().stream()
                    .map(method -> method.ref().methodName())
                    .collect(Collectors.toSet());
            for (MethodInfo method : owner.methods()) {
                if (method.declaration() == null) {
                    edges.put(method.ref(), List.of());
                    continue;
                }
                List<CallEdge> outgoing = new ArrayList<>();
                for (ExtractedCall call : methodCallExtractor.extract(method.declaration())) {
                    CallEdge edge = resolveCall(owner, ownerMethodNames, injections, bySimpleName, call);
                    if (policy.shouldSuppress(owner, edge)) {
                        continue;
                    }
                    outgoing.add(edge);
                }
                edges.put(method.ref(), outgoing);
            }
        }
        return new CallGraph(edges);
    }

    private CallEdge resolveCall(
            ClassInfo owner,
            Set<String> ownerMethodNames,
            Map<String, Map<String, String>> injections,
            Map<String, ClassInfo> bySimpleName,
            ExtractedCall call) {
        String scope = call.scope();
        if (scope == null || scope.isBlank() || scope.equals("this")) {
            if (ownerMethodNames.contains(call.methodName())) {
                return CallEdge.resolved(new MethodRef(owner.simpleName(), call.methodName()));
            }
            return CallEdge.marker("unresolved: " + call.displayText());
        }

        String targetClass = injections.getOrDefault(owner.simpleName(), Map.of()).get(scope);
        if (targetClass != null && bySimpleName.containsKey(targetClass)) {
            if (hasMethod(bySimpleName.get(targetClass), call.methodName())) {
                return CallEdge.resolved(new MethodRef(targetClass, call.methodName()));
            }
            return CallEdge.marker("unresolved: " + call.displayText());
        }

        return CallEdge.marker("unsupported: " + call.displayText());
    }

    private boolean hasMethod(ClassInfo classInfo, String methodName) {
        return classInfo.methods().stream()
                .anyMatch(method -> method.ref().methodName().equals(methodName));
    }
}
