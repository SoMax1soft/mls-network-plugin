// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.alerting.CrossServerFeed;
import italy.mlsac.net.alerting.RemoteEvent;
import italy.mlsac.net.backend.notice.InterServerAlert;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Stream;

public final class CrossServerRelay {

    private final CrossServerFeed feed;

    public CrossServerRelay(CrossServerFeed feed) {
        this.feed = feed;
    }

    public void received(InterServerAlert alert) {
        List<Optional<String>> labels = Stream.of(alert.type(), alert.model(), alert.action(), alert.hostLabel(),
                alert.playerNick()).map(Optional::ofNullable).toList();
        feed.receive(new RemoteEvent(alert.id(), labels.get(0), labels.get(1), labels.get(2), labels.get(3),
                labels.get(4), OptionalDouble.of(alert.offenceLevel()), OptionalDouble.empty(),
                alert.probability(), alert.buffer(), false));
    }
}
