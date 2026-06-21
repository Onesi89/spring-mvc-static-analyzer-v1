package com.onesi.smsa.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.onesi.smsa.core.AnalysisResult;
import com.onesi.smsa.core.AnalysisWarning;
import com.onesi.smsa.tree.CallTreeNode;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class TextReportWriterTest {
    @Test
    void writesTreeAndWarnings() {
        CallTreeNode root = new CallTreeNode("UserController.createUser()", List.of(
                new CallTreeNode("UserService.createUser()", List.of(
                        CallTreeNode.leaf("UserRepository.save()")))));
        AnalysisResult result = new AnalysisResult(List.of(root), List.of(
                new AnalysisWarning("parse-error", Path.of("Broken.java"), "Could not parse Java source.")));

        String report = new TextReportWriter().write(result);

        assertThat(report).isEqualTo("""
                ==================================================
                UserController.createUser()
                ==================================================

                UserController.createUser()
                └─ UserService.createUser()
                   └─ UserRepository.save()

                ==================================================
                Warnings
                ==================================================

                [parse-error] Broken.java
                  Could not parse Java source.

                """);
    }

    @Test
    void omitsWarningsSectionWhenNoWarnings() {
        CallTreeNode root = new CallTreeNode("OrderController.listOrders()", List.of(
                CallTreeNode.leaf("OrderService.listOrders()"),
                new CallTreeNode("AuditService.recordAccess()", List.of(
                        CallTreeNode.leaf("AuditRepository.save()")))));
        AnalysisResult result = new AnalysisResult(List.of(root), List.of());

        String report = new TextReportWriter().write(result);

        assertThat(report).contains("""
                OrderController.listOrders()
                ├─ OrderService.listOrders()
                └─ AuditService.recordAccess()
                   └─ AuditRepository.save()
                """.stripTrailing());
        assertThat(report).doesNotContain("\r\n");
        assertThat(report).doesNotContain("Warnings");
    }
}
