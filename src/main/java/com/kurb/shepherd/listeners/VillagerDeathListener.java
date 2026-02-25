package com.kurb.shepherd.listeners;

import com.kurb.shepherd.Shepherd;
import com.kurb.shepherd.items.StaffItem;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class VillagerDeathListener implements Listener {

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Villager)) return;

        UUID villagerUUID = event.getEntity().getUniqueId();

        // Cancel any pending re-freeze task for this villager
        Shepherd plugin = JavaPlugin.getPlugin(Shepherd.class);
        BukkitTask task = plugin.activePathTasks.remove(villagerUUID);
        if (task != null) task.cancel();

        for (Player player : event.getEntity().getServer().getOnlinePlayers()) {
            PlayerInventory inv = player.getInventory();
            int size = inv.getSize();

            for (int i = 0; i < size; i++) {
                ItemStack item = inv.getItem(i);
                if (!StaffItem.isStaff(item)) continue;
                if (!villagerUUID.equals(StaffItem.getLinkedVillager(item))) continue;

                StaffItem.removeLinkedVillager(item);
                inv.setItem(i, item);

                player.sendMessage("§c[Shepherd] Your linked villager has died. Staff unlinked.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_DEATH, 1.0f, 1.0f);
            }
        }
    }
}
