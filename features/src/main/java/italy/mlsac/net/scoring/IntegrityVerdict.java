// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.Locale;

public final class IntegrityVerdict {

    public static final String MODEL = "integrity";
    public static final String DISPLAY_NAME = "Bad packets";
    public static final double PROBABILITY = 1.0;

    private IntegrityVerdict() {
    }

    public static Verdict strike() {
        return new Verdict(MODEL, PROBABILITY);
    }

    public static boolean concerns(String model) {
        return model != null && MODEL.equals(model.trim().toLowerCase(Locale.ROOT));
    }
}
