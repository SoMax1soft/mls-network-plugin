// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server;

import italy.mlsac.net.features.live.ConsoleCommands;
import italy.mlsac.net.features.live.SuspectRows;
import italy.mlsac.net.features.server.menu.ItemFaces;
import italy.mlsac.net.features.server.menu.MenuHolder;
import italy.mlsac.net.features.server.menu.OpenMenus;
import italy.mlsac.net.menu.MenuCanvas;
import italy.mlsac.net.menu.MenuFrame;
import italy.mlsac.net.menu.SuspectRow;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.TaskTiming;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class BukkitMenuCanvas implements MenuCanvas {

    private final PlayerDirectory players;
    private final TaskScheduler scheduler;
    private final OwningThread threads;
    private final ConsoleCommands console;
    private final SuspectRows rows;
    private final ItemFaces faces;
    private final OpenMenus menus;

    public BukkitMenuCanvas(PlayerDirectory players, TaskScheduler scheduler, OwningThread threads,
                            ConsoleCommands console, SuspectRows rows, ItemFaces faces, OpenMenus menus) {
        this.players = players;
        this.scheduler = scheduler;
        this.threads = threads;
        this.console = console;
        this.rows = rows;
        this.faces = faces;
        this.menus = menus;
    }

    @Override
    public void open(UUID viewer, MenuFrame frame) {
        threads.withPlayer(viewer, player -> {
            MenuHolder holder = new MenuHolder(viewer, frame.size(), frame.title());
            faces.paint(holder.getInventory(), frame);
            menus.opened(holder);
            player.openInventory(holder.getInventory());
        });
    }

    @Override
    public void redraw(UUID viewer, MenuFrame frame) {
        threads.withPlayer(viewer, player -> menus.viewedBy(player)
                .ifPresent(holder -> faces.paint(holder.getInventory(), frame)));
    }

    @Override
    public void close(UUID viewer) {
        threads.withPlayer(viewer, player -> menus.viewedBy(player).ifPresent(holder -> player.closeInventory()));
    }

    @Override
    public void send(UUID viewer, String coloredText) {
        players.byId(viewer).filter(PlayerView::online).ifPresent(view -> view.sendMessage(coloredText));
    }

    @Override
    public void teleport(UUID viewer, UUID target) {
        threads.withPlayer(target, suspect -> {
            Location destination = suspect.getLocation();
            threads.withPlayer(viewer, player -> move(player, destination));
        });
    }

    @Override
    public void runConsoleNextTick(String command) {
        scheduler.submit(TaskLane.MAIN, TaskTiming.immediately(), () -> console.dispatch(command));
    }

    @Override
    public void onViewerThread(UUID viewer, Runnable task) {
        scheduler.submit(TaskLane.following(viewer), TaskTiming.immediately(), task);
    }

    @Override
    public List<SuspectRow> onlineSuspects() {
        return rows.onlineSuspects();
    }

    @Override
    public Optional<SuspectRow> suspect(UUID player) {
        return rows.suspect(player);
    }

    @Override
    public boolean online(UUID player) {
        return players.byId(player).map(PlayerView::online).orElse(false);
    }

    private void move(Player player, Location destination) {
        if (threads.folia()) {
            player.teleportAsync(destination);
        } else {
            player.teleport(destination);
        }
    }
}
