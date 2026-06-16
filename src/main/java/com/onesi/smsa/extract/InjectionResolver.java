package com.onesi.smsa.extract;

import com.onesi.smsa.model.ClassInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InjectionResolver {
    public Map<String, Map<String, String>> resolve(List<ClassInfo> classes) {
        Map<String, ClassInfo> bySimpleName = classes.stream()
                .collect(Collectors.toMap(ClassInfo::simpleName, Function.identity(), (left, right) -> left));
        Map<String, Map<String, String>> result = new HashMap<>();

        for (ClassInfo classInfo : classes) {
            Map<String, String> dependencies = new HashMap<>();
            classInfo.fields().stream()
                    .filter(field -> field.annotations().contains("Autowired"))
                    .filter(field -> bySimpleName.containsKey(field.typeName()))
                    .forEach(field -> dependencies.put(field.name(), field.typeName()));
            classInfo.constructors().stream()
                    .filter(parameter -> bySimpleName.containsKey(parameter.parameterType()))
                    .forEach(parameter -> dependencies.put(parameter.parameterName(), parameter.parameterType()));
            result.put(classInfo.simpleName(), dependencies);
        }

        return result;
    }
}
