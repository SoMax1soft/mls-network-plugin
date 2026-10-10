// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.console.LogLevel;
import italy.mlsac.net.console.LogSink;
import java.util.ArrayList;
import java.util.List;

public final class CapturingLogSink implements LogSink {

    public record Line(LogLevel level, String text, Throwable trace) {
    }

    private final List<Line> lines = new ArrayList<>();

    @Override
    public synchronized void write(LogLevel level, String line, Throwable attachedTrace) {
        lines.add(new Line(level, line, attachedTrace));
    }

    public synchronized List<Line> lines() {
        return List.copyOf(lines);
    }

    public synchronized List<String> texts(LogLevel level) {
        return lines.stream().filter(line -> line.level() == level).map(Line::text).toList();
    }

    public synchronized boolean contains(String fragment) {
        return lines.stream().anyMatch(line -> line.text().contains(fragment));
    }
}
