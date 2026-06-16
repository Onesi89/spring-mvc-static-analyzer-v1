package com.onesi.smsa.extract;

import com.onesi.smsa.model.Layer;
import java.util.Set;

public class LayerClassifier {
    public Layer classify(String simpleName, Set<String> annotations) {
        if (annotations.contains("Controller") || annotations.contains("RestController")) {
            return Layer.CONTROLLER;
        }
        if (annotations.contains("Service")) {
            return Layer.SERVICE;
        }
        if (annotations.contains("Repository")) {
            return Layer.REPOSITORY;
        }
        if (simpleName.endsWith("Repository")) {
            return Layer.REPOSITORY;
        }
        if (simpleName.endsWith("DAO") || simpleName.endsWith("Dao")) {
            return Layer.DAO;
        }
        if (simpleName.endsWith("Mapper")) {
            return Layer.MAPPER;
        }
        return Layer.UNKNOWN;
    }
}
