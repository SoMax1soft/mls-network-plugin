// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import italy.mlsac.net.logbook.Logbook;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class SubscriberBroadcast {

    private final StaffRoster staff;
    private final DeliveryLane lane;
    private final Supplier<AlertOutput> output;
    private final AlertSoundResolver sounds;
    private final AlertTexts texts;
    private final Logbook log;

    public SubscriberBroadcast(StaffRoster staff, DeliveryLane lane, Supplier<AlertOutput> output,
                               AlertSoundResolver sounds, AlertTexts texts, Logbook log) {
        this.staff = staff;
        this.lane = lane;
        this.output = output;
        this.sounds = sounds;
        this.texts = texts;
        this.log = log;
    }

    public void alert(SubscriberSet subscribers, String line) {
        AlertOutput current = output.get();
        Optional<String> sound = current.sound().enabled() ? sounds.resolve(current.sound().type()) : Optional.empty();
        for (UUID subscriber : subscribers.members()) {
            lane.onPlayerThread(subscriber, () -> deliverAlert(subscriber, line, sound, current.sound()));
        }
        if (current.console()) {
            lane.onMainThread(() -> log.info(texts.strip(line)));
        }
    }

    public void quietly(SubscriberSet subscribers, String line) {
        for (UUID subscriber : subscribers.members()) {
            lane.onPlayerThread(subscriber, () -> staff.find(subscriber)
                    .filter(StaffMember::online).filter(StaffMember::staffOrOp)
                    .ifPresent(member -> member.send(line)));
        }
    }

    private void deliverAlert(UUID subscriber, String line, Optional<String> sound, AlertSound settings) {
        Optional<StaffMember> member = staff.find(subscriber).filter(StaffMember::online);
        member.filter(StaffMember::staffOrOp).ifPresent(target -> target.send(line));
        sound.ifPresent(name -> member.ifPresent(target -> target.playSound(name, settings.volume(), settings.pitch())));
    }
}
