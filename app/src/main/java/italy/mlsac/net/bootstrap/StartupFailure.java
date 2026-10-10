// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

final class StartupFailure extends Exception {

    private static final long serialVersionUID = 1L;

    StartupFailure(String message, Throwable cause) {
        super(message, cause);
    }
}
