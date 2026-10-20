// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import java.util.Optional;

public record WireSeed(long bits) {

    private static final long JITTER_MASK = 0xD1B54A32D192ED03L;

    public static Optional<WireSeed> parse(String decimal) {
        if (decimal == null || decimal.isEmpty() || !decimal.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new WireSeed(Long.parseUnsignedLong(decimal)));
        } catch (NumberFormatException outOfRange) {
            return Optional.empty();
        }
    }

    public long jitterBits() {
        return bits ^ JITTER_MASK;
    }

    @Override
    public String toString() {
        return Long.toUnsignedString(bits);
    }
}
