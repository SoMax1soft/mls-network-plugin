// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.dossier.DossierTimes;
import italy.mlsac.net.dossier.HistoryEntry;
import italy.mlsac.net.dossier.ModelTally;
import italy.mlsac.net.dossier.PlayerDossier;
import italy.mlsac.net.text.TemplateValues;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ProfileValues {

    static final int SHOWN_MODELS = 3;

    private ProfileValues() {
    }

    public static TemplateValues of(PlayerDossier dossier, String player, DossierTimes times) {
        Optional<HistoryEntry> last = dossier.punishments().last();
        String models = dossier.detections().models().stream().limit(SHOWN_MODELS)
                .map(ProfileValues::model).collect(Collectors.joining("§8, "));
        return TemplateValues.of("{PLAYER}", player)
                .and("{FIRST_SEEN}", times.stamp(dossier.firstSeen()))
                .and("{LAST_SEEN}", times.stamp(dossier.lastSeen()))
                .and("{LAST_SEEN_AGO}", times.ago(dossier.lastSeen()))
                .and("{TOTAL}", dossier.detections().total())
                .and("{DAY}", dossier.detections().day())
                .and("{WEEK}", dossier.detections().week())
                .and("{MAX}", ValueShade.twoDecimals(dossier.detections().max()))
                .and("{AVG}", ValueShade.twoDecimals(dossier.detections().average()))
                .and("{MODELS}", models.isEmpty() ? DossierShades.NONE : models)
                .and("{KICKS}", dossier.punishments().kicks())
                .and("{BANS}", dossier.punishments().bans())
                .and("{PUNISHMENTS}", dossier.punishments().total())
                .and("{SUPPRESSIONS}", dossier.suppressionTotal())
                .and("{LAST_ACTION}", last.map(entry -> DossierShades.shownAction(entry.action()))
                        .orElse(DossierShades.NONE))
                .and("{LAST_AGO}", last.map(entry -> times.ago(entry.at())).orElse(DossierShades.NONE));
    }

    private static String model(ModelTally tally) {
        return "§f" + tally.model() + " " + tally.count() + " §8(" + ValueShade.twoDecimals(tally.max()) + "§8)";
    }
}
