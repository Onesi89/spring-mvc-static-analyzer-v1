package com.onesi.smsa.report;

import com.onesi.smsa.core.AnalysisResult;
import com.onesi.smsa.tree.CallTreeNode;

public class TextReportWriter {
    private static final String SEPARATOR = "==================================================";
    private static final String NEW_LINE = "\n";

    public String write(AnalysisResult result) {
        StringBuilder builder = new StringBuilder();
        for (CallTreeNode root : result.roots()) {
            builder.append(SEPARATOR).append(NEW_LINE);
            builder.append(root.text()).append(NEW_LINE);
            builder.append(SEPARATOR).append(NEW_LINE).append(NEW_LINE);
            appendTree(builder, root, "");
            builder.append(NEW_LINE);
        }

        if (!result.warnings().isEmpty()) {
            builder.append(SEPARATOR).append(NEW_LINE);
            builder.append("Warnings").append(NEW_LINE);
            builder.append(SEPARATOR).append(NEW_LINE).append(NEW_LINE);
            result.warnings().forEach(warning -> builder.append(warning.format())
                    .append(NEW_LINE)
                    .append(NEW_LINE));
        }
        return builder.toString();
    }

    private void appendTree(StringBuilder builder, CallTreeNode node, String prefix) {
        appendNode(builder, node, prefix, "");
    }

    private void appendNode(StringBuilder builder, CallTreeNode node, String prefix, String connector) {
        builder.append(prefix).append(connector).append(node.text()).append(NEW_LINE);
        String childPrefix = prefix + switch (connector) {
            case "├─ " -> "│  ";
            case "└─ " -> "   ";
            default -> "";
        };
        for (int i = 0; i < node.children().size(); i++) {
            CallTreeNode child = node.children().get(i);
            boolean last = i == node.children().size() - 1;
            appendNode(builder, child, childPrefix, last ? "└─ " : "├─ ");
        }
    }
}
