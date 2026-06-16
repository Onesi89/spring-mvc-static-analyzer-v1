package com.onesi.smsa.extract;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.ConstructorInfo;
import com.onesi.smsa.model.FieldInfo;
import com.onesi.smsa.model.Layer;
import com.onesi.smsa.model.MethodInfo;
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

        assertThat(controller.fields()).hasSize(1);
        FieldInfo field = controller.fields().get(0);
        assertThat(field.name()).isEqualTo("userService");
        assertThat(field.typeName()).isEqualTo("UserService");
        assertThat(field.annotations()).containsExactly("Autowired");

        assertThat(controller.constructors()).hasSize(1);
        ConstructorInfo constructor = controller.constructors().get(0);
        assertThat(constructor.parameterName()).isEqualTo("historyService");
        assertThat(constructor.parameterType()).isEqualTo("HistoryService");

        MethodInfo createUser = controller.methods().stream()
                .filter(method -> method.ref().methodName().equals("createUser"))
                .findFirst()
                .orElseThrow();
        assertThat(createUser.ref().className()).isEqualTo("UserController");
        assertThat(createUser.ref().methodName()).isEqualTo("createUser");
        assertThat(createUser.publicMethod()).isTrue();
        assertThat(createUser.rawCalls()).isEmpty();

        MethodInfo helper = controller.methods().stream()
                .filter(method -> method.ref().methodName().equals("helper"))
                .findFirst()
                .orElseThrow();
        assertThat(helper.ref().className()).isEqualTo("UserController");
        assertThat(helper.ref().methodName()).isEqualTo("helper");
        assertThat(helper.publicMethod()).isFalse();
        assertThat(helper.rawCalls()).isEmpty();
    }
}
