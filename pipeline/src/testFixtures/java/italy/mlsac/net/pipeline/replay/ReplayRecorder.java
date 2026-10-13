// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import italy.mlsac.net.pipeline.engine.InputTapSource;
import java.util.UUID;

interface ReplayRecorder {

    String DATASET_FOLDER = "data/";
    String RETIRED_HISTORY_FOLDER = "restored_data/";
    String STOP_ALL = "cmd.record_stop_all";

    ReplayRecorder ABSENT = new ReplayRecorder() {
        @Override
        public InputTapSource taps() {
            return InputTapSource.NONE;
        }

        @Override
        public void start(UUID player, String label, String comment) {
        }

        @Override
        public void stop(UUID player) {
        }

        @Override
        public void stopAll() {
        }

        @Override
        public boolean writesDatasets() {
            return false;
        }

        @Override
        public boolean replays(TraceEvent event) {
            return !STOP_ALL.equals(event.name());
        }
    };

    InputTapSource taps();

    void start(UUID player, String label, String comment);

    void stop(UUID player);

    void stopAll();

    boolean writesDatasets();

    default boolean replays(TraceEvent event) {
        return true;
    }

    default boolean expects(TraceRecord output) {
        if (!"file".equals(output.name())) {
            return true;
        }
        String path = output.text("path");
        return !path.startsWith(RETIRED_HISTORY_FOLDER) && (writesDatasets() || !path.startsWith(DATASET_FOLDER));
    }
}
