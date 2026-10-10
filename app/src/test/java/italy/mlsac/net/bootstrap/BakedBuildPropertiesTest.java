// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BakedBuildPropertiesTest {

    private static final String RESOURCE = "mlsac-build.properties";
    private static final String ABSENT = "mlsac.test.baked.absent";
    private static final String PRESENT = "mlsac.test.baked.present";

    @Test
    void bakedKeysAreAppliedButExistingOnesAreKept() {
        System.clearProperty(ABSENT);
        System.setProperty(PRESENT, "original");
        try {
            BakedBuildProperties.apply(loaderServing(ABSENT + "=0" + System.lineSeparator()
                    + PRESENT + "=0" + System.lineSeparator()));
            assertEquals("0", System.getProperty(ABSENT));
            assertEquals("original", System.getProperty(PRESENT));
        } finally {
            System.clearProperty(ABSENT);
            System.clearProperty(PRESENT);
        }
    }

    @Test
    void withoutTheResourceNothingIsApplied() {
        System.clearProperty(ABSENT);
        BakedBuildProperties.apply(new ClassLoader(getClass().getClassLoader()) {
            @Override
            public InputStream getResourceAsStream(String name) {
                return name.equals(RESOURCE) ? null : super.getResourceAsStream(name);
            }
        });
        assertNull(System.getProperty(ABSENT));
    }

    private ClassLoader loaderServing(String text) {
        return new ClassLoader(getClass().getClassLoader()) {
            @Override
            public InputStream getResourceAsStream(String name) {
                if (name.equals(RESOURCE)) {
                    return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
                }
                return super.getResourceAsStream(name);
            }
        };
    }
}
