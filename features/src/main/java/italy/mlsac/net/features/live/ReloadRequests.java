// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.banimation.AnimationLibrary;
import italy.mlsac.net.lifecycle.ConfigReloader;
import italy.mlsac.net.lifecycle.ReloadKind;
import italy.mlsac.net.maintenance.ReloadDesk;

public final class ReloadRequests implements ReloadDesk {

    private final ConfigReloader reloader;
    private final AnimationLibrary animations;
    private final BundledAnimations files;

    public ReloadRequests(ConfigReloader reloader, AnimationLibrary animations, BundledAnimations files) {
        this.reloader = reloader;
        this.animations = animations;
        this.files = files;
    }

    @Override
    public void scheduleFullReload() {
        reloader.reload(ReloadKind.RELOAD);
    }

    @Override
    public int reloadAnimations() {
        return animations.load(files.get());
    }

    @Override
    public boolean syncConfigAndReload() {
        reloader.reload(ReloadKind.REINSTALL);
        return true;
    }
}
