package com.onesi.smsa.model;

import java.util.List;
import java.util.Set;

public record ClassInfo(
        String packageName,
        String simpleName,
        String qualifiedName,
        Set<String> annotations,
        List<FieldInfo> fields,
        List<ConstructorInfo> constructors,
        List<MethodInfo> methods,
        Layer layer) {
    public ClassInfo {
        annotations = Set.copyOf(annotations);
        fields = List.copyOf(fields);
        constructors = List.copyOf(constructors);
        methods = List.copyOf(methods);
    }
}
