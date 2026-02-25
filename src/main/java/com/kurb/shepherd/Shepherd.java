package com.kurb.shepherd;

import com.kurb.shepherd.commands.ShepherdCommand;
import com.kurb.shepherd.listeners.StaffInteractListener;
import com.kurb.shepherd.listeners.VillagerDeathListener;
import com.kurb.shepherd.managers.RecipeManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Shepherd extends JavaPlugin {

    public static final NamespacedKey KEY_STAFF           = new NamespacedKey("shepherd", "staff");
    public static final NamespacedKey KEY_CHARGE          = new NamespacedKey("shepherd", "charge");
    public static final NamespacedKey KEY_LINKED_VILLAGER = new NamespacedKey("shepherd", "linked_villager");
    public static final NamespacedKey KEY_OWNER           = new NamespacedKey("shepherd", "owner");

    // Tracks the "re-freeze AI" task running per linked villager UUID
    public final Map<UUID, BukkitTask> activePathTasks = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        new RecipeManager(this).register();
        getServer().getPluginManager().registerEvents(new StaffInteractListener(), this);
        getServer().getPluginManager().registerEvents(new VillagerDeathListener(), this);
        ShepherdCommand shepherdCommand = new ShepherdCommand();
        getCommand("shepherd").setExecutor(shepherdCommand);
        getCommand("shepherd").setTabCompleter(shepherdCommand);
        getLogger().info("Shepherd enabled.");
    }

    @Override
    public void onDisable() {
        activePathTasks.values().forEach(BukkitTask::cancel);
        activePathTasks.clear();
        getLogger().info("Shepherd disabled.");
    }
}
