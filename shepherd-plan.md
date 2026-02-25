# Shepherd — Plugin Plan for Claude Code

## Context

Shepherd is a Paper 1.21.11 plugin that adds a craftable tool allowing players to link a villager and redirect it to a specific block. The project must be created entirely from scratch.

---

## Project Setup

- **Platform:** PaperMC 1.21.11
- **Build tool:** Maven
- **Java version:** 21
- **Main package:** `com.kurb.shepherd`
- **Plugin name:** `Shepherd`
- **Main class:** `com.kurb.shepherd.Shepherd`

### Maven dependencies required

- Paper API 1.21.11 (via `papermc` repository) — released December 21, 2025, confirmed at [papermc.io/news/1-21-11](https://papermc.io/news/1-21-11/)

---

## Items

### 1. Shepherd's Staff

- Custom item identified via **PersistentDataContainer** (PDC) key `shepherd:staff`
- Material base: `BLAZE_ROD` (visually fitting, placeholder until resource pack is made)
- Display name: `Shepherd's Staff`
- Custom lore explaining usage
- When a villager is linked, the item gets the **enchantment glint effect** via `ItemMeta.setEnchantmentGlintOverride(true)`
- When unlinked, the glint is removed via `ItemMeta.setEnchantmentGlintOverride(false)`

**Craft recipe (shaped, 3x3):**
```
copper ingot | amethyst shard | gold ingot
     -       | redstone torch   |     -
     -       |     diamond      |     -
```

### 2. Shepherd Charge

- Custom item identified via PDC key `shepherd:charge`
- Material base: `FIRE_CHARGE` (placeholder until resource pack)
- Display name: `Shepherd Charge`
- **Non-stackable** via `ItemMeta.setMaxStackSize(1)`
- Custom lore explaining it is consumed when linking a villager

**Craft recipe (shaped, 3x3):**
```
     -      | amethyst shard |     -
   carrot   | redstone dust  |   carrot
     -      |       -        |     -
```

---

## Core Mechanics

### Linking a villager (Right Click on Villager with Staff)

1. Check player is holding a Shepherd's Staff (`event.getHand() == EquipmentSlot.HAND`)
2. Check player has at least one **Shepherd Charge** in their inventory
3. Check clicked entity is a **Villager**
4. Check staff does not already have a linked villager (one at a time)
5. Check villager's PDC does **not** already contain a `shepherd:owner` key — if it does, send `"§c[Shepherd] This villager is already linked by another player."` and abort
6. Store the villager's **UUID** in the staff's PDC (`shepherd:linked_villager`)
7. Store the **player's UUID** in the villager's PDC (`shepherd:owner`)
8. Apply **glowing effect** to the villager (via `setGlowing(true)`)
9. Consume **one Shepherd Charge** from the player's inventory
10. Apply enchantment glint to the staff item
11. Send player message: `"§a[Shepherd] Villager linked! A charge has been consumed."`
12. Play sound: `Sound.BLOCK_AMETHYST_BLOCK_CHIME` at player location

### Redirecting the villager (Right Click on Block with Staff)

1. Check player is holding a Shepherd's Staff
2. Check staff has a linked villager UUID in PDC
3. Retrieve the villager entity from the world by UUID — if not found (dead/unloaded), auto-unlink and notify player
4. Check distance between villager and target block — if too far (>100 blocks), send message: `"§c[Shepherd] The villager is too far away. Get closer and try again."` and abort
5. Use Paper's `Mob#getPathfinder().moveTo(Location, speed)` to move villager to target block location (speed: `1.0`)
6. The villager stays linked after redirect — the player can redirect multiple times until they manually unlink
7. Send player message: `"§e[Shepherd] Villager is on its way!"`
8. Play sound: `Sound.ENTITY_VILLAGER_WORK_FARMER` at villager location

### Unlinking a villager (Sneak + Right Click on Villager with Staff)

1. Check player is sneaking, holding a Shepherd's Staff (`event.getHand() == EquipmentSlot.HAND`), and right-clicking a Villager
2. Check the clicked villager's UUID matches the one stored in the staff's PDC
3. Remove PDC key `shepherd:linked_villager` from staff
4. Remove PDC key `shepherd:owner` from villager
5. Remove glowing effect from villager (`setGlowing(false)`)
6. Remove enchantment glint from staff item
7. Send player message: `"§c[Shepherd] Villager unlinked."`
8. Play sound: `Sound.BLOCK_AMETHYST_BLOCK_BREAK` at player location

### Villager death handling

- Listen to `EntityDeathEvent`
- If the dead entity is a Villager, scan all online players' **entire inventory** (all slots, including off-hand) for any Shepherd's Staff whose PDC contains this villager's UUID
- If found: remove `shepherd:linked_villager` from the staff, remove glint, send message: `"§c[Shepherd] Your linked villager has died. Staff unlinked."`
- Play sound: `Sound.ENTITY_VILLAGER_DEATH` at the player's location
- Note: the villager's own PDC key `shepherd:owner` is lost on death naturally — no cleanup needed

### Admin command: `/shepherd unlink` (Force-unlink a villager)

- **Permission:** `shepherd.admin`
- **Usage:** `/shepherd unlink` while looking at a villager (uses `Player#getTargetEntity()`)
- **Behavior:**
  1. Check sender is a player with `shepherd.admin` permission
  2. Retrieve the villager the player is looking at (max 5 blocks ray trace)
  3. Check the villager has a `shepherd:owner` PDC key — if not, reply `"§c[Shepherd] This villager is not linked."`
  4. Remove `shepherd:owner` from the villager's PDC
  5. Remove `setGlowing(false)` from the villager
  6. Scan all online players' inventories for a staff linked to this villager — if found, remove `shepherd:linked_villager` and glint, and notify that player: `"§c[Shepherd] An admin has unlinked your villager."`
  7. Reply to admin: `"§a[Shepherd] Villager force-unlinked."`

---

## File Structure

```
src/main/java/com/kurb/shepherd/
├── Shepherd.java                  (main plugin class)
├── items/
│   ├── StaffItem.java             (creates & identifies the staff item)
│   └── ChargeItem.java            (creates & identifies the charge item)
├── listeners/
│   ├── StaffInteractListener.java (handles all click interactions)
│   └── VillagerDeathListener.java (handles villager death)
├── commands/
│   └── ShepherdCommand.java       (handles /shepherd admin commands)
└── managers/
    └── RecipeManager.java         (registers shaped recipes on startup)

src/main/resources/
└── plugin.yml
```

---

## plugin.yml

```yaml
name: Shepherd
version: 1.0.0
main: com.kurb.shepherd.Shepherd
api-version: 1.21.11
description: Control villagers with the Shepherd's Staff
authors: [kurb]
commands:
  shepherd:
    description: Shepherd admin commands
    usage: /shepherd unlink
    permission: shepherd.admin
permissions:
  shepherd.admin:
    description: Allows force-unlinking villagers
    default: op
```

---

## Important Implementation Notes

- Always identify custom items via **PDC**, never by display name or material alone
- The staff's linked villager UUID must be stored and read from the **item in hand at the moment of interaction**, not cached elsewhere — this way multiple staffs can coexist independently
- When consuming a Shepherd Charge, find the **first slot** in the player's inventory containing a valid Shepherd Charge (identified by PDC) and remove it
- Shepherd Charge must be forced non-stackable via `ItemMeta.setMaxStackSize(1)`
- Villager glowing should use `setGlowing(true)` — this makes it visible to all players nearby, which is intentional (the linked villager is visually highlighted)
- Use `PlayerInteractEntityEvent` for right-click on villager (link) and sneak+right-click on villager (unlink)
- Use `PlayerInteractEvent` with `Action.RIGHT_CLICK_BLOCK` for right-click on block (redirect)
- Always check `event.getHand() == EquipmentSlot.HAND` in both events to prevent double-firing (main hand + off hand)
- Always call `event.setCancelled(true)` in `PlayerInteractEvent` when the staff is used to redirect, to prevent triggering the block's default interaction (opening chests, doors, etc.)
- Distance check for redirect is **2D only** (ignore Y axis) — consistent with how Minecraft chunks work; villager and target block must be in the same world
- If `Mob#getPathfinder().moveTo()` returns `false` (path unreachable), send player message: `"§c[Shepherd] The villager can't reach that location."` and do not send the "on its way" message
- **No persistence across server restarts** — `setGlowing` is volatile; on restart all villagers lose their glow and all PDC keys on villagers are gone. Staffs retain their PDC (item NBT persists) but the linked villager won't be found, so the auto-unlink logic in the redirect flow will clean up the staff on next use

---

## Out of Scope (to be done manually later)

- **Custom textures / 3D models** for the Staff and Charge require a **resource pack** built with Blockbench. The plugin uses placeholder vanilla materials until then. The resource pack should use `custom_model_data` to override the model based on a PDC-driven integer tag on the item.
- Two visual states for the staff (linked vs unlinked) are handled **in-plugin via enchantment glint**, so a single model is sufficient for the resource pack.
