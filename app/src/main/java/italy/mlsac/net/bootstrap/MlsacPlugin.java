// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import italy.mlsac.net.platform.ClassProbe;
import italy.mlsac.net.platform.PlatformProfile;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class MlsacPlugin extends JavaPlugin {

    private PlatformProfile platform;
    private PluginRuntime runtime;

    @Override
    public void onLoad() {
        BakedBuildProperties.apply(getClassLoader());
        platform = PlatformProfile.detect(ClassProbe.of(getClassLoader()), Bukkit.getBukkitVersion(),
                Runtime.version().feature());
    }

    @Override
    public void onEnable() {
        if (platform == null) {
            onLoad();
        }
        runtime = new PluginRuntime(this, platform);
        try {
            runtime.enable();
        } catch (StartupFailure failure) {
            abort(failure.getMessage(), null);
        } catch (RuntimeException | LinkageError unexpected) {
            abort("MLSAC failed to enable: " + unexpected, unexpected);
        }
    }

    private void abort(String reason, Throwable trace) {
        getLogger().log(Level.SEVERE, reason, trace);
        runtime.disable();
        runtime = null;
        getServer().getPluginManager().disablePlugin(this);
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            runtime.disable();
            runtime = null;
        }
    }
}
