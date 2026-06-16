package com.onesi.smsa.model;

public record MethodRef(String className, String methodName) {
    public String displayName() {
        return className + "." + methodName + "()";
    }
}
