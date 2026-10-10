// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.BackendVerdict;
import italy.mlsac.net.backend.VerdictListener;
import italy.mlsac.net.json.JsonNumber;
import italy.mlsac.net.json.JsonObject;
import java.util.Locale;
import java.util.Optional;

public final class PredictStreamReader {

    private final ReplyInspector replies;

    public PredictStreamReader(ReplyInspector replies) {
        this.replies = replies;
    }

    public Result read(String body, VerdictListener listener) {
        int delivered = 0;
        for (String rawLine : body.split("\n", -1)) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            Optional<JsonObject> parsed = LenientJson.object(line);
            if (parsed.isEmpty()) {
                return Result.failed(delivered, "invalid response line");
            }
            JsonObject object = parsed.get();
            replies.warnings(object);
            String type = object.string("type").orElse("prediction").toLowerCase(Locale.ROOT);
            if (type.equals("done")) {
                return Result.completed(delivered);
            }
            if (type.equals("error")) {
                continue;
            }
            Optional<String> error = object.string("error").filter(text -> !text.isEmpty());
            if (error.isPresent()) {
                return Result.failed(delivered, "prediction error: " + error.get());
            }
            double probability = object.number("probability").map(JsonNumber::doubleValue).orElse(0.0);
            String model = object.string("model").orElse("unknown");
            listener.onVerdict(new BackendVerdict(model, probability));
            delivered++;
        }
        return Result.completed(delivered);
    }

    public record Result(int delivered, Optional<String> failure) {

        static Result completed(int delivered) {
            return new Result(delivered, Optional.empty());
        }

        static Result failed(int delivered, String reason) {
            return new Result(delivered, Optional.of(reason));
        }
    }
}
