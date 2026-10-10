// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag;

public enum Pursuit {
    DURING_FIGHTS {
        @Override
        public boolean waitsForFight() {
            return true;
        }

        @Override
        public boolean fallsBackWithinReach() {
            return false;
        }
    },
    RELENTLESS {
        @Override
        public boolean waitsForFight() {
            return false;
        }

        @Override
        public boolean fallsBackWithinReach() {
            return true;
        }
    };

    public abstract boolean waitsForFight();

    public abstract boolean fallsBackWithinReach();
}
