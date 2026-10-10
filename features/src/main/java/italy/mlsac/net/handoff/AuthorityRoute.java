// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.handoff;

import italy.mlsac.net.scoring.BufferReactions;
import italy.mlsac.net.scoring.FlagContext;
import italy.mlsac.net.scoring.FlagPenalties;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public final class AuthorityRoute implements FlagPenalties, Supplier<BufferReactions> {

    private final Supplier<PenaltyAuthority> authority;
    private final Map<PenaltyAuthority, FlagPenalties> penalties = new EnumMap<>(PenaltyAuthority.class);
    private final Map<PenaltyAuthority, BufferReactions> reactions = new EnumMap<>(PenaltyAuthority.class);

    public AuthorityRoute(Supplier<PenaltyAuthority> authority, FlagPenalties ladder, BufferReactions legacyResponses,
                          SuppressionEngine engine) {
        this.authority = authority;
        penalties.put(PenaltyAuthority.LEGACY_LADDER, ladder);
        penalties.put(PenaltyAuthority.SUPPRESSION_ENGINE, new SuppressionPenalties(engine));
        reactions.put(PenaltyAuthority.LEGACY_LADDER, legacyResponses);
        reactions.put(PenaltyAuthority.SUPPRESSION_ENGINE, new SuppressionBufferReactions(engine));
    }

    @Override
    public void onFlag(FlagContext flag) {
        penalties.get(authority.get()).onFlag(flag);
    }

    @Override
    public BufferReactions get() {
        return reactions.get(authority.get());
    }
}
