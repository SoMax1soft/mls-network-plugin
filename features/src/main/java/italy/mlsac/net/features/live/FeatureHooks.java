// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.banimation.AnimationDirector;
import italy.mlsac.net.features.settings.FeatureSettings;
import italy.mlsac.net.menu.MenuDesk;
import italy.mlsac.net.nametag.NametagDirector;
import italy.mlsac.net.response.DamageReductions;
import java.util.Optional;
import java.util.function.Supplier;

public record FeatureHooks(MenuDesk menus, AnimationDirector animations, DamageReductions reductions,
                           Supplier<Optional<NametagDirector>> nametags, Supplier<FeatureSettings> settings) {
}
