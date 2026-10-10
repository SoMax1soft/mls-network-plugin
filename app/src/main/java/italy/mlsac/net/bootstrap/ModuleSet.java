// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.module.PluginModule;
import italy.mlsac.net.text.MessageBook;
import java.util.ArrayList;
import java.util.List;

final class ModuleSet {

    private final List<PluginModule> modules;
    private final PluginLog log;
    private final List<PluginModule> started = new ArrayList<>();

    ModuleSet(List<PluginModule> modules, PluginLog log) {
        this.modules = List.copyOf(modules);
        this.log = log;
    }

    void registerMessages(MessageBook messages) {
        modules.forEach(module -> messages.registerDefaults(module.messageDefaults()));
    }

    List<Subcommand> subcommands() {
        List<Subcommand> all = new ArrayList<>();
        modules.forEach(module -> all.addAll(module.subcommands()));
        return all;
    }

    void startAll() {
        for (PluginModule module : modules) {
            try {
                module.start();
                started.add(module);
            } catch (RuntimeException failure) {
                log.error(LogTopic.GENERAL, "Could not start the " + module.name() + " module: " + failure, failure);
            }
        }
    }

    void haltEvery() {
        for (int i = started.size() - 1; i >= 0; i--) {
            PluginModule module = started.get(i);
            try {
                module.stop();
            } catch (RuntimeException failure) {
                log.error(LogTopic.GENERAL, "Module " + module.name() + " failed to stop: " + failure, failure);
            }
        }
        started.clear();
    }
}
