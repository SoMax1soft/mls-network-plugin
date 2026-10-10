// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import italy.mlsac.net.commands.Target;
import italy.mlsac.net.text.MessageBook;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProbTracking {

    private final Map<UUID, Watch> watches = new ConcurrentHashMap<>();
    private final ActionBarPorts ports;
    private final ActionBarText texts;
    private final MessageBook messages;

    public ProbTracking(ActionBarPorts ports, ActionBarText texts, MessageBook messages) {
        this.ports = ports;
        this.texts = texts;
        this.messages = messages;
    }

    public boolean stop(UUID viewer) {
        return watches.remove(viewer) != null;
    }

    public void start(UUID viewer, Target target) {
        Watch watch = new Watch(viewer, target);
        watches.put(viewer, watch);
        ports.repeatOnViewerThread(viewer, ActionBarPorts.CADENCE_TICKS, watch::pulse);
    }

    public void haltEvery() {
        watches.clear();
    }

    private final class Watch {

        private final UUID viewer;
        private final Target target;

        private Watch(UUID viewer, Target target) {
            this.viewer = viewer;
            this.target = target;
        }

        private boolean pulse() {
            boolean current = watches.get(viewer) == this;
            boolean viewerHere = current && ports.online(viewer);
            boolean targetHere = viewerHere && ports.online(target.id());
            if (targetHere) {
                ports.showHotbarText(viewer, frame());
                return true;
            }
            if (viewerHere) {
                ports.showHotbarText(viewer, messages.text("player-offline"));
            }
            if (current) {
                watches.remove(viewer, this);
            }
            return false;
        }

        private String frame() {
            return ports.snapshot(target.id())
                    .map(suspect -> texts.build(suspect, ports.offenceLevel(target.id())))
                    .orElseGet(() -> texts.unscored(target.name()));
        }
    }
}
