// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.detection.RegionEntry;
import italy.mlsac.net.config.hologram.AvgColorBand;
import italy.mlsac.net.config.hologram.Billboard;
import italy.mlsac.net.config.hologram.TagPlacement;
import italy.mlsac.net.config.response.TrickKind;
import italy.mlsac.net.config.suppression.BufferRule;
import italy.mlsac.net.config.suppression.CommandAction;
import italy.mlsac.net.config.suppression.DamageDealtAction;
import italy.mlsac.net.config.suppression.LagAction;
import italy.mlsac.net.config.suppression.LagEffect;
import italy.mlsac.net.config.suppression.LagLevel;
import italy.mlsac.net.config.suppression.SuppressionSettings;
import italy.mlsac.net.config.suppression.SuppressionStep;
import italy.mlsac.net.config.suppression.TrickAction;
import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.json.JsonParser;
import italy.mlsac.net.json.JsonSyntaxException;
import italy.mlsac.net.testkit.TestConfigs;
import italy.mlsac.net.text.LanguageId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PresetDocumentTest {

    static final String FORMAT_TWO_EXAMPLE = """
            {"alert-responses":{"alerts":{"buffer-step-percent":0.33},"damage-reduction":{"enabled":true,"stages":[{"buffer":20,"duration-seconds":8,"enabled":true,"reduction-percent":20}]},"enabled":true,"troll":{"actions":[{"buffer":20,"cooldown-seconds":20,"enabled":true,"horizontal-velocity":1.4,"message":"&#F1F1F1▍ &#D8D8D8M&#BFBFBFL&#A6A6A6S&#8D8D8DA&#747474C &f{PLAYER} &7Forced to shuffle his inventory at &#EE1955(&#EE1955B&#E81750u&#E2154Bf&#DC1245f&#D61040e&#CF0E3Br: &f{BUFFER}&#EE1955)&f","only-sword":true,"type":"shuffle_inventory","vertical-velocity":0.45}],"enabled":true}},
             "alerts":{"console":false,"sound":{"enabled":true,"pitch":1,"type":"BLOCK_NOTE_BLOCK_PLING","volume":1},"threshold":0.8},
             "detection":{"models":{"experimental":{"enabled":false,"name":"Experimental","only-alert":false},"fast":{"enabled":false,"name":"Mini","only-alert":false},"pro":{"enabled":false,"name":"Pro","only-alert":false},"pulse":{"enabled":false,"name":"Pulse","only-alert":false,"sensitivity":"normal"},"ultra":{"enabled":true,"name":"Ultra","only-alert":false},"ultra80":{"enabled":false,"name":"Ultra80","only-alert":true}},"worldguard":{"disabled-regions":["spawn","world_nether:*","world:pvp-arena"],"enabled":true}},
             "general":{"holograms":{"avg-colors":[{"bold":false,"color":"f","from":0},{"bold":false,"color":"c","from":60},{"bold":true,"color":"4","from":90}],"display":{"background":true,"background-color":"#000000","background-opacity":25,"billboard":"center","scale":1,"see-through":false,"shadow":true},"enabled":true,"fast-format":"&2F&f{RESULT}%","format":"&f&lAVG: {AVG}% &7| {HIST}","history-limit":5,"pro-format":"&6P&f{RESULT}%","pulse-format":"&bPu&f{RESULT}%","show-empty":false,"ultra-format":"&cU&f{RESULT}%","view-distance":30},"modules":{"anti-esp":false,"detection":true,"menus":true,"updates":true},"return-to-free":false,"safe-name-check":false},
             "messages":{"prefix":"&6[AC] &r"},"messages-language":"en",
             "penalties":{"actions":{"1":["kick {PLAYER} MLSAC (VL:{VL})"],"2":["{ANIMATION} ban {PLAYER} Cheating"]},"animation":{"enabled":true,"type":"classic_ban"}},
             "suppression":{"buffer":[{"actions":[{"percent":20,"seconds":8,"stage":0,"type":"damage_dealt"},{"stage":0,"type":"shuffle_inventory"}],"cooldown-seconds":20,"enabled":true,"percent":50}],"repeat-last":true,"steps":[{"actions":[{"percent":30,"seconds":10,"stage":0,"type":"damage_dealt"},{"animation":false,"command":"kick {PLAYER} MLSAC (VL:{VL})","preset":"kick","stage":0,"type":"command"}],"flag":1},{"actions":[{"effects":["delay","miss"],"level":"medium","seconds":15,"stage":0,"type":"lag"},{"animation":true,"command":"ban {PLAYER} Cheating","preset":"ban","stage":1,"type":"command"}],"flag":2}]},
             "violation":{"decay":{"amount":1.5,"threshold":0.5},"multiplier":45,"reset-value":20,"threshold":40,"vl-decay":{"amount":1,"enabled":false,"interval":1500}}}
            """;

    static final String FORMAT_ONE_EXAMPLE = """
            {"alert-responses":{"alerts":{"buffer-step-percent":0.25},"damage-reduction":{"enabled":true,"stages":[{"buffer":15,"duration-seconds":8,"reduction-percent":15}]},"enabled":true,"troll":{"actions":[{"buffer":30,"cooldown-seconds":10,"type":"LAUNCH"}],"enabled":true}},
             "alerts":{"console":true,"sound":{"enabled":false,"pitch":3,"type":"bad sound!","volume":20},"threshold":1.5},
             "detection":{"models":{"fast":{"enabled":true,"name":"Mini","only-alert":true},"pro":{"enabled":true,"name":"Pro","only-alert":false},"ultra":{"enabled":false,"name":"Ultra","only-alert":false}},"worldguard":{"disabled-regions":["World:Region:Sub","bad region!"],"enabled":true}},
             "penalties":{"actions":{"1":"kick {PLAYER} x","3":"{BAN} ban {PLAYER} y","abc":"say no"},"animation":{"enabled":false,"type":"dramatic_ban"}},
             "violation":{"decay":{"amount":2,"threshold":0.4},"multiplier":50,"reset-value":25,"threshold":20,"vl-decay":{"amount":2,"enabled":true,"interval":60.9}}}
            """;

    private final ConfigAssembler assembler = new ConfigAssembler();
    private final PresetLayout layout = new PresetLayout();

    @Test
    void formatTwoExampleFillsEveryTypedSection() throws JsonSyntaxException {
        EffectiveConfig config = apply(FORMAT_TWO_EXAMPLE);

        assertEquals(Set.of("fast", "pro", "ultra", "experimental", "pulse"),
                config.detection().models().models().keySet());
        assertTrue(config.detection().ultraMode());
        assertEquals("Mini", config.detection().models().displayName("fast"));
        assertFalse(config.detection().models().enabled("pulse"));
        assertTrue(config.detection().models().enabled("some-new-model"));
        assertTrue(config.detection().regions().enabled());
        assertEquals(List.of(new RegionEntry.RegionAnywhere("spawn"), new RegionEntry.WholeWorld("world_nether"),
                new RegionEntry.RegionInWorld("world", "pvp-arena")), config.detection().regions().entries());

        assertEquals(0.8, config.alerts().threshold());
        assertEquals(40.0, config.violation().threshold());
        assertEquals(20.0, config.violation().resetValue());
        assertEquals(45.0, config.violation().multiplier());
        assertEquals(1500, config.violation().vlDecay().everySeconds());

        assertEquals(List.of("kick {PLAYER} MLSAC (VL:{VL})"), config.penalties().ladder().rungFor(1).orElseThrow());
        assertEquals(0.33, config.alertResponses().stepPercent());
        assertEquals(1, config.alertResponses().damageStages().size());
        assertEquals(TrickKind.SHUFFLE_INVENTORY, config.alertResponses().prankRules().get(0).kind());
        assertTrue(config.alertResponses().trickParameters(TrickKind.SHUFFLE_INVENTORY).orElseThrow().bladeRequired());

        SuppressionSettings suppression = config.suppression().orElseThrow();
        assertTrue(config.suppressionEngineActive());
        assertTrue(suppression.repeatLast());
        assertFalse(suppression.bufferEnabled());
        assertEquals(2, suppression.steps().size());
        SuppressionStep first = suppression.steps().get(0);
        assertEquals(new DamageDealtAction(0, 30, 10), first.actions().get(0));
        assertEquals("kick {PLAYER} MLSAC (VL:{VL})", ((CommandAction) first.actions().get(1)).ladderText());
        SuppressionStep second = suppression.steps().get(1);
        assertEquals(new LagAction(0, LagLevel.MEDIUM, List.of(LagEffect.DELAY, LagEffect.MISS), 15),
                second.actions().get(0));
        assertEquals("{ANIMATION} ban {PLAYER} Cheating", ((CommandAction) second.actions().get(1)).ladderText());
        BufferRule rule = suppression.bufferRules().get(0);
        assertEquals(50, rule.percent());
        assertEquals(20, rule.restSeconds());
        assertEquals(new TrickAction(0, TrickKind.SHUFFLE_INVENTORY), rule.actions().get(1));
        assertEquals(20.0, rule.threshold(config.violation().suppressionCapacity()));

        assertTrue(config.modules().menus());
        assertTrue(config.modules().updates());
        assertFalse(config.penalties().nickGuard());
        assertEquals(List.of(new AvgColorBand(0, "f", false), new AvgColorBand(60, "c", false),
                new AvgColorBand(90, "4", true)), config.holograms().bands());
        assertEquals(Billboard.CENTER, config.holograms().display().billboard());
        assertEquals(Optional.of("&6[AC] &r"), config.messages().overrides().text("prefix"));
        assertEquals(Optional.of(LanguageId.EN), config.messages().presetLanguage());
        assertTrue(config.preset().applied());
        assertEquals("690d9ffd19c517be8e20b889ad289395", config.preset().hash());
    }

    @Test
    void formatOneIsParsedWithClampsDropsAndRewrites() throws JsonSyntaxException {
        EffectiveConfig config = apply(FORMAT_ONE_EXAMPLE);

        assertFalse(config.detection().ultraMode());
        assertFalse(config.detection().models().onlyAlert("fast"));
        assertEquals(Set.of("fast", "pro", "ultra"), config.detection().models().models().keySet());
        assertEquals(List.of(new RegionEntry.RegionInWorld("world", "region:sub")),
                config.detection().regions().entries());
        assertEquals(1.0, config.alerts().threshold());
        assertEquals(10.0f, config.alerts().sound().volume());
        assertEquals(2.0f, config.alerts().sound().pitch());
        assertEquals("BLOCK_NOTE_BLOCK_PLING", config.alerts().sound().type());
        assertEquals(20.0, config.violation().threshold());
        assertEquals(10.0, config.violation().resetValue());
        assertEquals(60, config.violation().vlDecay().everySeconds());
        assertEquals(Set.of(1, 3), config.penalties().ladder().rungs().keySet());
        assertEquals(List.of("{ANIMATION} ban {PLAYER} y"), config.penalties().ladder().rungFor(7).orElseThrow());
        assertEquals(List.of("kick {PLAYER} x"), config.penalties().ladder().rungFor(2).orElseThrow());
        assertTrue(config.alertResponses().damageStages().get(0).enabled());
        assertEquals(TrickKind.LAUNCH, config.alertResponses().prankRules().get(0).kind());
        assertTrue(config.suppression().isEmpty());
        assertFalse(config.penalties().banShowOn());
        assertEquals("dramatic_ban", config.penalties().banShowStyle());
    }

    @Test
    void resetValueAtOrAboveThresholdIsHalvedOnlyForPresets() {
        EffectiveConfig preset = applyMap(Map.of("violation", Map.of("threshold", 20, "reset-value", 25)));
        assertEquals(10.0, preset.violation().resetValue());
        EffectiveConfig local = assembler.assemble(TestConfigs.core(),
                TestConfigs.yaml("violation:\n  threshold: 20\n  reset-value: 25\n"), Optional.empty()).config();
        assertEquals(25.0, local.violation().resetValue());
    }

    @Test
    void presetReplacesModelsAndLadderWholesale() {
        EffectiveConfig config = applyMap(Map.of("detection", Map.of("models", Map.of("pro", Map.of("enabled", true))),
                "penalties", Map.of("actions", Map.of("5", List.of("say five")))));
        assertEquals(Set.of("pro"), config.detection().models().models().keySet());
        assertFalse(config.detection().ultraMode());
        assertEquals(Set.of(5), config.penalties().ladder().rungs().keySet());
    }

    @Test
    void retiredUltra80ModelKeyIsIgnoredWithoutIssues() {
        Map<String, Object> models = Map.of(
                "ultra", Map.of("enabled", true, "name", "Ultra", "only-alert", false),
                "ultra80", Map.of("enabled", false, "name", "Ultra80", "only-alert", true));
        PresetSnapshot snapshot = new PresetSnapshot(Optional.empty(), "x",
                layout.toEffective(Map.of("detection", Map.of("models", models))));
        ConfigAssembler.AssembledConfig assembled = assembler.assemble(TestConfigs.core(), ConfigTree.empty(),
                Optional.of(snapshot));

        assertEquals(List.of(), assembled.issues());
        assertEquals(Set.of("ultra"), assembled.config().detection().models().models().keySet());
        assertTrue(assembled.config().detection().ultraMode());
        assertEquals("Ultra", assembled.config().detection().models().displayName("ultra"));
        assertTrue(assembled.config().detection().models().enabled("ultra80"));
        assertFalse(assembled.config().detection().models().onlyAlert("ultra80"));
        assertEquals(0, assembler.countUsableValues(layout.toEffective(
                Map.of("detection", Map.of("models", Map.of("ultra80", models.get("ultra80")))))));
    }

    @Test
    void presetWithOnlyInvalidValuesHasNothingUsable() {
        ConfigTree tree = layout.toEffective(Map.of("alerts", Map.of("threshold", "high", "console", "yes"),
                "violation", "nope", "general", Map.of("modules", Map.of("detection", 1))));
        assertEquals(0, assembler.countUsableValues(tree));
    }

    @Test
    void hologramBandsMustAscendAndOpacityMapsToAlpha() {
        EffectiveConfig config = applyMap(Map.of("general", Map.of("holograms", Map.of(
                "avg-colors", List.of(Map.of("from", 0, "color", "f"), Map.of("from", 60, "color", "c"),
                        Map.of("from", 50, "color", "#FF0000")),
                "display", Map.of("background-opacity", 100, "background-color", "#102030", "billboard", "sideways")))));
        assertEquals(2, config.holograms().bands().size());
        assertEquals(0xFF102030, config.holograms().display().plateColour());
        assertEquals(Billboard.CENTER, config.holograms().display().billboard());
    }

    @Test
    void hologramLinesHeightAndOwnTagAreRead() {
        EffectiveConfig config = applyMap(Map.of("general", Map.of("holograms", Map.of(
                "format", "&f{AVG}",
                "lines", List.of("&bAI:\u0007", 5, "&f&lAVG: {AVG}% &7| {HIST}", "c", "d", "e"),
                "height", 7.5,
                "show-self", true))));
        assertEquals(List.of("&bAI:", "&f&lAVG: {AVG}% &7| {HIST}", "c", "d"), config.holograms().formats().lines());
        assertEquals(new TagPlacement(3.0, true), config.holograms().placement());
    }

    @Test
    void hologramsWithoutLinesKeepTheSingleFormatLine() {
        EffectiveConfig config = applyMap(Map.of("general", Map.of("holograms", Map.of(
                "format", "&f{AVG}", "lines", List.of(), "height", -4))));
        assertEquals(List.of("&f{AVG}"), config.holograms().formats().lines());
        assertEquals(new TagPlacement(-1.0, false), config.holograms().placement());
        EffectiveConfig older = applyMap(Map.of("general", Map.of("holograms", Map.of("enabled", true))));
        assertEquals(List.of("&f&lAVG: {AVG}% &7| {HIST}"), older.holograms().formats().lines());
        assertEquals(new TagPlacement(0.0, false), older.holograms().placement());
    }

    @Test
    void generalSectionIsRelocatedToEffectivePaths() {
        EffectiveConfig config = applyMap(Map.of("general", Map.of("safe-name-check", true,
                "modules", Map.of("detection", false, "menus", false, "updates", false, "anti-esp", true))));
        assertFalse(config.detection().enabled());
        assertFalse(config.modules().menus());
        assertFalse(config.modules().updates());
        assertTrue(config.penalties().nickGuard());
    }

    private EffectiveConfig apply(String json) throws JsonSyntaxException {
        JsonObject parsed = JsonParser.parseObject(json);
        @SuppressWarnings("unchecked")
        Map<String, Object> plain = (Map<String, Object>) parsed.toPlain();
        PresetSnapshot snapshot = new PresetSnapshot(Optional.of("pvp"), "690d9ffd19c517be8e20b889ad289395",
                layout.toEffective(plain));
        return assembler.assemble(TestConfigs.core(), ConfigTree.empty(), Optional.of(snapshot)).config();
    }

    private EffectiveConfig applyMap(Map<String, Object> preset) {
        PresetSnapshot snapshot = new PresetSnapshot(Optional.empty(), "x", layout.toEffective(preset));
        return assembler.assemble(TestConfigs.core(), ConfigTree.empty(), Optional.of(snapshot)).config();
    }
}
