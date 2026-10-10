// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.animation;

import italy.mlsac.net.banimation.AnimationDirector;
import italy.mlsac.net.features.server.EventBinding;
import java.util.List;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public final class AnimationLockdown implements EventBinding {

    private final AnimationDirector animations;
    private final List<ProtectedAction<?>> actions;

    public AnimationLockdown(AnimationDirector animations, List<ProtectedAction<?>> actions) {
        this.animations = animations;
        this.actions = List.copyOf(actions);
    }

    @Override
    public void bindTo(Plugin plugin, PluginManager events) {
        actions.forEach(action -> events.registerEvent(action.kind(), this, action.priority(),
                (listener, event) -> action.enforce(event, animations::animating), plugin));
    }
}
