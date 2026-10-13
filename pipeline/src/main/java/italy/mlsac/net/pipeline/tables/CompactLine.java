// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.tables;

import italy.mlsac.net.pipeline.numeric.CompactNumberText;
import java.util.ArrayList;
import java.util.List;

public final class CompactLine implements RowFields {
    private final List<String> fields = new ArrayList<>(90);

    @Override
    public CompactLine number(double value) {
        fields.add(CompactNumberText.of(value));
        return this;
    }

    @Override
    public CompactLine single(float value) {
        fields.add(CompactNumberText.of(value));
        return this;
    }

    @Override
    public CompactLine integer(long value) {
        fields.add(CompactNumberText.of(value));
        return this;
    }

    @Override
    public CompactLine flag(boolean value) {
        fields.add(CompactNumberText.of(value));
        return this;
    }

    @Override
    public CompactLine text(String value) {
        fields.add(value);
        return this;
    }

    public List<String> fields() {
        return fields;
    }
}
