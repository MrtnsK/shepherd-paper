# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build

Maven 3.9+ is installed via Chocolatey. Java 21 is at `C:/Program Files/Java/jdk-21/`.

```bash
mvn package          # produces target/Shepherd-<version>.jar
mvn compile          # compile only
mvn clean package    # clean build
```

The output JAR is named `Shepherd-${project.version}` (set in `pom.xml`). Drop it into a Paper server's `plugins/` folder to test.

No tests exist in this project.

## Architecture

This is a **Paper 1.21.11** Minecraft plugin. The main class `Shepherd.java` is the entry point and shared state holder.

### PDC Key constants (defined in `Shepherd.java`)

All custom item/entity metadata uses PaperMC's `PersistentDataContainer` (PDC). The four keys are:

| Key | Stored on | Purpose |
|-----|-----------|---------|
| `shepherd:staff` | ItemStack | Identifies an item as a Shepherd's Staff |
| `shepherd:charge` | ItemStack | Identifies an item as a Shepherd Charge |
| `shepherd:linked_villager` | Staff ItemStack | UUID string of the linked villager |
| `shepherd:owner` | Villager entity | UUID string of the owning player |

### Shared state

`Shepherd.activePathTasks` (`Map<UUID, BukkitTask>`) tracks the repeating re-freeze task running per linked villager. It is accessed from three places: `StaffInteractListener`, `VillagerDeathListener`, and `ShepherdCommand` via `JavaPlugin.getPlugin(Shepherd.class)`. Always cancel + remove from this map before scheduling a new task for the same villager.

### Core flow

1. **Link** (`StaffInteractListener#handleLink`): right-click villager with staff → consume charge, write `KEY_OWNER` to villager PDC, write `KEY_LINKED_VILLAGER` to staff PDC, `setAI(false)`, `setGlowing(true)`.

2. **Redirect** (`StaffInteractListener#handleRedirect`): right-click block with linked staff → 2D distance check (max 20 blocks), `setAI(true)`, `moveTo(location, 1.0)`, schedule 10-tick repeating `BukkitRunnable` that watches `hasPath()`. When path ends, `setAI(false)`. If distance to target > 3 blocks when path ends → send stuck message to owner.

3. **Unlink** (`StaffInteractListener#handleUnlink`): sneak + right-click linked villager → cancel path task, `setAI(true)`, `setGlowing(false)`, remove both PDC entries.

4. **Death** (`VillagerDeathListener`): cancel path task, scan all online players' inventories, remove `KEY_LINKED_VILLAGER` from matching staves.

5. **Admin commands** (`ShepherdCommand`, permission `shepherd.admin`):
   - `/shepherd link` — force-links the looked-at villager, gives admin a pre-linked staff (no charge consumed — intentional admin bypass)
   - `/shepherd unlink` — force-unlinks the looked-at villager, cleans all matching staves across online players
   - `/shepherd give <staff|charge>` — gives item directly

### Important Paper behavior

`PlayerInventory#getContents()` returns `CraftItemStack` mirrors, not copies. Calling `setAmount(0)` on them directly modifies the inventory — no need to call `setItem()` afterward. This is how charge consumption works in `handleLink`.

Both `PlayerInteractEntityEvent` and `PlayerInteractEvent` guard against off-hand double-firing with `event.getHand() != EquipmentSlot.HAND`.

## Review process

`review.md` tracks versioned feedback between the user and Claude. Format:
- User tests in-game and fills in the current version's `### Bug` / `### Improvement` sections
- Claude implements the changes, checks off completed items `[x]`, bumps the version in `pom.xml` and `plugin.yml` is NOT version-tagged (stays `1.0.0`), adds the next version template with Claude's own suggestions at the top of the file
- The file uses `<!-- markdownlint-disable MD024 -->` to allow repeated section headings across versions
