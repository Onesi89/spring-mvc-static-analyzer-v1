package com.onesi.smsa.extract;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import java.util.List;

public class MethodCallExtractor {
    public List<ExtractedCall> extract(MethodDeclaration declaration) {
        return declaration.findAll(MethodCallExpr.class).stream()
                .map(call -> new ExtractedCall(
                        call.getScope().map(Object::toString).orElse(null),
                        call.getNameAsString()))
                .toList();
    }
}
