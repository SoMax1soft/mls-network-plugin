// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.staff;

public record DossierSettings(boolean network, KickPage kicks, ProfileSheet profile) {

    public static DossierSettings pluginDefaults() {
        return new DossierSettings(false, new KickPage(true, 7, true, true), new ProfileSheet(true, 14, true, true));
    }

    public String scope() {
        return network ? "owner" : "server";
    }

    public record KickPage(boolean enabled, int days, boolean bans, boolean suppressions) {
    }

    public record ProfileSheet(boolean enabled, int latest, boolean suppressions, boolean punishments) {
    }
}
