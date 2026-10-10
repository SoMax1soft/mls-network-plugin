// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

final class BakedBuildProperties {

    private static final String RESOURCE = "mlsac-build.properties";

    private BakedBuildProperties() {
    }

    static void apply(ClassLoader loader) {
        Properties baked = new Properties();
        try (InputStream in = loader.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return;
            }
            baked.load(in);
        } catch (IOException failure) {
            return;
        }
        for (String key : baked.stringPropertyNames()) {
            if (System.getProperty(key) == null) {
                System.setProperty(key, baked.getProperty(key));
            }
        }
    }
}
