// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.commands.PlayerLookup;
import italy.mlsac.net.commands.Target;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import java.util.List;
import java.util.Optional;

public final class DirectoryLookup implements PlayerLookup {

    private final PlayerDirectory players;

    public DirectoryLookup(PlayerDirectory players) {
        this.players = players;
    }

    @Override
    public Optional<Target> byNamePrefix(String name) {
        return players.byNamePrefix(name).map(DirectoryLookup::target);
    }

    @Override
    public Optional<Target> byExactName(String name) {
        return players.byExactName(name).map(DirectoryLookup::target);
    }

    @Override
    public List<String> onlineNames() {
        return players.online().stream().map(PlayerView::name).toList();
    }

    private static Target target(PlayerView view) {
        return new Target(view.id(), view.name());
    }
}
