// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.engine;

@FunctionalInterface
public interface InputTapSource {

    InputTapSource NONE = parts -> InputTaps.NONE;

    InputTaps open(TapParts parts);
}
