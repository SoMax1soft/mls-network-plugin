// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.json;

public final class JsonSyntaxException extends Exception {

    private static final long serialVersionUID = 1L;

    public JsonSyntaxException(String message, int position) {
        super(message + " at offset " + position);
    }
}
