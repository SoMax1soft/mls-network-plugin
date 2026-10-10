// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.numeric;

import java.util.Locale;

public final class SixDigitNumberText {
    private SixDigitNumberText() {
    }

    public static String of(float value) {
        return String.format(Locale.ROOT, "%.6f", (double) value);
    }
}
