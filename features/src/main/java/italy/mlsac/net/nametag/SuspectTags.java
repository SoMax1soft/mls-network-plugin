// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SuspectTags {

    private final NametagIdentities identities;
    private final Map<UUID, Tag> bySuspect = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> byEntity = new ConcurrentHashMap<>();

    public SuspectTags(NametagIdentities identities) {
        this.identities = identities;
    }

    public Tag register(UUID suspect, int entityNumber, SuspectMode mode) {
        remove(suspect);
        Tag tag = new Tag(suspect, entityNumber, TagEntities.of(identities.allocate()), Optional.empty(), Long.MIN_VALUE,
                mode);
        bySuspect.put(suspect, tag);
        byEntity.put(entityNumber, suspect);
        return tag;
    }

    public void remove(UUID suspect) {
        Tag old = bySuspect.remove(suspect);
        if (old != null) {
            byEntity.remove(old.entityNumber(), suspect);
        }
    }

    public Optional<Tag> bySuspect(UUID suspect) {
        return Optional.ofNullable(bySuspect.get(suspect));
    }

    public Optional<Tag> byEntity(int entityNumber) {
        UUID suspect = byEntity.get(entityNumber);
        return suspect == null ? Optional.empty() : bySuspect(suspect);
    }

    public List<Tag> all() {
        return List.copyOf(bySuspect.values());
    }

    public boolean updateText(UUID suspect, long historyRevision, Optional<TagLines> text) {
        Tag current = bySuspect.get(suspect);
        if (current == null) {
            return false;
        }
        TagEntities entities = current.entities().grownTo(text.map(TagLines::count).orElse(1), identities);
        bySuspect.put(suspect, new Tag(suspect, current.entityNumber(), entities, text, historyRevision,
                current.mode()));
        return !Objects.equals(current.text(), text);
    }

    public boolean applyMode(UUID suspect, SuspectMode mode) {
        Tag current = bySuspect.get(suspect);
        if (current == null || current.mode() == mode) {
            return false;
        }
        bySuspect.put(suspect, new Tag(suspect, current.entityNumber(), current.entities(), current.text(),
                current.historyRevision(), mode));
        return true;
    }

    public void clear() {
        bySuspect.clear();
        byEntity.clear();
    }

    public record Tag(UUID suspect, int entityNumber, TagEntities entities, Optional<TagLines> text,
                      long historyRevision, SuspectMode mode) {

        public Optional<TagLines> displayedText() {
            return mode.carriesTag() ? text : Optional.empty();
        }
    }
}
