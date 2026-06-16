package com.onesi.smsa.extract;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.Layer;
import com.onesi.smsa.parser.ParsedSource;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClassModelExtractorTest {
    @Test
    void extractsClassAnnotationsFieldsConstructorsAndPublicMethods() {
        String source = """
                package demo;

                import org.springframework.stereotype.Controller;
                import org.springframework.beans.factory.annotation.Autowired;

                @Controller
                public class UserController {
                    @Autowired
                    private UserService userService;

                    public UserController(HistoryService historyService) {
                    }

                    public void createUser() {
                        userService.createUser();
                    }

                    private void helper() {
                    }
                }
                """;
        ParsedSource parsed = new ParsedSource(Path.of("UserController.java"), StaticJavaParser.parse(source));

        List<ClassInfo> classes = new ClassModelExtractor(new LayerClassifier()).extract(List.of(parsed));

        assertThat(classes).hasSize(1);
        ClassInfo controller = classes.get(0);
        assertThat(controller.packageName()).isEqualTo("demo");
        assertThat(controller.simpleName()).isEqualTo("UserController");
        assertThat(controller.qualifiedName()).isEqualTo("demo.UserController");
        assertThat(controller.layer()).isEqualTo(Layer.CONTROLLER);
        assertThat(controller.annotations()).containsExactly("Controller");
        assertThat(controller.fields()).extracting("name").containsExactly("userService");
        assertThat(controller.fields().get(0).annotations()).containsExactly("Autowired");
        assertThat(controller.constructors()).extracting("parameterType").containsExactly("HistoryService");
        assertThat(controller.methods()).extracting(method -> method.ref().methodName()).contains("createUser", "helper");
        assertThat(controller.methods()).allSatisfy(method -> assertThat(method.rawCalls()).isEmpty());
    }
}
