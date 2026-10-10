// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;

final class GoldenTraces {
    private GoldenTraces() {
    }

    static Path locate(String scenario) {
        String folder = System.getProperty("mlsac.golden");
        Assumptions.assumeTrue(folder != null, "golden folder not configured");
        String only = System.getProperty("mlsac.only", "");
        Assumptions.assumeTrue(only.isEmpty() || only.equals(scenario), "scenario filtered out");
        Path trace = Path.of(folder, scenario + ".jsonl.gz");
        Assumptions.assumeTrue(Files.exists(trace), "golden trace missing");
        return trace;
    }

    static Path locateLive(String version, String scenario) throws IOException {
        String folder = System.getProperty("mlsac.golden.live");
        Assumptions.assumeTrue(folder != null, "live golden folder not configured");
        String name = liveName(version, scenario);
        String only = System.getProperty("mlsac.only", "");
        Assumptions.assumeTrue(only.isEmpty() || only.equals(name), "scenario filtered out");
        Path run = Path.of(folder, version, scenario);
        Assumptions.assumeTrue(Files.isDirectory(run), "live golden trace missing");
        try (Stream<Path> files = Files.list(run)) {
            List<Path> traces = files.filter(file -> file.getFileName().toString().endsWith(".jsonl.gz")).sorted()
                    .toList();
            Assumptions.assumeTrue(traces.size() == 1, "expected exactly one live trace in " + run);
            return traces.get(0);
        }
    }

    static String liveName(String version, String scenario) {
        return "live_" + version + "_" + scenario;
    }

    static void store(String scenario, ParityReport report) throws IOException {
        String folder = System.getProperty("mlsac.parity.folder");
        if (folder == null) {
            return;
        }
        Path output = Path.of(folder, scenario + ".txt");
        Files.createDirectories(output.getParent());
        Files.writeString(output, report.render(), StandardCharsets.UTF_8);
    }
}
