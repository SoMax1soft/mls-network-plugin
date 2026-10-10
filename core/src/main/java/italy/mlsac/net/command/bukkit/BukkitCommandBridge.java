// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.command.bukkit;

import italy.mlsac.net.command.CommandRouter;
import java.util.Arrays;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

public final class BukkitCommandBridge implements TabExecutor {

    private final CommandRouter router;

    public BukkitCommandBridge(CommandRouter router) {
        this.router = router;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        router.dispatch(new BukkitCommandActor(sender), Arrays.asList(args));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return router.complete(new BukkitCommandActor(sender), Arrays.asList(args));
    }
}
