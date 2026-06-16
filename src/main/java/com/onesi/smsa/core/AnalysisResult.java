package com.onesi.smsa.core;

import com.onesi.smsa.tree.CallTreeNode;
import java.util.List;

public record AnalysisResult(List<CallTreeNode> roots, List<AnalysisWarning> warnings) {
    public AnalysisResult {
        roots = List.copyOf(roots);
        warnings = List.copyOf(warnings);
    }
}
