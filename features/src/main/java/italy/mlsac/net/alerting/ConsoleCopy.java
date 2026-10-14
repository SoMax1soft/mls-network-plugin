// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import italy.mlsac.net.logbook.Logbook;

public enum ConsoleCopy {
    WHEN_CONFIGURED {
        @Override
        boolean wanted(AlertOutput output) {
            return output.console();
        }

        @Override
        void write(Logbook log, String plainLine) {
            log.info(plainLine);
        }
    },
    DEBUG_ONLY {
        @Override
        boolean wanted(AlertOutput output) {
            return true;
        }

        @Override
        void write(Logbook log, String plainLine) {
            log.debug(plainLine);
        }
    },
    ALWAYS {
        @Override
        boolean wanted(AlertOutput output) {
            return true;
        }

        @Override
        void write(Logbook log, String plainLine) {
            log.info(plainLine);
        }
    };

    abstract boolean wanted(AlertOutput output);

    abstract void write(Logbook log, String plainLine);
}
