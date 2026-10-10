// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.ArrayList;
import java.util.List;

public record TagEntities(List<NametagIdentity> bottomFirst) {

    public TagEntities {
        if (bottomFirst.isEmpty()) {
            throw new IllegalArgumentException("a tag needs at least one entity");
        }
        bottomFirst = List.copyOf(bottomFirst);
    }

    public static TagEntities of(NametagIdentity identity) {
        return new TagEntities(List.of(identity));
    }

    public NametagIdentity bottom() {
        return bottomFirst.get(0);
    }

    public NametagIdentity atLevel(int level) {
        return bottomFirst.get(level);
    }

    public int count() {
        return bottomFirst.size();
    }

    public List<Integer> numbers() {
        return bottomFirst.stream().map(NametagIdentity::entityNumber).toList();
    }

    public boolean contains(int entityNumber) {
        return bottomFirst.stream().anyMatch(identity -> identity.entityNumber() == entityNumber);
    }

    public TagEntities lowest(int wanted) {
        return new TagEntities(bottomFirst.subList(0, Math.max(1, Math.min(wanted, bottomFirst.size()))));
    }

    public TagEntities grownTo(int wanted, NametagIdentities allocator) {
        if (wanted <= bottomFirst.size()) {
            return this;
        }
        List<NametagIdentity> grown = new ArrayList<>(bottomFirst);
        while (grown.size() < wanted) {
            grown.add(allocator.allocate());
        }
        return new TagEntities(grown);
    }
}
