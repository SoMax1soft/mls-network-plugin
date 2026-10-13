// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

public final class JointTapSource implements InputTapSource {
    private final InputTapSource first;
    private final InputTapSource second;

    public JointTapSource(InputTapSource first, InputTapSource second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public InputTaps open(TapParts parts) {
        return new JointTaps(first.open(parts), second.open(parts));
    }
}
