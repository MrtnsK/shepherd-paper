package com.kurb.shepherd.items;

import com.kurb.shepherd.Shepherd;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public class StaffItem {

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Shepherd's Staff")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Right-click a villager to link it.")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Right-click a block to redirect it.")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Sneak + right-click a villager to unlink.")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.setMaxStackSize(1);
        meta.getPersistentDataContainer().set(Shepherd.KEY_STAFF, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isStaff(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(Shepherd.KEY_STAFF, PersistentDataType.BYTE);
    }

    public static UUID getLinkedVillager(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String uuidStr = meta.getPersistentDataContainer().get(Shepherd.KEY_LINKED_VILLAGER, PersistentDataType.STRING);
        if (uuidStr == null) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void setLinkedVillager(ItemStack item, UUID uuid) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(Shepherd.KEY_LINKED_VILLAGER, PersistentDataType.STRING, uuid.toString());
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
    }

    public static void removeLinkedVillager(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().remove(Shepherd.KEY_LINKED_VILLAGER);
        meta.setEnchantmentGlintOverride(false);
        item.setItemMeta(meta);
    }
}
