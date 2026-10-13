// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.tables;

public interface RowFields {

    RowFields integer(long value);

    RowFields number(double value);

    RowFields single(float value);

    RowFields flag(boolean value);

    RowFields text(String value);
}
