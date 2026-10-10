// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.preset;

import italy.mlsac.net.backend.http.HttpReply;
import italy.mlsac.net.backend.session.LenientJson;
import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.json.JsonValue;
import italy.mlsac.net.json.JsonWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class PresetAnswerReader {

    private static final int HASH_HEX_LENGTH = 32;

    public PresetAnswer read(HttpReply reply) {
        int status = reply.status();
        if (status == 401 || status == 403) {
            return new PresetAnswer.Failed("HTTP " + status, true);
        }
        if (status == 404 || status == 405) {
            return new PresetAnswer.Unavailable(MissingReason.NO_ENDPOINT);
        }
        if (!reply.success()) {
            return new PresetAnswer.Failed("HTTP " + status, false);
        }
        Optional<JsonObject> data = LenientJson.object(reply.text()).flatMap(root -> root.object("data"));
        if (data.isEmpty()) {
            return new PresetAnswer.Failed("answer without a data object", false);
        }
        Optional<JsonValue> config = data.get().get("config");
        if (config.isEmpty() || !(config.get() instanceof JsonObject configObject)) {
            return new PresetAnswer.Unavailable(MissingReason.fromBackend(data.get().string("reason").orElse("")));
        }
        String hash = data.get().string("hash").filter(h -> !h.isEmpty()).orElseGet(() -> digest(configObject));
        return new PresetAnswer.Delivered(data.get().string("preset"), hash, toPlainMap(configObject));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toPlainMap(JsonObject config) {
        return new LinkedHashMap<>((Map<String, Object>) config.toPlain());
    }

    static String digest(JsonObject config) {
        try {
            byte[] canonical = JsonWriter.compact().write(config).getBytes(StandardCharsets.UTF_8);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonical);
            return HexFormat.of().formatHex(hash).substring(0, HASH_HEX_LENGTH);
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException("SHA-256 is not available", missing);
        }
    }
}
