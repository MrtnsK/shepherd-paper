package com.kurb.shepherd.commands;

import com.kurb.shepherd.Shepherd;
import com.kurb.shepherd.items.ChargeItem;
import com.kurb.shepherd.items.StaffItem;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

public class ShepherdCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("shepherd.admin")) {
            sender.sendMessage("§c[Shepherd] You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§e[Shepherd] Usage: /shepherd <reload|unlink|link|give <staff|charge>>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            JavaPlugin.getPlugin(Shepherd.class).reloadConfig();
            sender.sendMessage("§a[Shepherd] Config reloaded.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "unlink" -> handleAdminUnlink(player);
            case "link"   -> handleAdminLink(player);
            case "give"   -> {
                if (args.length < 2) {
                    player.sendMessage("§e[Shepherd] Usage: /shepherd give <staff|charge>");
                    return true;
                }
                handleGive(player, args[1]);
            }
            default -> player.sendMessage("§e[Shepherd] Usage: /shepherd <reload|unlink|link|give <staff|charge>>");
        }
        return true;
    }

    // -------------------------------------------------------------------------

    private void handleAdminUnlink(Player admin) {
        Entity target = admin.getTargetEntity(5);

        if (!(target instanceof Villager villager)) {
            admin.sendMessage("§c[Shepherd] Look at a villager within 5 blocks.");
            return;
        }

        if (!villager.getPersistentDataContainer().has(Shepherd.KEY_OWNER, PersistentDataType.STRING)) {
            admin.sendMessage("§c[Shepherd] This villager is not linked.");
            return;
        }

        UUID villagerUUID = villager.getUniqueId();

        Shepherd plugin = JavaPlugin.getPlugin(Shepherd.class);
        BukkitTask task = plugin.activePathTasks.remove(villagerUUID);
        if (task != null) task.cancel();

        villager.getPersistentDataContainer().remove(Shepherd.KEY_OWNER);
        villager.setGlowing(false);
        villager.setAI(true);

        for (Player online : admin.getServer().getOnlinePlayers()) {
            PlayerInventory inv = online.getInventory();
            int size = inv.getSize();

            for (int i = 0; i < size; i++) {
                ItemStack item = inv.getItem(i);
                if (!StaffItem.isStaff(item)) continue;
                if (!villagerUUID.equals(StaffItem.getLinkedVillager(item))) continue;

                StaffItem.removeLinkedVillager(item);
                inv.setItem(i, item);

                online.sendMessage("§c[Shepherd] An admin has unlinked your villager.");
                online.playSound(online.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.0f);
            }
        }

        admin.sendMessage("§a[Shepherd] Villager force-unlinked.");
    }

    private void handleAdminLink(Player admin) {
        Entity target = admin.getTargetEntity(5);

        if (!(target instanceof Villager villager)) {
            admin.sendMessage("§c[Shepherd] Look at a villager within 5 blocks.");
            return;
        }

        if (villager.getPersistentDataContainer().has(Shepherd.KEY_OWNER, PersistentDataType.STRING)) {
            admin.sendMessage("§c[Shepherd] This villager is already linked by another player.");
            return;
        }

        ItemStack staff = StaffItem.create();
        StaffItem.setLinkedVillager(staff, villager.getUniqueId());

        villager.getPersistentDataContainer().set(Shepherd.KEY_OWNER, PersistentDataType.STRING, admin.getUniqueId().toString());
        villager.setGlowing(true);
        villager.setAI(false);

        admin.getInventory().addItem(staff);
        admin.sendMessage("§a[Shepherd] Villager linked. Linked staff added to your inventory.");
    }

    private void handleGive(Player player, String itemName) {
        switch (itemName.toLowerCase()) {
            case "staff" -> {
                player.getInventory().addItem(StaffItem.create());
                player.sendMessage("§a[Shepherd] Shepherd's Staff added to your inventory.");
            }
            case "charge" -> {
                player.getInventory().addItem(ChargeItem.create());
                player.sendMessage("§a[Shepherd] Shepherd Charge added to your inventory.");
            }
            default -> player.sendMessage("§e[Shepherd] Usage: /shepherd give <staff|charge>");
        }
    }

    // -------------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("shepherd.admin")) return List.of();

        if (args.length == 1) {
            return List.of("reload", "link", "unlink", "give").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return List.of("staff", "charge").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}
