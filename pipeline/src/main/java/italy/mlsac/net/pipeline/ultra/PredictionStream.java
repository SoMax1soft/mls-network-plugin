// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import italy.mlsac.net.pipeline.json.JsonNumber;
import italy.mlsac.net.pipeline.json.JsonReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public enum PredictionStream {
    ULTRA {
        @Override
        void collect(List<Prediction> predictions, String model, double probability) {
            if (Double.isFinite(probability)) {
                predictions.add(new Prediction(ULTRA_MODEL, probability));
            }
        }
    },
    LEGACY {
        @Override
        void collect(List<Prediction> predictions, String model, double probability) {
            predictions.add(new Prediction(model, probability));
        }
    };

    private static final String ULTRA_MODEL = "ultra";

    abstract void collect(List<Prediction> predictions, String model, double probability);

    public List<Prediction> read(String body) throws PredictionFailure {
        List<Prediction> predictions = new ArrayList<>();
        for (String line : body.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Map<String, Object> object = JsonReader.parseObject(trimmed);
            String type = object.get("type") instanceof String text ? text : "prediction";
            if (type.equals("done")) {
                break;
            }
            if (type.equals("error")) {
                continue;
            }
            if (object.get("error") instanceof String message && !message.isEmpty()) {
                throw new PredictionFailure(message);
            }
            double probability = object.get("probability") instanceof JsonNumber number ? number.asDouble() : 0.0;
            String model = object.get("model") instanceof String name ? name : null;
            collect(predictions, model, probability);
        }
        return predictions;
    }
}
