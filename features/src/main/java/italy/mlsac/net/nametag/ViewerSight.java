// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ViewerSight {

    private final OverheadTagPainter renderer;
    private final DestroyStyle destroyStyle;
    private final SuspectTags tags;
    private final OwnTagSeat seat;
    private final Map<Integer, Sighting> tracked = new HashMap<>();
    private boolean staff;

    public ViewerSight(OverheadTagPainter renderer, DestroyStyle destroyStyle, SuspectTags tags, boolean staff,
                       int ownEntityNumber) {
        this.renderer = renderer;
        this.destroyStyle = destroyStyle;
        this.tags = tags;
        this.staff = staff;
        this.seat = new OwnTagSeat(ownEntityNumber);
    }

    public synchronized boolean staff() {
        return staff;
    }

    public synchronized int ownEntityNumber() {
        return seat.entityNumber();
    }

    public synchronized List<NametagPacket> spawned(int entityNumber, double x, double y, double z, NametagLook look) {
        if (!staff) {
            return List.of();
        }
        Optional<SuspectTags.Tag> tag = tags.byEntity(entityNumber);
        if (tag.isEmpty()) {
            return List.of();
        }
        List<NametagPacket> packets = new ArrayList<>(hide(tracked.get(entityNumber)));
        Sighting sighting = new Sighting(tag.get().suspect(), entityNumber, x, y, z);
        tracked.put(entityNumber, sighting);
        packets.addAll(showIfWanted(sighting, tag.get(), look));
        return packets;
    }

    public synchronized DestroyReaction destroyed(List<Integer> entityNumbers) {
        List<Integer> tagIds = new ArrayList<>();
        for (int entityNumber : entityNumbers) {
            Sighting sighting = tracked.remove(entityNumber);
            if (sighting != null && sighting.shown()) {
                tagIds.addAll(sighting.shownTag().entities().numbers());
            }
        }
        return tagIds.isEmpty() ? DestroyReaction.none() : destroyStyle.react(tagIds);
    }

    public synchronized void viewReset(ClientStage stage) {
        tracked.clear();
        seat.enter(stage);
    }

    public synchronized List<NametagPacket> movedBy(int entityNumber, double dx, double dy, double dz) {
        Sighting sighting = tracked.get(entityNumber);
        if (sighting == null) {
            return List.of();
        }
        sighting.shiftBy(dx, dy, dz);
        return sighting.shown() ? renderer.relativeMove(sighting.shownTag(), dx, dy, dz) : List.of();
    }

    public synchronized List<NametagPacket> movedTo(int entityNumber, double x, double y, double z) {
        Sighting sighting = tracked.get(entityNumber);
        if (sighting == null) {
            return List.of();
        }
        sighting.moveTo(x, y, z);
        return sighting.shown() ? renderer.teleport(sighting.shownTag(), sighting.view()) : List.of();
    }

    public synchronized List<NametagPacket> ownMoved(double x, double y, double z) {
        seat.place(x, y, z);
        Sighting own = tracked.get(seat.entityNumber());
        if (own == null || own.standsAt(x, y, z)) {
            return List.of();
        }
        own.moveTo(x, y, z);
        return own.shown() ? renderer.teleport(own.shownTag(), own.view()) : List.of();
    }

    public synchronized void ownPlaced(SeedSighting place) {
        seat.place(place.x(), place.y(), place.z());
    }

    public synchronized List<NametagPacket> renumberOwn(int entityNumber) {
        if (entityNumber == seat.entityNumber()) {
            return List.of();
        }
        Sighting own = tracked.remove(seat.entityNumber());
        seat.renumber(entityNumber);
        return hide(own);
    }

    public synchronized List<NametagPacket> posed(int entityNumber, SuspectPose pose) {
        Sighting sighting = tracked.get(entityNumber);
        if (sighting == null || sighting.pose() == pose) {
            return List.of();
        }
        SuspectPose previous = sighting.pose();
        sighting.pose(pose);
        return sighting.shown() ? renderer.poseChanged(sighting.shownTag(), sighting.view(), previous) : List.of();
    }

    public synchronized Optional<List<Integer>> passengers(int entityNumber, List<Integer> serverPassengers) {
        Sighting sighting = tracked.get(entityNumber);
        if (sighting == null) {
            return Optional.empty();
        }
        List<Integer> withoutTag = sighting.shown()
                ? serverPassengers.stream().filter(id -> !sighting.shownTag().entities().contains(id)).toList()
                : serverPassengers;
        sighting.passengers(withoutTag);
        if (!sighting.shown()) {
            return Optional.empty();
        }
        List<Integer> rewritten = renderer.passengersWithTag(sighting.shownTag(), withoutTag);
        return rewritten.equals(serverPassengers) ? Optional.empty() : Optional.of(rewritten);
    }

    public synchronized void becomeStaff(List<SeedSighting> seeds) {
        staff = true;
        for (SeedSighting seed : seeds) {
            tags.byEntity(seed.entityNumber()).ifPresent(tag -> tracked.put(seed.entityNumber(),
                    new Sighting(tag.suspect(), seed.entityNumber(), seed.x(), seed.y(), seed.z())));
        }
    }

    public synchronized void loseStaff() {
        staff = false;
    }

    public synchronized boolean missingOwnTag(NametagLook look) {
        int own = seat.entityNumber();
        return look.enabled() && ownTagAllowed(look) && !tracked.containsKey(own) && seat.place().isPresent()
                && tags.byEntity(own).flatMap(SuspectTags.Tag::displayedText).isPresent();
    }

    public synchronized List<NametagPacket> reconcile(NametagLook look) {
        List<NametagPacket> packets = new ArrayList<>(seatOwnTag(look));
        for (Map.Entry<Integer, Sighting> entry : tracked.entrySet()) {
            Sighting sighting = entry.getValue();
            Optional<SuspectTags.Tag> tag = tags.byEntity(entry.getKey())
                    .filter(current -> current.suspect().equals(sighting.suspect()));
            boolean wanted = staff && tag.isPresent() && tag.get().displayedText().isPresent() && look.enabled();
            if (sighting.shown() && (!wanted || outdated(sighting.shownTag(), tag.get(), look))) {
                packets.addAll(hide(sighting));
            }
            if (!wanted) {
                continue;
            }
            TagLines text = tag.get().displayedText().get();
            if (!sighting.shown()) {
                packets.addAll(show(sighting, tag.get(), look));
            } else if (!text.equals(sighting.shownTag().text())) {
                sighting.markShown(sighting.shownTag().retexted(text));
                packets.addAll(renderer.text(sighting.shownTag()));
            }
        }
        if (!staff) {
            tracked.clear();
        }
        return packets;
    }

    public synchronized List<NametagPacket> repairPassengers() {
        List<NametagPacket> packets = new ArrayList<>();
        for (Sighting sighting : tracked.values()) {
            if (sighting.shown()) {
                packets.addAll(renderer.passengerRepair(sighting.shownTag(), sighting.view()));
            }
        }
        return packets;
    }

    public synchronized List<NametagPacket> destroyAll() {
        List<Integer> ids = tracked.values().stream().filter(Sighting::shown)
                .flatMap(sighting -> sighting.shownTag().entities().numbers().stream()).toList();
        tracked.values().forEach(Sighting::markHidden);
        tracked.clear();
        return ids.isEmpty() ? List.of() : List.of(new NametagPacket.Destroy(ids));
    }

    public synchronized int shownCount() {
        return (int) tracked.values().stream().filter(Sighting::shown).count();
    }

    public synchronized int trackedCount() {
        return tracked.size();
    }

    private boolean ownTagAllowed(NametagLook look) {
        return staff && look.ownTagShown() && seat.acceptsEntities();
    }

    private List<NametagPacket> seatOwnTag(NametagLook look) {
        int own = seat.entityNumber();
        Sighting current = tracked.get(own);
        if (!ownTagAllowed(look)) {
            tracked.remove(own);
            return hide(current);
        }
        Optional<SuspectTags.Tag> tag = tags.byEntity(own);
        Optional<SeedSighting> place = seat.place();
        if (current == null && tag.isPresent() && place.isPresent()) {
            tracked.put(own, new Sighting(tag.get().suspect(), own, place.get().x(), place.get().y(), place.get().z()));
        }
        return List.of();
    }

    private boolean outdated(ShownTag shown, SuspectTags.Tag tag, NametagLook look) {
        return shown.lookVersion() != look.version()
                || !shown.entities().bottom().equals(tag.entities().bottom())
                || !renderer.claim(tag.entities(), tag.displayedText().orElseThrow()).equals(shown.entities());
    }

    private List<NametagPacket> showIfWanted(Sighting sighting, SuspectTags.Tag tag, NametagLook look) {
        return tag.displayedText().isPresent() && look.enabled() ? show(sighting, tag, look) : List.of();
    }

    private List<NametagPacket> show(Sighting sighting, SuspectTags.Tag tag, NametagLook look) {
        TagLines text = tag.displayedText().orElseThrow();
        ShownTag shown = new ShownTag(renderer.claim(tag.entities(), text), look.version(), text,
                look.settings().layout().extraHeight());
        sighting.markShown(shown);
        return renderer.spawn(shown, sighting.view(), look.settings());
    }

    private static List<NametagPacket> hide(Sighting sighting) {
        if (sighting == null || !sighting.shown()) {
            return List.of();
        }
        List<Integer> ids = sighting.shownTag().entities().numbers();
        sighting.markHidden();
        return List.of(new NametagPacket.Destroy(ids));
    }
}
