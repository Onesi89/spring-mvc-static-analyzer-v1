package com.onesi.smsa.extract;

import static org.assertj.core.api.Assertions.assertThat;

import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.ConstructorInfo;
import com.onesi.smsa.model.FieldInfo;
import com.onesi.smsa.model.Layer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InjectionResolverTest {
    @Test
    void resolvesAutowiredFieldsAndConstructorParametersByType() {
        ClassInfo controller = new ClassInfo("", "UserController", "UserController", Set.of("Controller"),
                List.of(new FieldInfo("userService", "UserService", Set.of("Autowired"))),
                List.of(new ConstructorInfo("historyService", "HistoryService")),
                List.of(),
                Layer.CONTROLLER);
        ClassInfo userService = new ClassInfo("", "UserService", "UserService", Set.of("Service"),
                List.of(), List.of(), List.of(), Layer.SERVICE);
        ClassInfo historyService = new ClassInfo("", "HistoryService", "HistoryService", Set.of("Service"),
                List.of(), List.of(), List.of(), Layer.SERVICE);

        Map<String, Map<String, String>> resolved = new InjectionResolver().resolve(List.of(controller, userService, historyService));

        assertThat(resolved.get("UserController"))
                .containsEntry("userService", "UserService")
                .containsEntry("historyService", "HistoryService");
    }
}
