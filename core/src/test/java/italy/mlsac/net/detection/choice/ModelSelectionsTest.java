// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection.choice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.api.FallbackReason;
import italy.mlsac.net.api.ModelChoice;
import italy.mlsac.net.api.ModelSelection;
import italy.mlsac.net.player.PlayerLeft;
import italy.mlsac.net.testkit.TestConfigs;
import italy.mlsac.net.testkit.TestCore;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModelSelectionsTest {

    private static final UUID FIRST = new UUID(1, 1);
    private static final UUID SECOND = new UUID(2, 2);

    private final TestCore core = new TestCore();
    private final ModelSelections selections = core.selections;
    private final List<ModelSelectionChanged> changes = new ArrayList<>();

    @BeforeEach
    void listen() {
        core.bus.subscribe(ModelSelectionChanged.class, changes::add);
    }

    @Test
    void withoutAnOverrideEveryPlayerKeepsTheServerDefault() {
        assertEquals(ModelSelection.serverDefault(FIRST), selections.get(FIRST));
        assertEquals(ModelRoute.SERVER_DEFAULT, selections.routeOf(FIRST));
        assertFalse(selections.anyPlayerOn(ModelRoute.ULTRA));
        assertTrue(selections.available().contains(ModelChoice.DEFAULT));
        assertTrue(changes.isEmpty());
    }

    @Test
    void anAvailableChoiceIsGrantedAndAnnounced() {
        ModelSelection granted = selections.set(FIRST, ModelChoice.PRO);

        assertEquals(ModelSelection.granted(FIRST, ModelChoice.PRO), granted);
        assertEquals(granted, selections.get(FIRST));
        assertEquals(ModelRoute.PRO, selections.routeOf(FIRST));
        assertEquals(ModelRoute.SERVER_DEFAULT, selections.routeOf(SECOND));
        assertEquals(List.of(new ModelSelectionChanged(ModelSelection.serverDefault(FIRST), granted)), changes);

        selections.set(FIRST, ModelChoice.PRO);
        assertEquals(1, changes.size());
    }

    @Test
    void ultraIsOfferedWhateverThePresetSaysAndCountedForSampling() {
        core.apply(TestConfigs.withModels(core.config.get(), true, "pro", "fast"));
        assertTrue(selections.available().contains(ModelChoice.ULTRA));

        selections.set(FIRST, ModelChoice.ULTRA);

        assertEquals(ModelRoute.ULTRA, selections.routeOf(FIRST));
        assertTrue(selections.anyPlayerOn(ModelRoute.ULTRA));
        selections.clear(FIRST);
        assertFalse(selections.anyPlayerOn(ModelRoute.ULTRA));
        assertEquals(ModelSelection.serverDefault(FIRST), changes.get(changes.size() - 1).current());
    }

    @Test
    void aModelSwitchedOffInThePresetFallsBackWithTheReason() {
        core.apply(TestConfigs.withModels(core.config.get(), true, "ultra", "fast"));

        ModelSelection pro = selections.set(FIRST, ModelChoice.PRO);

        assertEquals(ModelSelection.fellBack(FIRST, ModelChoice.PRO, FallbackReason.DISABLED_IN_PRESET), pro);
        assertEquals(ModelRoute.SERVER_DEFAULT, selections.routeOf(FIRST));
        assertEquals(Optional.of(FallbackReason.DISABLED_IN_PRESET), selections.unavailableBecause(ModelChoice.PRO));
        assertEquals(EnumSet.of(ModelChoice.DEFAULT, ModelChoice.MINI, ModelChoice.ULTRA), selections.available());
        assertEquals(1, changes.size());
    }

    @Test
    void detectionOffLeavesOnlyTheServerDefault() {
        core.apply(TestConfigs.withModels(core.config.get(), false, "pro", "fast", "ultra"));

        assertEquals(Set.of(ModelChoice.DEFAULT), selections.available());
        assertEquals(Optional.of(FallbackReason.DETECTION_OFF), selections.set(FIRST, ModelChoice.ULTRA).fallback());
    }

    @Test
    void presetChangesMoveEveryRequestBetweenFallbackAndGrant() {
        selections.set(FIRST, ModelChoice.PRO);
        selections.set(SECOND, ModelChoice.MINI);
        changes.clear();

        core.apply(TestConfigs.withModels(core.config.get(), true, "fast"));

        assertEquals(ModelChoice.DEFAULT, selections.get(FIRST).effective());
        assertEquals(ModelChoice.PRO, selections.get(FIRST).requested());
        assertEquals(ModelChoice.MINI, selections.get(SECOND).effective());
        assertEquals(1, changes.size());

        core.apply(TestConfigs.withModels(core.config.get(), true, "fast", "pro"));

        assertEquals(ModelSelection.granted(FIRST, ModelChoice.PRO), selections.get(FIRST));
        assertEquals(2, changes.size());
    }

    @Test
    void aBackendRefusalAppliesToEveryPlayerUntilTheConfigurationIsRebuilt() {
        selections.set(FIRST, ModelChoice.ULTRA);
        selections.set(SECOND, ModelChoice.ULTRA);
        changes.clear();

        selections.refused(ModelChoice.ULTRA, FallbackReason.REFUSED_BY_BACKEND);
        selections.refused(ModelChoice.ULTRA, FallbackReason.REFUSED_BY_BACKEND);

        assertEquals(2, changes.size());
        assertEquals(ModelSelection.fellBack(FIRST, ModelChoice.ULTRA, FallbackReason.REFUSED_BY_BACKEND),
                selections.get(FIRST));
        assertFalse(selections.anyPlayerOn(ModelRoute.ULTRA));
        assertFalse(selections.available().contains(ModelChoice.ULTRA));
        assertEquals(Optional.of(FallbackReason.REFUSED_BY_BACKEND),
                selections.set(new UUID(3, 3), ModelChoice.ULTRA).fallback());
        assertTrue(core.logSink.contains("Model choice ultra is not available any more"));

        core.apply(core.config.get());

        assertEquals(ModelSelection.granted(FIRST, ModelChoice.ULTRA), selections.get(FIRST));
        assertEquals(ModelSelection.granted(SECOND, ModelChoice.ULTRA), selections.get(SECOND));
        assertTrue(selections.anyPlayerOn(ModelRoute.ULTRA));
    }

    @Test
    void aRefusalOfTheServerDefaultMeansNothing() {
        selections.refused(ModelChoice.DEFAULT, FallbackReason.REFUSED_BY_BACKEND);
        assertTrue(selections.available().contains(ModelChoice.DEFAULT));
        assertTrue(changes.isEmpty());
    }

    @Test
    void quittingForgetsTheOverrideWithoutAnEvent() {
        selections.set(FIRST, ModelChoice.ULTRA);
        changes.clear();

        core.bus.publish(new PlayerLeft(FIRST, "First", false));

        assertEquals(ModelSelection.serverDefault(FIRST), selections.get(FIRST));
        assertFalse(selections.anyPlayerOn(ModelRoute.ULTRA));
        assertTrue(changes.isEmpty());
    }

    @Test
    void stoppingDropsEveryOverride() {
        selections.set(FIRST, ModelChoice.MINI);
        selections.stop();
        assertEquals(ModelRoute.SERVER_DEFAULT, selections.routeOf(FIRST));
        core.apply(core.config.get());
        assertTrue(selections.get(FIRST).equals(ModelSelection.serverDefault(FIRST)));
    }
}
