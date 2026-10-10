// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

public enum SuspectMode {
    PLAYING,
    SPECTATING;

    public static SuspectMode fromGameMode(String modeOfPlay) {
        return "SPECTATOR".equalsIgnoreCase(modeOfPlay == null ? "" : modeOfPlay.trim()) ? SPECTATING : PLAYING;
    }

    public boolean carriesTag() {
        return this == PLAYING;
    }
}
