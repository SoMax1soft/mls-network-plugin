// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class FakePlayers implements PlayerDirectory {

    private final List<FakePlayer> players = new ArrayList<>();

    public synchronized FakePlayer add(String name) {
        FakePlayer player = new FakePlayer(UUID.nameUUIDFromBytes(name.getBytes()), name);
        players.add(player);
        return player;
    }

    @Override
    public synchronized Optional<PlayerView> byId(UUID id) {
        return players.stream().filter(p -> p.id().equals(id)).<PlayerView>map(p -> p).findFirst();
    }

    @Override
    public synchronized Optional<PlayerView> byNamePrefix(String name) {
        String prefix = name.toLowerCase(Locale.ROOT);
        return players.stream().filter(p -> p.name().toLowerCase(Locale.ROOT).startsWith(prefix))
                .<PlayerView>map(p -> p).findFirst();
    }

    @Override
    public synchronized Optional<PlayerView> byExactName(String name) {
        return players.stream().filter(p -> p.name().equalsIgnoreCase(name)).<PlayerView>map(p -> p).findFirst();
    }

    @Override
    public synchronized List<PlayerView> online() {
        return new ArrayList<>(players.stream().filter(FakePlayer::online).toList());
    }
}
