package com.onesi.smsa.extract;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.ConstructorInfo;
import com.onesi.smsa.model.FieldInfo;
import com.onesi.smsa.model.Layer;
import com.onesi.smsa.model.MethodInfo;
import com.onesi.smsa.model.MethodRef;
import com.onesi.smsa.parser.ParsedSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassModelExtractor {
    private final LayerClassifier layerClassifier;

    public ClassModelExtractor(LayerClassifier layerClassifier) {
        this.layerClassifier = layerClassifier;
    }

    public List<ClassInfo> extract(List<ParsedSource> sources) {
        List<ClassInfo> result = new ArrayList<>();
        for (ParsedSource source : sources) {
            String packageName = source.compilationUnit().getPackageDeclaration()
                    .map(packageDeclaration -> packageDeclaration.getName().asString())
                    .orElse("");
            for (ClassOrInterfaceDeclaration declaration : source.compilationUnit().findAll(ClassOrInterfaceDeclaration.class)) {
                String simpleName = declaration.getNameAsString();
                String qualifiedName = packageName.isBlank() ? simpleName : packageName + "." + simpleName;
                Set<String> annotations = annotationNames(declaration);
                Layer layer = layerClassifier.classify(simpleName, annotations);
                result.add(new ClassInfo(
                        packageName,
                        simpleName,
                        qualifiedName,
                        annotations,
                        fields(declaration),
                        constructors(declaration),
                        methods(simpleName, declaration),
                        layer));
            }
        }
        return result;
    }

    private Set<String> annotationNames(ClassOrInterfaceDeclaration declaration) {
        return declaration.getAnnotations().stream()
                .map(annotation -> annotation.getName().getIdentifier())
                .collect(Collectors.toSet());
    }

    private List<FieldInfo> fields(ClassOrInterfaceDeclaration declaration) {
        List<FieldInfo> fields = new ArrayList<>();
        for (FieldDeclaration field : declaration.getFields()) {
            Set<String> annotations = field.getAnnotations().stream()
                    .map(annotation -> annotation.getName().getIdentifier())
                    .collect(Collectors.toSet());
            field.getVariables().forEach(variable -> fields.add(new FieldInfo(
                    variable.getNameAsString(),
                    variable.getType().asString(),
                    annotations)));
        }
        return fields;
    }

    private List<ConstructorInfo> constructors(ClassOrInterfaceDeclaration declaration) {
        List<ConstructorInfo> constructors = new ArrayList<>();
        for (ConstructorDeclaration constructor : declaration.getConstructors()) {
            constructor.getParameters().forEach(parameter -> constructors.add(new ConstructorInfo(
                    parameter.getNameAsString(),
                    parameter.getType().asString())));
        }
        return constructors;
    }

    private List<MethodInfo> methods(String simpleName, ClassOrInterfaceDeclaration declaration) {
        List<MethodInfo> methods = new ArrayList<>();
        for (MethodDeclaration method : declaration.getMethods()) {
            methods.add(new MethodInfo(
                    new MethodRef(simpleName, method.getNameAsString()),
                    method.isPublic(),
                    method,
                    List.of()));
        }
        return methods;
    }
}
