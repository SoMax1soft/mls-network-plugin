// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.profile;

import italy.mlsac.net.backend.dossier.DossierKind;
import italy.mlsac.net.backend.dossier.DossierQuery;
import italy.mlsac.net.backend.dossier.DossierReply;
import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.commands.CommandLanes;
import italy.mlsac.net.commands.Completions;
import italy.mlsac.net.commands.PlayerLookup;
import italy.mlsac.net.commands.Target;
import italy.mlsac.net.config.staff.DossierSettings;
import italy.mlsac.net.dossier.DossierDesk;
import italy.mlsac.net.dossier.DossierReading;
import italy.mlsac.net.dossier.DossierTimes;
import italy.mlsac.net.dossier.PlayerDossier;
import italy.mlsac.net.menu.DossierTrouble;
import italy.mlsac.net.menu.ProfileValues;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class ProfileSubcommand implements Subcommand {

    static final UUID CONSOLE_VIEWER = new UUID(0L, 0L);
    static final Pattern PLAYER_NAME = Pattern.compile("^[A-Za-z0-9_.*-]{1,32}$");

    private final PlayerLookup players;
    private final ProfileFacts facts;
    private final MessageBook messages;
    private final ProfileMenus menus;
    private final Supplier<DossierSettings> settings;
    private final DossierDesk dossiers;
    private final DossierTimes times;
    private final CommandLanes lanes;

    public ProfileSubcommand(PlayerLookup players, ProfileFacts facts, MessageBook messages, ProfileMenus menus,
                             Supplier<DossierSettings> settings, DossierDesk dossiers, DossierTimes times,
                             CommandLanes lanes) {
        this.players = players;
        this.facts = facts;
        this.messages = messages;
        this.menus = menus;
        this.settings = settings;
        this.dossiers = dossiers;
        this.times = times;
        this.lanes = lanes;
    }

    @Override
    public SubcommandName name() {
        return SubcommandName.PROFILE;
    }

    @Override
    public void execute(CommandActor invoker, List<String> args) {
        if (args.isEmpty()) {
            invoker.send(messages.prefixed("usage-profile"));
            return;
        }
        Optional<Target> online = players.byNamePrefix(args.get(0));
        String player = online.map(Target::name).orElse(args.get(0));
        if (!PLAYER_NAME.matcher(player).matches()) {
            invoker.send(messages.prefixed("menu-invalid-player"));
            return;
        }
        DossierSettings now = settings.get();
        Optional<UUID> viewer = invoker.actorUuid();
        if (now.profile().enabled() && viewer.isPresent()) {
            menus.openProfile(viewer.get(), invoker.name(), player);
            return;
        }
        List<String> head = new ArrayList<>();
        head.add(messages.text("profile-header", TemplateValues.of("{PLAYER}", player)));
        online.ifPresent(target -> head.addAll(localLines(target)));
        head.forEach(invoker::send);
        DossierQuery query = DossierQuery.of(DossierKind.PROFILE).with("player", player).with("scope", now.scope())
                .with("latest", now.profile().latest());
        dossiers.ask(viewer.orElse(CONSOLE_VIEWER), query)
                .thenAccept(reply -> lanes.reply(invoker, dossierLines(reply, player)));
    }

    private List<String> localLines(Target target) {
        String sens = facts.sensitivityPercent(target.id()).stream().mapToObj(Integer::toString).findFirst()
                .orElse("N/A");
        String client = facts.clientVersion(target.id()).orElse("Unknown");
        TemplateValues values = TemplateValues.of("{PLAYER}", target.name()).and("{SENS}", sens)
                .and("{CLIENT}", client).and("{DETECTIONS}", facts.highProbabilityCount(target.id()));
        List<String> lines = messages.lines("profile-info", values);
        return lines.isEmpty() ? messages.lines("profile-info-fallback", values) : lines;
    }

    private List<String> dossierLines(DossierReply reply, String player) {
        if (!(reply instanceof DossierReply.Found found)) {
            return List.of(messages.text("profile-dossier-" + DossierTrouble.of(reply).key()));
        }
        PlayerDossier dossier = DossierReading.profile(found.data());
        if (!dossier.known()) {
            return List.of(messages.text("profile-dossier-unknown", TemplateValues.of("{PLAYER}", player)));
        }
        return messages.lines("profile-dossier", ProfileValues.of(dossier, player, times));
    }

    @Override
    public List<String> complete(CommandActor invoker, List<String> args) {
        return args.size() == 1 ? Completions.matching(players.onlineNames(), args.get(0)) : List.of();
    }
}
