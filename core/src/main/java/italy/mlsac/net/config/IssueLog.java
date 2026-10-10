// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config;

import java.util.ArrayList;
import java.util.List;

public final class IssueLog {

    private final List<ConfigIssue> issues = new ArrayList<>();
    private int acceptedValues;

    public void drop(LayerSource source, String path, String reason, IssueSeverity severity) {
        issues.add(new ConfigIssue(source, path, reason, severity));
    }

    public void accepted() {
        acceptedValues++;
    }

    public int acceptedValues() {
        return acceptedValues;
    }

    public List<ConfigIssue> issues() {
        return List.copyOf(issues);
    }
}
