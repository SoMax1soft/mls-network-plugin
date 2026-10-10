// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.telemetry;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Stream;

public record HostMachine(Optional<String> osName, Optional<String> osVersion, Optional<String> osArch,
                          Optional<String> javaVersion, Optional<String> javaVendor, OptionalInt cpuCores,
                          OptionalLong maxHeapMegabytes) {

    public static final HostMachine UNKNOWN = new HostMachine(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), OptionalInt.empty(), OptionalLong.empty());

    private static final int TEXT_LIMIT = 64;
    private static final long MEGABYTE = 1024L * 1024L;

    public HostMachine {
        Stream.of(osName, osVersion, osArch, javaVersion, javaVendor, cpuCores, maxHeapMegabytes)
                .forEach(Objects::requireNonNull);
        osName = trimmed(osName);
        osVersion = trimmed(osVersion);
        osArch = trimmed(osArch);
        javaVersion = trimmed(javaVersion);
        javaVendor = trimmed(javaVendor);
    }

    public static HostMachine read() {
        return new HostMachine(property("os.name"), property("os.version"), property("os.arch"),
                property("java.version"), property("java.vendor"), cores(), heapMegabytes());
    }

    public boolean known() {
        return osName.isPresent() || osVersion.isPresent() || osArch.isPresent() || javaVersion.isPresent()
                || javaVendor.isPresent() || cpuCores.isPresent() || maxHeapMegabytes.isPresent();
    }

    private static Optional<String> property(String name) {
        try {
            return Optional.ofNullable(System.getProperty(name));
        } catch (RuntimeException denied) {
            return Optional.empty();
        }
    }

    private static OptionalInt cores() {
        try {
            int cores = Runtime.getRuntime().availableProcessors();
            return cores > 0 ? OptionalInt.of(cores) : OptionalInt.empty();
        } catch (RuntimeException denied) {
            return OptionalInt.empty();
        }
    }

    private static OptionalLong heapMegabytes() {
        try {
            long max = Runtime.getRuntime().maxMemory();
            if (max <= 0L || max == Long.MAX_VALUE) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(Math.max(1L, max / MEGABYTE));
        } catch (RuntimeException denied) {
            return OptionalLong.empty();
        }
    }

    private static Optional<String> trimmed(Optional<String> value) {
        return value.map(String::trim).filter(text -> !text.isEmpty())
                .map(text -> text.length() <= TEXT_LIMIT ? text : text.substring(0, TEXT_LIMIT));
    }
}
