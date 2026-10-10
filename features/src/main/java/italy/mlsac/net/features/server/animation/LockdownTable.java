// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.animation;

import italy.mlsac.net.banimation.MovementClamp;
import italy.mlsac.net.banimation.Point;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;

public final class LockdownTable {

    private final InventoryResync resync;

    public LockdownTable(InventoryResync resync) {
        this.resync = resync;
    }

    public List<ProtectedAction<?>> actions() {
        return Stream.of(handsOff(), inventoryFrozen(), bodyKept()).flatMap(Function.identity()).toList();
    }

    private Stream<ProtectedAction<?>> handsOff() {
        return Stream.of(
                guard(PlayerInteractEvent.class, PlayerEvent::getPlayer, Countermeasure.veto()),
                guard(PlayerInteractEntityEvent.class, PlayerEvent::getPlayer, Countermeasure.veto()),
                guard(PlayerInteractAtEntityEvent.class, PlayerEvent::getPlayer, Countermeasure.veto()),
                guard(PlayerArmorStandManipulateEvent.class, PlayerEvent::getPlayer, Countermeasure.veto()),
                guard(PlayerCommandPreprocessEvent.class, PlayerEvent::getPlayer, Countermeasure.veto()),
                guard(BlockBreakEvent.class, BlockBreakEvent::getPlayer, Countermeasure.veto()),
                guard(BlockPlaceEvent.class, BlockPlaceEvent::getPlayer, Countermeasure.veto()),
                guard(EntityPickupItemEvent.class, EntityEvent::getEntity, Countermeasure.veto()));
    }

    private Stream<ProtectedAction<?>> inventoryFrozen() {
        return Stream.of(
                guard(InventoryClickEvent.class, InventoryInteractEvent::getWhoClicked, vetoAndRefresh()),
                guard(InventoryDragEvent.class, InventoryInteractEvent::getWhoClicked, vetoAndRefresh()),
                guard(InventoryOpenEvent.class, InventoryOpenEvent::getPlayer, vetoAndRefresh()),
                guard(PlayerSwapHandItemsEvent.class, PlayerEvent::getPlayer, vetoAndRefresh()),
                guard(PlayerItemHeldEvent.class, PlayerEvent::getPlayer, vetoAndRefresh()),
                guard(PlayerDropItemEvent.class, PlayerEvent::getPlayer, Countermeasure.<PlayerDropItemEvent>veto()
                        .then((event, suspect) -> event.getItemDrop().remove())
                        .then(refresh())),
                new ProtectedAction<>(InventoryMoveItemEvent.class, EventPriority.HIGHEST,
                        event -> Stream.of(event.getSource(), event.getDestination()).flatMap(LockdownTable::wearer),
                        Countermeasure.veto()),
                new ProtectedAction<>(InventoryCloseEvent.class, EventPriority.MONITOR,
                        event -> Stream.of(event.getPlayer()), (event, suspect) -> resync.shutAgain(suspect)));
    }

    private Stream<ProtectedAction<?>> bodyKept() {
        return Stream.of(
                new ProtectedAction<>(EntityDamageByEntityEvent.class, EventPriority.HIGHEST,
                        event -> Stream.of(event.getEntity(), shooterOrSelf(event.getDamager())),
                        Countermeasure.veto()),
                guard(PlayerDeathEvent.class, PlayerDeathEvent::getEntity, LockdownTable::keepBelongings),
                guard(PlayerMoveEvent.class, PlayerEvent::getPlayer, LockdownTable::pinHorizontally));
    }

    private static <E extends Event> ProtectedAction<E> guard(Class<E> kind,
            Function<? super E, ? extends Entity> actor, Countermeasure<? super E> countermeasure) {
        return new ProtectedAction<>(kind, EventPriority.HIGHEST, event -> Stream.ofNullable(actor.apply(event)),
                countermeasure);
    }

    private <E extends Event & Cancellable> Countermeasure<E> vetoAndRefresh() {
        return Countermeasure.<E>veto().then(refresh());
    }

    private Countermeasure<Event> refresh() {
        return (event, suspect) -> resync.refresh(suspect);
    }

    private static Stream<Entity> wearer(Inventory inventory) {
        if (inventory.getType() != InventoryType.PLAYER) {
            return Stream.empty();
        }
        return inventory.getHolder() instanceof Player owner ? Stream.of(owner) : Stream.empty();
    }

    private static Entity shooterOrSelf(Entity damager) {
        return damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter
                ? shooter : damager;
    }

    private static void keepBelongings(PlayerDeathEvent death, Player suspect) {
        death.setKeepInventory(true);
        death.setKeepLevel(true);
        death.getDrops().clear();
        death.setDroppedExp(0);
    }

    private static void pinHorizontally(PlayerMoveEvent move, Player suspect) {
        Location from = move.getFrom();
        Location to = move.getTo();
        if (move.isCancelled() || to == null || (from.getX() == to.getX() && from.getZ() == to.getZ())) {
            return;
        }
        Point kept = MovementClamp.verticalOnly(new Point(from.getX(), from.getY(), from.getZ()),
                new Point(to.getX(), to.getY(), to.getZ()));
        Location pinned = to.clone();
        pinned.setX(kept.x());
        pinned.setY(kept.y());
        pinned.setZ(kept.z());
        move.setTo(pinned);
    }
}
