// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.backend.dossier.DossierQuery;
import italy.mlsac.net.backend.dossier.DossierReply;
import italy.mlsac.net.config.staff.DossierSettings;
import italy.mlsac.net.dossier.DossierDesk;
import italy.mlsac.net.dossier.DossierTimes;
import italy.mlsac.net.backend.session.LenientJson;
import italy.mlsac.net.testkit.BundledResources;
import italy.mlsac.net.testkit.RecordingLog;
import italy.mlsac.net.text.LanguageId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class DossierMenuTest implements MenuCanvas {

    private static final long NOW = 1_760_200_000_000L;
    private static final String KICKS = "{\"page\":0,\"size\":45,\"days\":7,\"total\":2,\"pages\":2,\"hasMore\":true,"
            + "\"entries\":[{\"player\":\"Steve\",\"t\":" + (NOW - 300_000) + ",\"action\":\"animation\","
            + "\"model\":\"pro\",\"p\":0.95,\"kicks\":2,\"bans\":1},{\"player\":\"Alex\",\"t\":" + (NOW - 7_200_000)
            + ",\"action\":\"kick\",\"model\":\"\",\"p\":0.0,\"kicks\":1,\"bans\":0}]}";
    private static final String HISTORY = "{\"player\":\"Steve\",\"page\":0,\"size\":45,\"total\":3,\"pages\":1,"
            + "\"hasMore\":false,\"entries\":[{\"kind\":\"punish\",\"t\":" + NOW + ",\"action\":\"kick\","
            + "\"model\":\"pro\",\"p\":0.95},{\"kind\":\"suppression\",\"t\":" + NOW + ",\"trigger\":\"stage\","
            + "\"level\":1,\"buffer\":30.1,\"model\":\"pro\",\"p\":0.92,\"actions\":[\"lag medium 15s\"]},"
            + "{\"kind\":\"detection\",\"t\":" + NOW + ",\"action\":\"alert\",\"model\":\"pro\",\"p\":0.91}]}";
    private static final String PROFILE = "{\"player\":\"Steve\",\"known\":true,\"firstSeen\":" + (NOW - 86_400_000L * 3)
            + ",\"lastSeen\":" + NOW + ",\"detections\":{\"total\":42,\"day\":3,\"week\":12,\"max\":0.973,"
            + "\"avg\":0.884,\"models\":[{\"model\":\"pro\",\"count\":30,\"max\":0.973}]},\"punishments\":{\"total\":3,"
            + "\"kicks\":2,\"bans\":1,\"last\":{\"t\":" + NOW + ",\"action\":\"kick\"}},\"suppressions\":{\"total\":5,"
            + "\"latest\":[{\"t\":" + NOW + ",\"trigger\":\"buffer\",\"level\":50,\"actions\":[\"damage 25%\"]}]},"
            + "\"latest\":[{\"t\":" + NOW + ",\"model\":\"pro\",\"p\":0.934},{\"t\":" + NOW
            + ",\"model\":\"pulse\",\"p\":0.45}]}";

    private final Set<String> knownMaterials = new HashSet<>(Set.of("ARROW", "COMPASS", "BARRIER", "PAPER",
            "BLACK_STAINED_GLASS_PANE", "PLAYER_HEAD", "SUNFLOWER", "RED_STAINED_GLASS_PANE",
            "ORANGE_STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE", "LIME_STAINED_GLASS_PANE",
            "GREEN_STAINED_GLASS_PANE", "IRON_SWORD", "SHIELD"));
    private final FramePainter painter = new FramePainter(BundledResources.colors(), knownMaterials::contains);
    private final BlueprintSource english = new BlueprintSource(BundledResources.menus(), () -> LanguageId.EN);
    private final List<String> events = new ArrayList<>();
    private final Map<UUID, MenuFrame> screens = new HashMap<>();
    private final List<String> asked = new ArrayList<>();
    private final Map<String, String> answers = new HashMap<>(Map.of("kicks", KICKS, "history", HISTORY,
            "profile", PROFILE));
    private final UUID viewer = UUID.randomUUID();
    private DossierReply refusal;
    private CompletableFuture<DossierReply> pending;
    private DossierSettings settings = DossierSettings.pluginDefaults();
    private long now = NOW;
    private final MenuDesk desk = new MenuDesk(english, MenuLayouts::shipped, painter, this,
            BundledResources.english(), new RecordingLog(), new DossierDesk(this::answer, () -> now),
            () -> settings, new DossierTimes(() -> now, ZoneOffset.UTC));

    private CompletableFuture<DossierReply> answer(DossierQuery query) {
        asked.add(query.path());
        if (pending != null) {
            return pending;
        }
        if (refusal != null) {
            return CompletableFuture.completedFuture(refusal);
        }
        String kind = query.path().substring("plugin/dossier/".length(), query.path().indexOf('?'));
        return CompletableFuture.completedFuture(new DossierReply.Found(LenientJson.object(answers.get(kind))
                .orElseThrow()));
    }

    private MenuFrame screen() {
        return screens.get(viewer);
    }

    @Test
    void suspectsButtonOpensTheKicksPageWithThePresetQuery() {
        desk.openSuspects(viewer, "Mod");
        assertEquals("IRON_SWORD", screen().at(53).orElseThrow().material());
        desk.clicked(viewer, "Mod", 53, ClickKind.LEFT);
        assertEquals(List.of("plugin/dossier/kicks?page=0&size=45&days=7&bans=1&scope=server"), asked);
        ItemFace steve = screen().at(0).orElseThrow();
        assertEquals("PLAYER_HEAD", steve.material());
        assertTrue(steve.name().contains("Steve") && steve.name().contains("ban"), steve.name());
        assertTrue(String.join("|", steve.lore()).contains("5m"), steve.lore().toString());
        assertTrue(String.join("|", screen().at(1).orElseThrow().lore()).contains("-"));
        assertTrue(screen().at(50).orElseThrow().name().contains("(2)"));
        assertEquals("BLACK_STAINED_GLASS_PANE", screen().at(48).orElseThrow().material());
        desk.clicked(viewer, "Mod", 50, ClickKind.LEFT);
        assertEquals("plugin/dossier/kicks?page=1&size=45&days=7&bans=1&scope=server", asked.get(1));
    }

    @Test
    void kickEntryOpensHistoryAndBackReturnsFromTheCache() {
        desk.openKicks(viewer, "Mod", 0);
        desk.clicked(viewer, "Mod", 0, ClickKind.LEFT);
        assertEquals("plugin/dossier/history?player=Steve&page=0&size=45&kinds=punish%2Csuppression&scope=server",
                asked.get(1));
        assertEquals("IRON_SWORD", screen().at(0).orElseThrow().material());
        assertEquals("SHIELD", screen().at(1).orElseThrow().material());
        assertTrue(String.join("|", screen().at(1).orElseThrow().lore()).contains("lag medium 15s"));
        assertEquals("RED_STAINED_GLASS_PANE", screen().at(2).orElseThrow().material());
        desk.clicked(viewer, "Mod", 45, ClickKind.LEFT);
        assertEquals(2, asked.size());
        assertTrue(screen().at(0).orElseThrow().name().contains("Steve"));
        now += 46_000;
        desk.openKicks(viewer, "Mod", 0);
        assertEquals(3, asked.size());
    }

    @Test
    void profileShowsPanesHeaderAndExtras() {
        desk.openProfile(viewer, "Mod", "Steve");
        assertEquals("plugin/dossier/profile?player=Steve&scope=server&latest=14", asked.get(0));
        assertEquals("RED_STAINED_GLASS_PANE", screen().at(9).orElseThrow().material());
        assertEquals("LIME_STAINED_GLASS_PANE", screen().at(10).orElseThrow().material());
        ItemFace header = screen().at(4).orElseThrow();
        assertEquals("PLAYER_HEAD", header.material());
        String lore = String.join("|", header.lore());
        assertTrue(lore.contains("42") && lore.contains("pro 30"), lore);
        assertEquals("SHIELD", screen().at(47).orElseThrow().material());
        assertTrue(String.join("|", screen().at(47).orElseThrow().lore()).contains("damage 25%"));
        assertEquals("IRON_SWORD", screen().at(51).orElseThrow().material());
        desk.clicked(viewer, "Mod", 51, ClickKind.LEFT);
        assertTrue(asked.get(1).startsWith("plugin/dossier/history?player=Steve&page=0"));
        desk.clicked(viewer, "Mod", 45, ClickKind.LEFT);
        assertEquals(2, asked.size());
        assertEquals("PLAYER_HEAD", screen().at(4).orElseThrow().material());
    }

    @Test
    void profileSwitchesHonourThePreset() {
        settings = new DossierSettings(true, settings.kicks(), new DossierSettings.ProfileSheet(true, 1, false, false));
        desk.openProfile(viewer, "Mod", "Steve");
        assertEquals("plugin/dossier/profile?player=Steve&scope=owner&latest=1", asked.get(0));
        assertTrue(screen().at(10).isEmpty());
        assertTrue(screen().at(47).orElseThrow().material().startsWith("BLACK"));
        assertTrue(screen().at(51).orElseThrow().material().startsWith("BLACK"));
    }

    @Test
    void troublesShowANoticeAndThrottlingKeepsQuiet() {
        refusal = new DossierReply.Refused(429, "RATE_LIMITED");
        desk.openKicks(viewer, "Mod", 0);
        ItemFace notice = screen().at(22).orElseThrow();
        assertEquals("BARRIER", notice.material());
        assertTrue(notice.name().contains("minute"), notice.name());
        refusal = null;
        desk.openProfile(viewer, "Mod", "Steve");
        assertEquals(1, asked.size());
        now += 61_000;
        desk.openProfile(viewer, "Mod", "Steve");
        assertEquals(2, asked.size());
        refusal = new DossierReply.Unavailable();
        desk.openProfile(viewer, "Mod", "Alex");
        assertTrue(screen().at(23).orElseThrow().name().contains("not reachable"));
        desk.clicked(viewer, "Mod", 49, ClickKind.LEFT);
        assertEquals("close", events.get(events.size() - 1));
    }

    @Test
    void oneRequestPerViewerAtATime() {
        pending = new CompletableFuture<>();
        desk.openKicks(viewer, "Mod", 0);
        assertEquals("SUNFLOWER", screen().at(22).orElseThrow().material());
        desk.openProfile(viewer, "Mod", "Steve");
        assertEquals(1, asked.size());
        assertTrue(screen().at(23).orElseThrow().name().contains("Still loading"));
        pending.complete(new DossierReply.Found(LenientJson.object(KICKS).orElseThrow()));
        assertTrue(screen().at(23).orElseThrow().name().contains("Still loading"));
        pending = null;
        desk.openProfile(viewer, "Mod", "Steve");
        assertEquals(2, asked.size());
        assertEquals("PLAYER_HEAD", screen().at(4).orElseThrow().material());
    }

    @Test
    void disabledKicksPageAndBadNamesNeverReachTheBackend() {
        settings = new DossierSettings(false, new DossierSettings.KickPage(false, 7, true, true), settings.profile());
        desk.openSuspects(viewer, "Mod");
        assertEquals("BLACK_STAINED_GLASS_PANE", screen().at(53).orElseThrow().material());
        desk.openKicks(viewer, "Mod", 0);
        assertTrue(events.get(events.size() - 1).startsWith("send "));
        desk.openProfile(viewer, "Mod", "bad name");
        desk.openHistory(viewer, "Mod", new MenuReaction.OpenHistory("a;b", 0, true, new MenuReaction.Close()));
        assertTrue(asked.isEmpty());
        assertFalse(events.isEmpty());
    }

    @Override
    public void open(UUID target, MenuFrame frame) {
        events.add("open " + frame.title());
        screens.put(target, frame);
    }

    @Override
    public void redraw(UUID target, MenuFrame frame) {
        events.add("redraw");
        screens.put(target, frame);
    }

    @Override
    public void close(UUID target) {
        events.add("close");
    }

    @Override
    public void send(UUID target, String coloredText) {
        events.add("send " + coloredText);
    }

    @Override
    public void teleport(UUID target, UUID suspect) {
        events.add("teleport " + suspect);
    }

    @Override
    public void runConsoleNextTick(String command) {
        events.add("console " + command);
    }

    @Override
    public void onViewerThread(UUID target, Runnable task) {
        task.run();
    }

    @Override
    public List<SuspectRow> onlineSuspects() {
        return List.of();
    }

    @Override
    public Optional<SuspectRow> suspect(UUID player) {
        return Optional.empty();
    }

    @Override
    public boolean online(UUID player) {
        return true;
    }
}
