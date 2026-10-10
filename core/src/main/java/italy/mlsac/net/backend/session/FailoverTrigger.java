// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

public enum FailoverTrigger {
    NETWORK(false),
    NO_SUCH_ENDPOINT(true),
    BACKEND_FAULT(true);

    private final boolean entersErrorStateWhenReserveFails;

    FailoverTrigger(boolean entersErrorStateWhenReserveFails) {
        this.entersErrorStateWhenReserveFails = entersErrorStateWhenReserveFails;
    }

    public boolean entersErrorStateWhenReserveFails() {
        return entersErrorStateWhenReserveFails;
    }
}
