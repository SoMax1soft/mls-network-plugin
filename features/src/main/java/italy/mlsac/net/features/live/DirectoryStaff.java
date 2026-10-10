// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.alerting.StaffMember;
import italy.mlsac.net.alerting.StaffRoster;
import italy.mlsac.net.player.Audience;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DirectoryStaff implements StaffRoster {

    private final PlayerDirectory players;
    private final StaffSounds sounds;

    public DirectoryStaff(PlayerDirectory players, StaffSounds sounds) {
        this.players = players;
        this.sounds = sounds;
    }

    @Override
    public Optional<StaffMember> find(UUID player) {
        return players.byId(player).map(this::member);
    }

    @Override
    public List<StaffMember> online() {
        return players.online().stream().map(this::member).toList();
    }

    private StaffMember member(PlayerView view) {
        return new ViewedMember(view, sounds);
    }

    private record ViewedMember(PlayerView view, StaffSounds sounds) implements StaffMember {

        @Override
        public UUID id() {
            return view.id();
        }

        @Override
        public String name() {
            return view.name();
        }

        @Override
        public boolean online() {
            return view.online();
        }

        @Override
        public boolean staff() {
            return Audience.STAFF.test(view);
        }

        @Override
        public boolean operator() {
            return view.op();
        }

        @Override
        public void send(String coloredText) {
            view.sendMessage(coloredText);
        }

        @Override
        public void playSound(String sound, float volume, float pitch) {
            sounds.play(view.id(), sound, volume, pitch);
        }
    }
}
