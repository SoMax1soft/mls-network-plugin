// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.config.ConfigChangeCause;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.lifecycle.ConfigReloader;
import italy.mlsac.net.lifecycle.ReloadKind;
import italy.mlsac.net.lifecycle.ReloadOutcome;
import java.util.concurrent.CompletableFuture;

final class ReloadProcedure implements ConfigReloader {

    private final CoreWiring core;

    ReloadProcedure(CoreWiring core) {
        this.core = core;
    }

    @Override
    public CompletableFuture<ReloadOutcome> reload(ReloadKind kind) {
        CompletableFuture<ReloadOutcome> outcome = new CompletableFuture<>();
        core.scheduler.submit(TaskLane.MAIN, TaskTiming.immediately(), () -> outcome.complete(runOnMainThread(kind)));
        return outcome;
    }

    private ReloadOutcome runOnMainThread(ReloadKind kind) {
        try {
            if (kind == ReloadKind.REINSTALL) {
                core.localFile.syncMissing();
            }
            EffectiveConfig config = new ConfigurationLoad(core).readLocal(ConfigChangeCause.RELOAD);
            core.presets.stop();
            core.updates.stop();
            core.backend.stop().whenComplete((ignored, error) -> {
                if (config.detection().enabled()) {
                    core.backend.start(config.core());
                }
            });
            core.updates.start();
            core.presets.start(config.core(), config.detection().enabled());
            core.log.info(LogTopic.GENERAL, kind == ReloadKind.REINSTALL
                    ? "config.yml completed with missing defaults and reloaded" : "Settings reloaded");
            return ReloadOutcome.succeeded();
        } catch (Exception failure) {
            core.log.error(LogTopic.GENERAL, "Reload aborted: " + failure.getMessage(), failure);
            return ReloadOutcome.failed(String.valueOf(failure.getMessage()));
        }
    }
}
