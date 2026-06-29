package wtf.mlsac.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSupportTest {

    private static JsonObject obj(String json) {
        return new JsonParser().parse(json).getAsJsonObject();
    }

    @Test
    void getStringReadsPresentValueAndFallsBack() {
        assertEquals("hi", JsonSupport.getString(obj("{\"k\":\"hi\"}"), "k", "fb"));
        assertEquals("fb", JsonSupport.getString(obj("{}"), "k", "fb"));
        assertEquals("fb", JsonSupport.getString(obj("{\"k\":null}"), "k", "fb"));
    }

    @Test
    void getDoubleReadsPresentValueAndFallsBack() {
        assertEquals(1.5, JsonSupport.getDouble(obj("{\"k\":1.5}"), "k", -1.0), 1e-9);
        assertEquals(-1.0, JsonSupport.getDouble(obj("{}"), "k", -1.0), 1e-9);
        assertEquals(-1.0, JsonSupport.getDouble(obj("{\"k\":null}"), "k", -1.0), 1e-9);
    }

    @Test
    void parseReadsProbabilityAndModel() {
        AIResponse response = JsonSupport.parsePredictResponse("{\"probability\":0.9,\"model\":\"v2\"}");
        assertEquals(0.9, response.getProbability(), 1e-9);
        assertEquals("v2", response.getModel());
        assertNull(response.getError());
    }

    @Test
    void parseDefaultsWhenFieldsMissing() {
        AIResponse response = JsonSupport.parsePredictResponse("{}");
        assertEquals(0.0, response.getProbability(), 1e-9);
        // AIResponse.getModel() coalesces a null model to "unknown".
        assertEquals("unknown", response.getModel());
        assertNull(response.getError());
    }

    @Test
    void parseThrowsOnErrorField() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> JsonSupport.parsePredictResponse("{\"error\":\"INVALID_SEQUENCE:40\"}"));
        assertTrue(ex.getMessage().contains("INVALID_SEQUENCE:40"));
    }

    @Test
    void parseThrowsOnMalformedJson() {
        assertThrows(RuntimeException.class, () -> JsonSupport.parsePredictResponse("not json"));
    }
}
