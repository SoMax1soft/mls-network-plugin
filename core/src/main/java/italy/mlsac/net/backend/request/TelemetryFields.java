// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.request;

import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.json.JsonObjectBuilder;
import italy.mlsac.net.telemetry.ClientDistribution;
import italy.mlsac.net.telemetry.HostMachine;
import italy.mlsac.net.telemetry.ServerEnvironment;
import italy.mlsac.net.telemetry.ServerStack;
import java.util.Map;

final class TelemetryFields {

    static final String ENVIRONMENT = "environment";
    static final String CLIENT_VERSIONS = "clientVersions";

    private TelemetryFields() {
    }

    static JsonObjectBuilder appendEnvironment(JsonObjectBuilder body, ServerEnvironment environment) {
        if (environment == null || !environment.known()) {
            return body;
        }
        JsonObjectBuilder facts = JsonObject.builder();
        appendStack(facts, environment.stack());
        appendHost(facts, environment.host());
        return body.put(ENVIRONMENT, facts.build());
    }

    static JsonObjectBuilder appendClients(JsonObjectBuilder body, ClientDistribution clients) {
        if (clients == null || clients.empty()) {
            return body;
        }
        JsonObjectBuilder counts = JsonObject.builder();
        if (!clients.protocols().isEmpty()) {
            counts.put("protocols", tally(clients.protocols()));
        }
        if (!clients.brands().isEmpty()) {
            counts.put("brands", tally(clients.brands()));
        }
        return body.put(CLIENT_VERSIONS, counts.build());
    }

    private static void appendStack(JsonObjectBuilder facts, ServerStack stack) {
        stack.name().ifPresent(value -> facts.put("serverCore", value));
        stack.version().ifPresent(value -> facts.put("serverVersion", value));
        stack.apiVersion().ifPresent(value -> facts.put("apiVersion", value));
        stack.software().ifPresent(value -> facts.put("serverSoftware", value));
        stack.gameVersion().ifPresent(value -> facts.put("gameVersion", value));
        stack.compatibility().ifPresent(value -> facts.put("compatibility", value));
        stack.folia().ifPresent(value -> facts.put("folia", value));
    }

    private static void appendHost(JsonObjectBuilder facts, HostMachine host) {
        host.osName().ifPresent(value -> facts.put("osName", value));
        host.osVersion().ifPresent(value -> facts.put("osVersion", value));
        host.osArch().ifPresent(value -> facts.put("osArch", value));
        host.javaVersion().ifPresent(value -> facts.put("javaVersion", value));
        host.javaVendor().ifPresent(value -> facts.put("javaVendor", value));
        host.cpuCores().ifPresent(value -> facts.put("cpuCores", (long) value));
        host.maxHeapMegabytes().ifPresent(value -> facts.put("maxHeapMb", value));
    }

    private static JsonObject tally(Map<String, Integer> counts) {
        JsonObjectBuilder entries = JsonObject.builder();
        counts.forEach((key, value) -> entries.put(key, (long) value));
        return entries.build();
    }
}
