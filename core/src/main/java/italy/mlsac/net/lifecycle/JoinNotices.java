// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.lifecycle;

import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.bus.Subscription;
import italy.mlsac.net.player.Audience;
import italy.mlsac.net.player.NoticeBoard;
import italy.mlsac.net.player.PlayerJoined;
import italy.mlsac.net.preset.MissingPresetNotices;
import italy.mlsac.net.text.LegacyColorizer;
import italy.mlsac.net.update.ReleaseWatcher;
import java.util.List;

public final class JoinNotices {

    static final long DELAY_TICKS = 20L;
    private static final String RULE = "&6=================================================";

    private final NoticeBoard notices;
    private final MissingPresetNotices missingPreset;
    private final ReleaseWatcher updates;
    private final LegacyColorizer colorizer;
    private Subscription subscription;

    public JoinNotices(NoticeBoard notices, MissingPresetNotices missingPreset, ReleaseWatcher updates,
                       LegacyColorizer colorizer) {
        this.notices = notices;
        this.missingPreset = missingPreset;
        this.updates = updates;
        this.colorizer = colorizer;
    }

    public void start(EventBus bus) {
        subscription = bus.subscribe(PlayerJoined.class, this::greet);
    }

    public void stop() {
        if (subscription != null) {
            subscription.cancel();
            subscription = null;
        }
    }

    private void greet(PlayerJoined joined) {
        missingPreset.greet(joined.player());
        updates.availableVersion().ifPresent(version -> notices.sendLater(joined.player(), Audience.STAFF,
                List.of(colorizer.colorize(RULE),
                        colorizer.colorize("&eA NEW MLSAC UPDATE IS AVAILABLE: &f" + version),
                        colorizer.colorize(RULE)), DELAY_TICKS));
    }
}
