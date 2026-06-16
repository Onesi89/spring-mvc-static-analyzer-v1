package com.onesi.smsa.extract;

import static org.assertj.core.api.Assertions.assertThat;

import com.onesi.smsa.model.Layer;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LayerClassifierTest {
    private final LayerClassifier classifier = new LayerClassifier();

    @Test
    void classifiesSpringAnnotations() {
        assertThat(classifier.classify("UserController", Set.of("Controller"))).isEqualTo(Layer.CONTROLLER);
        assertThat(classifier.classify("UserApi", Set.of("RestController"))).isEqualTo(Layer.CONTROLLER);
        assertThat(classifier.classify("UserService", Set.of("Service"))).isEqualTo(Layer.SERVICE);
        assertThat(classifier.classify("UserRepository", Set.of("Repository"))).isEqualTo(Layer.REPOSITORY);
    }

    @Test
    void classifiesLegacySuffixes() {
        assertThat(classifier.classify("UserRepository", Set.of())).isEqualTo(Layer.REPOSITORY);
        assertThat(classifier.classify("UserDao", Set.of())).isEqualTo(Layer.DAO);
        assertThat(classifier.classify("UserDAO", Set.of())).isEqualTo(Layer.DAO);
        assertThat(classifier.classify("UserMapper", Set.of())).isEqualTo(Layer.MAPPER);
    }

    @Test
    void unknownWhenNoKnownAnnotationOrSuffixExists() {
        assertThat(classifier.classify("UserDto", Set.of())).isEqualTo(Layer.UNKNOWN);
    }
}
