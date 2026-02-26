package com.kurb.shepherd.listeners;

import com.kurb.shepherd.Shepherd;
import com.kurb.shepherd.items.ChargeItem;
import com.kurb.shepherd.items.StaffItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class StaffInteractListener implements Listener {

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!StaffItem.isStaff(item)) return;
        if (!(event.getRightClicked() instanceof Villager villager)) return;

        // Always cancel to prevent opening the trade GUI
        event.setCancelled(true);

        if (!player.hasPermission("shepherd.use")) {
            player.sendMessage("§c[Shepherd] You don't have permission to use the Shepherd's Staff.");
            return;
        }

        if (player.isSneaking()) {
            handleUnlink(player, item, villager);
        } else {
            handleLink(player, item, villager);
        }
    }

    @EventHandler
    public void onInteractWithCharge(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (!ChargeItem.isCharge(item)) return;
        // Cancel vanilla fire_charge behaviour (launches a fireball that ignites blocks)
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteractBlock(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!StaffItem.isStaff(item)) return;

        if (!player.hasPermission("shepherd.use")) {
            player.sendMessage("§c[Shepherd] You don't have permission to use the Shepherd's Staff.");
            return;
        }

        UUID linkedUUID = StaffItem.getLinkedVillager(item);
        if (linkedUUID == null) return; // staff not linked, let vanilla block interaction happen

        event.setCancelled(true);
        handleRedirect(player, item, linkedUUID, event.getClickedBlock().getLocation());
    }

    // -------------------------------------------------------------------------

    private void handleLink(Player player, ItemStack staffItem, Villager villager) {
        if (StaffItem.getLinkedVillager(staffItem) != null) {
            player.sendMessage("§c[Shepherd] Your staff is already linked to a villager. Unlink it first.");
            return;
        }

        ItemStack charge = findCharge(player);
        if (charge == null) {
            player.sendMessage("§c[Shepherd] You need a Shepherd Charge to link a villager.");
            return;
        }

        PersistentDataContainer vilPDC = villager.getPersistentDataContainer();
        if (vilPDC.has(Shepherd.KEY_OWNER, PersistentDataType.STRING)) {
            player.sendMessage("§c[Shepherd] This villager is already linked by another player.");
            return;
        }

        StaffItem.setLinkedVillager(staffItem, villager.getUniqueId());
        player.getInventory().setItemInMainHand(staffItem);

        vilPDC.set(Shepherd.KEY_OWNER, PersistentDataType.STRING, player.getUniqueId().toString());
        villager.setGlowing(true);
        villager.setAI(false);

        charge.setAmount(0);

        player.sendMessage("§a[Shepherd] Villager linked! A charge has been consumed.");
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
    }

    private void handleUnlink(Player player, ItemStack staffItem, Villager villager) {
        UUID linkedUUID = StaffItem.getLinkedVillager(staffItem);

        if (linkedUUID == null) {
            player.sendMessage("§c[Shepherd] Your staff is not linked to any villager.");
            return;
        }

        if (!linkedUUID.equals(villager.getUniqueId())) {
            player.sendMessage("§c[Shepherd] This is not your linked villager.");
            return;
        }

        cancelPathTask(villager.getUniqueId());

        StaffItem.removeLinkedVillager(staffItem);
        player.getInventory().setItemInMainHand(staffItem);

        villager.getPersistentDataContainer().remove(Shepherd.KEY_OWNER);
        villager.setGlowing(false);
        villager.setAI(true);

        player.sendMessage("§c[Shepherd] Villager unlinked.");
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.0f);
    }

    private void handleRedirect(Player player, ItemStack staffItem, UUID linkedUUID, Location targetLocation) {
        Entity entity = Bukkit.getEntity(linkedUUID);

        if (!(entity instanceof Villager villager)) {
            StaffItem.removeLinkedVillager(staffItem);
            player.getInventory().setItemInMainHand(staffItem);
            player.sendMessage("§c[Shepherd] Your linked villager could not be found. Staff unlinked.");
            return;
        }

        if (!villager.getWorld().equals(targetLocation.getWorld())) {
            player.sendMessage("§c[Shepherd] The villager is in a different world.");
            return;
        }

        Shepherd plugin = JavaPlugin.getPlugin(Shepherd.class);
        double maxDistance = plugin.getConfig().getDouble("max-distance", 20.0);
        double speed = plugin.getConfig().getDouble("speed", 1.0);

        double dx = villager.getLocation().getX() - targetLocation.getX();
        double dz = villager.getLocation().getZ() - targetLocation.getZ();
        double distance2D = Math.sqrt(dx * dx + dz * dz);

        if (distance2D > maxDistance) {
            player.sendMessage("§c[Shepherd] The villager is too far away. Get closer and try again.");
            return;
        }

        // Re-enable AI so the navigation system can tick
        villager.setAI(true);
        cancelPathTask(villager.getUniqueId());

        player.sendMessage("§e[Shepherd] Villager is on its way!");
        villager.getWorld().playSound(villager.getLocation(), Sound.ENTITY_VILLAGER_WORK_FARMER, 1.0f, 1.0f);

        // If the villager is mid-air (e.g. was frozen in the air), wait for it to land
        // before issuing moveTo — avoids pathfinding failure from a floating position.
        if (!villager.isOnGround()) {
            BukkitTask waitTask = new BukkitRunnable() {
                int waitTicks = 0;

                @Override
                public void run() {
                    if (!villager.isValid()) {
                        plugin.activePathTasks.remove(villager.getUniqueId());
                        cancel();
                        return;
                    }
                    waitTicks += 2;
                    if (waitTicks > 60) { // 3-second timeout
                        villager.setAI(false);
                        plugin.activePathTasks.remove(villager.getUniqueId());
                        cancel();
                        return;
                    }
                    if (villager.isOnGround()) {
                        plugin.activePathTasks.remove(villager.getUniqueId());
                        cancel();
                        startPathfinding(plugin, player, villager, targetLocation, speed);
                    }
                }
            }.runTaskTimer(plugin, 2L, 2L);
            plugin.activePathTasks.put(villager.getUniqueId(), waitTask);
            return;
        }

        startPathfinding(plugin, player, villager, targetLocation, speed);
    }

    private void startPathfinding(Shepherd plugin, Player player, Villager villager, Location targetLocation, double speed) {
        boolean pathFound = villager.getPathfinder().moveTo(targetLocation, speed);
        if (!pathFound) {
            villager.setAI(false);
            player.sendMessage("§c[Shepherd] The villager can't reach that location.");
            return;
        }

        BukkitTask task = new BukkitRunnable() {
            int noPathTicks = 0;

            @Override
            public void run() {
                if (!villager.isValid()) {
                    plugin.activePathTasks.remove(villager.getUniqueId());
                    cancel();
                    return;
                }
                // Proximity freeze: if the villager is close enough, freeze immediately
                // instead of waiting for hasPath()=false (fixes circling-at-destination bug).
                if (villager.getLocation().distance(targetLocation) <= 0.9) {
                    villager.setAI(false);
                    plugin.activePathTasks.remove(villager.getUniqueId());
                    cancel();
                    return;
                }
                if (!villager.getPathfinder().hasPath()) {
                    // Only count no-path ticks while on the ground — a villager falling off
                    // a ledge will briefly lose its path; freezing it mid-air would look wrong.
                    if (villager.isOnGround()) {
                        noPathTicks++;
                        if (noPathTicks >= 2) {
                            villager.setAI(false);
                            plugin.activePathTasks.remove(villager.getUniqueId());
                            if (villager.getLocation().distance(targetLocation) > 3.0) {
                                String ownerStr = villager.getPersistentDataContainer()
                                        .get(Shepherd.KEY_OWNER, PersistentDataType.STRING);
                                if (ownerStr != null) {
                                    Player owner = Bukkit.getPlayer(UUID.fromString(ownerStr));
                                    if (owner != null) {
                                        owner.sendMessage("§c[Shepherd] Your villager couldn't reach the destination.");
                                    }
                                }
                            }
                            cancel();
                        }
                    }
                } else {
                    noPathTicks = 0;
                    // Re-issue moveTo every tick to prevent the villager's AI brain
                    // (schedules, POI, bed) from overriding our goal (bug: ignores order)
                    villager.getPathfinder().moveTo(targetLocation, speed);
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);
        plugin.activePathTasks.put(villager.getUniqueId(), task);
    }

    // -------------------------------------------------------------------------

    private void cancelPathTask(UUID villagerUUID) {
        Shepherd plugin = JavaPlugin.getPlugin(Shepherd.class);
        BukkitTask task = plugin.activePathTasks.remove(villagerUUID);
        if (task != null) task.cancel();
    }

    private ItemStack findCharge(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (ChargeItem.isCharge(item)) return item;
        }
        return null;
    }
}
