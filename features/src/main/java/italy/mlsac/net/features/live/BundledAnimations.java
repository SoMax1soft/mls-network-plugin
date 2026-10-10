// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.banimation.AnimationFile;
import italy.mlsac.net.resource.ResourceSource;
import italy.mlsac.net.yaml.YamlFormatException;
import italy.mlsac.net.yaml.YamlParser;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class BundledAnimations implements Supplier<List<AnimationFile>> {

    private static final String FOLDER = "animations/";
    private static final List<String> SHIPPED = List.of("classic_ban.yml", "dramatic_ban.yml", "rainbow_ban.yml");

    private final ResourceSource resources;
    private final YamlParser yaml;

    public BundledAnimations(ResourceSource resources, YamlParser yaml) {
        this.resources = resources;
        this.yaml = yaml;
    }

    @Override
    public List<AnimationFile> get() {
        return SHIPPED.stream().map(name -> new AnimationFile(name, () -> document(name))).toList();
    }

    private Map<?, ?> document(String name) {
        String text = resources.text(FOLDER + name)
                .orElseThrow(() -> new IllegalStateException("Bundled animation " + name + " is missing"));
        try {
            return yaml.parse(text);
        } catch (YamlFormatException invalid) {
            throw new IllegalStateException("Bundled animation " + name + " is unreadable: " + invalid.getMessage(),
                    invalid);
        }
    }
}
