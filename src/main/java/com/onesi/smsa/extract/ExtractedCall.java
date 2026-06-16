package com.onesi.smsa.extract;

public record ExtractedCall(String scope, String methodName) {
    public String displayText() {
        if (scope == null || scope.isBlank()) {
            return methodName + "()";
        }
        return scope + "." + methodName + "()";
    }
}
