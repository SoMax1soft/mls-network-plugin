// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.console;

final class RedactedThrowable extends Throwable {

    private static final long serialVersionUID = 1L;

    RedactedThrowable(Throwable original, SecretRedactor redactor) {
        super(original.getClass().getName() + ": " + redactor.redact(String.valueOf(original.getMessage())),
                original.getCause() == null ? null : new RedactedThrowable(original.getCause(), redactor),
                false, true);
        setStackTrace(original.getStackTrace());
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
