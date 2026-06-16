package com.onesi.smsa.model;

import com.github.javaparser.ast.body.MethodDeclaration;
import java.util.List;

public record MethodInfo(
        MethodRef ref,
        boolean publicMethod,
        MethodDeclaration declaration,
        List<String> rawCalls) {
    public MethodInfo {
        rawCalls = List.copyOf(rawCalls);
    }
}
