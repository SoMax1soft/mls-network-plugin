// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.http;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;

public final class TransportFailure extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final FailureKind kind;

    public TransportFailure(FailureKind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    public FailureKind kind() {
        return kind;
    }

    public static Optional<TransportFailure> unwrap(Throwable error) {
        Throwable current = error;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current instanceof TransportFailure failure ? Optional.of(failure) : Optional.empty();
    }

    public static TransportFailure of(Throwable error) {
        return unwrap(error).orElseGet(() -> new TransportFailure(FailureKind.OTHER, String.valueOf(error), error));
    }
}
