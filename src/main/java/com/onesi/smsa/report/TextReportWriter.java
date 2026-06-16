package com.onesi.smsa.report;

import com.onesi.smsa.core.AnalysisResult;
import com.onesi.smsa.tree.CallTreeNode;

public class TextReportWriter {
    private static final String SEPARATOR = "==================================================";

    public String write(AnalysisResult result) {
        StringBuilder builder = new StringBuilder();
        for (CallTreeNode root : result.roots()) {
            builder.append(SEPARATOR).append(System.lineSeparator());
            builder.append(root.text()).append(System.lineSeparator());
            builder.append(SEPARATOR).append(System.lineSeparator()).append(System.lineSeparator());
            appendTree(builder, root, "");
            builder.append(System.lineSeparator());
        }

        if (!result.warnings().isEmpty()) {
            builder.append(SEPARATOR).append(System.lineSeparator());
            builder.append("Warnings").append(System.lineSeparator());
            builder.append(SEPARATOR).append(System.lineSeparator()).append(System.lineSeparator());
            result.warnings().forEach(warning -> builder.append(warning.format())
                    .append(System.lineSeparator())
                    .append(System.lineSeparator()));
        }
        return builder.toString();
    }

    private void appendTree(StringBuilder builder, CallTreeNode node, String prefix) {
        builder.append(prefix).append(node.text()).append(System.lineSeparator());
        for (int i = 0; i < node.children().size(); i++) {
            CallTreeNode child = node.children().get(i);
            boolean last = i == node.children().size() - 1;
            builder.append(prefix).append(last ? "└─ " : "├─ ").append(child.text()).append(System.lineSeparator());
            appendChildren(builder, child, prefix + (last ? "   " : "│  "));
        }
    }

    private void appendChildren(StringBuilder builder, CallTreeNode node, String prefix) {
        for (int i = 0; i < node.children().size(); i++) {
            CallTreeNode child = node.children().get(i);
            boolean last = i == node.children().size() - 1;
            builder.append(prefix).append(last ? "└─ " : "├─ ").append(child.text()).append(System.lineSeparator());
            appendChildren(builder, child, prefix + (last ? "   " : "│  "));
        }
    }
}
