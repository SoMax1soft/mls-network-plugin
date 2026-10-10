// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

public enum StatusClass {
    SUCCESS,
    AUTH,
    QUOTA,
    NO_SUCH_ENDPOINT,
    METHOD_NOT_ALLOWED,
    CONFLICT,
    BACKEND_FAULT,
    OTHER;

    public static StatusClass of(int status) {
        if (status >= 200 && status < 300) {
            return SUCCESS;
        }
        if (status >= 500) {
            return BACKEND_FAULT;
        }
        return switch (status) {
            case 401, 403 -> AUTH;
            case 429 -> QUOTA;
            case 404 -> NO_SUCH_ENDPOINT;
            case 405 -> METHOD_NOT_ALLOWED;
            case 409 -> CONFLICT;
            default -> OTHER;
        };
    }

    public boolean unsupportedEndpoint() {
        return this == NO_SUCH_ENDPOINT || this == METHOD_NOT_ALLOWED;
    }
}
