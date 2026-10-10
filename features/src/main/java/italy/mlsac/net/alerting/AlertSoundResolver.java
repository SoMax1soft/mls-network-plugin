// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import italy.mlsac.net.logbook.WarnOnce;
import java.util.Optional;
import java.util.regex.Pattern;

public final class AlertSoundResolver {

    private static final Pattern ACCEPTED = Pattern.compile("^[A-Za-z0-9_.]{1,64}$");

    private final SoundCatalog catalog;
    private final WarnOnce warnings;

    public AlertSoundResolver(SoundCatalog catalog, WarnOnce warnings) {
        this.catalog = catalog;
        this.warnings = warnings;
    }

    public Optional<String> resolve(String requested) {
        if (requested != null && ACCEPTED.matcher(requested).matches() && catalog.exists(requested)) {
            return Optional.of(requested);
        }
        warnings.warn("sound:" + requested, "[Alerts] Unknown alert sound '" + requested + "', using "
                + AlertSound.DEFAULT_TYPE);
        if (catalog.exists(AlertSound.DEFAULT_TYPE)) {
            return Optional.of(AlertSound.DEFAULT_TYPE);
        }
        warnings.warn("sound-default", "[Alerts] Fallback sound " + AlertSound.DEFAULT_TYPE
                + " is not available on this server; alerts play no sound");
        return Optional.empty();
    }
}
