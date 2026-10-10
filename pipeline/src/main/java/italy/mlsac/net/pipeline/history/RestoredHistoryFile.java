// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.history;

import italy.mlsac.net.pipeline.environment.DataFolder;
import italy.mlsac.net.pipeline.environment.PlatformText;
import italy.mlsac.net.pipeline.environment.WallClock;
import italy.mlsac.net.pipeline.environment.WallPurpose;
import italy.mlsac.net.pipeline.legacy.LegacyDelta;
import italy.mlsac.net.pipeline.numeric.SixDigitNumberText;
import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;

public final class RestoredHistoryFile {
    private static final String FOLDER = "restored_data/";
    private static final String STAMP_PATTERN = "yyyyMMdd_HHmmss";
    private static final String UNLABELED_CLASS = "0";
    private static final String HEADER =
            "is_cheating,delta_yaw,delta_pitch,accel_yaw,accel_pitch,jerk_yaw,jerk_pitch,gcd_error_yaw,gcd_error_pitch";

    private final DataFolder folder;
    private final PlatformText platform;
    private final WallClock wallClock;

    public RestoredHistoryFile(DataFolder folder, PlatformText platform, WallClock wallClock) {
        this.folder = folder;
        this.platform = platform;
        this.wallClock = wallClock;
    }

    public boolean write(String playerNick, List<LegacyDelta> history) throws IOException {
        if (history.isEmpty()) {
            return false;
        }
        String separator = platform.lineSeparator();
        StringJoiner document = new StringJoiner(separator, "", separator).add(HEADER);
        history.stream().map(RestoredHistoryFile::csvRow).forEach(document::add);
        String fileName = PlayerNameSanitizer.sanitise(playerNick) + "_"
                + wallClock.localStamp(WallPurpose.RESTORE_STAMP, STAMP_PATTERN) + ".csv";
        folder.write(FOLDER + fileName, document.toString().getBytes(platform.defaultCharset()));
        return true;
    }

    private static String csvRow(LegacyDelta delta) {
        StringJoiner row = new StringJoiner(",").add(UNLABELED_CLASS);
        for (float value : delta.values()) {
            row.add(SixDigitNumberText.of(value));
        }
        return row.toString();
    }
}
