// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.staff;

import italy.mlsac.net.config.SettingsLayer;

public final class StaffToolReader {

    public DataRestoreSettings restore(DataRestoreSettings lower, SettingsLayer layer) {
        return new DataRestoreSettings(
                layer.bool("data-restore.enabled").orElse(lower.enabled()),
                layer.intClamped("data-restore.max-players", 1, 500).orElse(lower.maxPlayers()),
                layer.intClamped("data-restore.max-memory-mb", 8, 1024).orElse(lower.maxMegabytes()));
    }

    public DossierSettings dossier(DossierSettings lower, SettingsLayer layer) {
        DossierSettings.KickPage kicks = lower.kicks();
        DossierSettings.ProfileSheet profile = lower.profile();
        return new DossierSettings(
                layer.bool("dossier.network").orElse(lower.network()),
                new DossierSettings.KickPage(
                        layer.bool("dossier.kicks.enabled").orElse(kicks.enabled()),
                        layer.intClamped("dossier.kicks.days", 1, 30).orElse(kicks.days()),
                        layer.bool("dossier.kicks.bans").orElse(kicks.bans()),
                        layer.bool("dossier.kicks.suppressions").orElse(kicks.suppressions())),
                new DossierSettings.ProfileSheet(
                        layer.bool("dossier.profile.enabled").orElse(profile.enabled()),
                        layer.intClamped("dossier.profile.latest", 1, 28).orElse(profile.latest()),
                        layer.bool("dossier.profile.suppressions").orElse(profile.suppressions()),
                        layer.bool("dossier.profile.punishments").orElse(profile.punishments())));
    }
}
