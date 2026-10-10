// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.numeric;

public enum AngleWrap {
    REMAINDER {
        @Override
        public double apply(double angle) {
            double rest = angle % 360.0;
            if (rest > 180.0) {
                return rest - 360.0;
            }
            if (rest < -180.0) {
                return rest + 360.0;
            }
            return rest;
        }
    },
    FLOOR {
        @Override
        public double apply(double angle) {
            return ((angle + 180.0) - 360.0 * Math.floor((angle + 180.0) / 360.0)) - 180.0;
        }
    },
    SHIFTED_REMAINDER {
        @Override
        public double apply(double angle) {
            double shifted = (angle + 180.0) % 360.0;
            if (shifted < 0) {
                shifted += 360.0;
            }
            return shifted - 180.0;
        }
    };

    public abstract double apply(double angle);

    public static float remainderSingle(float angle) {
        float rest = angle % 360.0f;
        if (rest > 180.0f) {
            return rest - 360.0f;
        }
        if (rest < -180.0f) {
            return rest + 360.0f;
        }
        return rest;
    }
}
