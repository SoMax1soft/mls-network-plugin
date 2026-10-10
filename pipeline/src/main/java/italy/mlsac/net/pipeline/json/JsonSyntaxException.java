// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.json;

public final class JsonSyntaxException extends RuntimeException {
    public JsonSyntaxException(String message, int position) {
        super(message + " at " + position);
    }
}
