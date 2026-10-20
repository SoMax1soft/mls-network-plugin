// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection.choice.bukkit;

import org.bukkit.event.Event;

@FunctionalInterface
public interface ApiEventSink {

    void fire(Event event);
}
