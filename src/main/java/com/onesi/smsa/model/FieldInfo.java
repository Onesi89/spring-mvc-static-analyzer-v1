package com.onesi.smsa.model;

import java.util.Set;

public record FieldInfo(String name, String typeName, Set<String> annotations) {
    public FieldInfo {
        annotations = Set.copyOf(annotations);
    }
}
