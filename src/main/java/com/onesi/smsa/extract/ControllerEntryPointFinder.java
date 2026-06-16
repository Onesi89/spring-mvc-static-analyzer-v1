package com.onesi.smsa.extract;

import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.Layer;
import com.onesi.smsa.model.MethodRef;
import java.util.List;

public class ControllerEntryPointFinder {
    public List<MethodRef> find(List<ClassInfo> classes) {
        return classes.stream()
                .filter(classInfo -> classInfo.layer() == Layer.CONTROLLER)
                .flatMap(classInfo -> classInfo.methods().stream())
                .filter(method -> method.publicMethod())
                .map(method -> method.ref())
                .toList();
    }
}
