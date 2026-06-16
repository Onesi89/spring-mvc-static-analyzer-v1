package com.onesi.smsa.extract;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.Test;

class MethodCallExtractorTest {
    @Test
    void extractsScopedAndUnscopedCallsInEncounterOrder() {
        MethodDeclaration method = StaticJavaParser.parseMethodDeclaration("""
                public void createUser() {
                    validate();
                    userService.createUser();
                    this.audit();
                }
                """);

        assertThat(new MethodCallExtractor().extract(method))
                .extracting(ExtractedCall::displayText)
                .containsExactly("validate()", "userService.createUser()", "this.audit()");
    }
}
