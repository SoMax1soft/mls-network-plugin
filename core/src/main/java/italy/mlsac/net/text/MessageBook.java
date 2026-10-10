// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.text;

import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.config.message.MessageOverrides;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public final class MessageBook {

    public static final String PREFIX_KEY = "prefix";
    private static final String FALLBACK_PREFIX = "&6MLSAC &8» &r";

    private final MessageCatalog catalog;
    private final Supplier<EffectiveConfig> config;
    private final LegacyColorizer colorizer;
    private final List<MessageDefaults> moduleDefaults = new CopyOnWriteArrayList<>();

    public MessageBook(MessageCatalog catalog, Supplier<EffectiveConfig> config, LegacyColorizer colorizer) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.config = Objects.requireNonNull(config, "config");
        this.colorizer = Objects.requireNonNull(colorizer, "colorizer");
    }

    public void registerDefaults(MessageDefaults defaults) {
        moduleDefaults.add(Objects.requireNonNull(defaults, "defaults"));
    }

    public LanguageId messageLanguage() {
        EffectiveConfig current = config.get();
        return current.messages().presetLanguage().orElse(current.core().language());
    }

    public LanguageId serverLanguage() {
        return config.get().core().language();
    }

    public String rawText(String key) {
        MessageOverrides overrides = config.get().messages().overrides();
        LanguageId language = messageLanguage();
        return overrides.text(key)
                .or(() -> catalog.text(language, key))
                .or(() -> moduleDefault(key, language))
                .orElse("&cNo text for '" + key + "'");
    }

    public List<String> rawLines(String key) {
        MessageOverrides overrides = config.get().messages().overrides();
        return overrides.lines(key).or(() -> catalog.lines(messageLanguage(), key)).orElse(List.of());
    }

    public String rawPrefix() {
        MessageOverrides overrides = config.get().messages().overrides();
        return overrides.text(PREFIX_KEY).or(() -> catalog.text(messageLanguage(), PREFIX_KEY)).orElse(FALLBACK_PREFIX);
    }

    public String text(String key, TemplateValues values) {
        return colorizer.colorize(values.apply(rawText(key)));
    }

    public String text(String key) {
        return text(key, TemplateValues.none());
    }

    public String prefixed(String key) {
        return prefixed(key, TemplateValues.none());
    }

    public String prefixed(String key, TemplateValues values) {
        return colorizer.colorize(rawPrefix()) + text(key, values);
    }

    public String prefixedLiteral(String rawText) {
        return colorizer.colorize(rawPrefix()) + colorizer.colorize(rawText);
    }

    public List<String> lines(String key, TemplateValues values) {
        return rawLines(key).stream().map(line -> colorizer.colorize(values.apply(line))).toList();
    }

    public boolean isBlank(String key) {
        return rawText(key).isBlank();
    }

    public String colorize(String raw) {
        return colorizer.colorize(raw);
    }

    public String strip(String raw) {
        return colorizer.strip(raw);
    }

    private Optional<String> moduleDefault(String key, LanguageId language) {
        for (MessageDefaults defaults : moduleDefaults) {
            Optional<String> text = defaults.text(key, language);
            if (text.isPresent()) {
                return text;
            }
        }
        return Optional.empty();
    }
}
