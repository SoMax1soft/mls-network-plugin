// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import java.util.OptionalInt;

public sealed interface RootVerdict permits RootVerdict.Cleared, RootVerdict.Refused {

    static RootVerdict untargeted(String rootKey) {
        return new Cleared(rootKey, OptionalInt.empty());
    }

    static RootVerdict targetAt(String rootKey, int argument) {
        return new Cleared(rootKey, OptionalInt.of(argument));
    }

    static RootVerdict refused(GuardReason reason, String detail) {
        return new Refused(new GuardRefusal(reason, detail));
    }

    record Cleared(String rootKey, OptionalInt targetArgument) implements RootVerdict {
    }

    record Refused(GuardRefusal refusal) implements RootVerdict {
    }
}
