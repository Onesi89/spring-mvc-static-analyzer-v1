package com.onesi.smsa.tree;

import java.util.List;

public record CallTreeNode(String text, List<CallTreeNode> children) {
    public CallTreeNode {
        children = List.copyOf(children);
    }

    public static CallTreeNode leaf(String text) {
        return new CallTreeNode(text, List.of());
    }
}
