// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import italy.mlsac.net.config.ConfigChangeCause;
import italy.mlsac.net.config.ConfigTree;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.config.core.CoreSettingsReader;
import italy.mlsac.net.config.local.LocalConfigException;
import italy.mlsac.net.console.LogTopic;

final class ConfigurationLoad {

    private final CoreWiring core;

    ConfigurationLoad(CoreWiring core) {
        this.core = core;
    }

    EffectiveConfig readLocal(ConfigChangeCause cause) throws LocalConfigException {
        ConfigTree file = core.localFile.prepare();
        CoreSettingsReader.Result settings = core.coreReader.read(file);
        settings.warnings().forEach(warning -> core.log.warn(LogTopic.CONFIG, warning));
        core.redactor.register(settings.settings().accessKey().value());
        return core.composer.loadLocal(file, settings.settings(), cause);
    }
}
