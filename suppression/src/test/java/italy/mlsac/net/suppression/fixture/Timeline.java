// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.fixture;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class Timeline {

    private final ManualClock clock;
    private final List<Entry> entries = new ArrayList<>();

    public Timeline(ManualClock clock) {
        this.clock = clock;
    }

    public void note(String what) {
        entries.add(new Entry(clock.clockMillis(), what));
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public List<String> rendered() {
        return entries.stream().map(entry -> entry.at() + " " + entry.what()).collect(Collectors.toList());
    }

    public List<Long> timesOf(String prefix) {
        return entries.stream().filter(entry -> entry.what().startsWith(prefix)).map(Entry::at)
                .collect(Collectors.toList());
    }

    public void clear() {
        entries.clear();
    }

    public record Entry(long at, String what) {
    }
}
