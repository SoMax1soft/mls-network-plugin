// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import italy.mlsac.net.logbook.Logbook;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class AnimationLibrary {

    public static final String DEFAULT_ANIMATION = "classic_ban";

    private final ScriptReader reader;
    private final Logbook log;
    private volatile Map<String, AnimationScript> scripts = Map.of();

    public AnimationLibrary(ScriptReader reader, Logbook log) {
        this.reader = reader;
        this.log = log;
    }

    public int load(List<AnimationFile> files) {
        Map<String, AnimationScript> loaded = new TreeMap<>();
        for (AnimationFile file : files) {
            if (!file.fileName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                continue;
            }
            try {
                AnimationScript script = reader.read(file.fileName(), file.document().get());
                loaded.put(script.name(), script);
            } catch (RuntimeException failure) {
                log.warn("[Animation] Failed to load " + file.fileName() + ": " + failure.getMessage());
            }
        }
        scripts = Map.copyOf(loaded);
        return loaded.size();
    }

    public Optional<AnimationScript> find(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(scripts.get(name.toLowerCase(Locale.ROOT)));
    }

    public List<String> names() {
        return new TreeMap<>(scripts).keySet().stream().toList();
    }

    public int size() {
        return scripts.size();
    }
}
