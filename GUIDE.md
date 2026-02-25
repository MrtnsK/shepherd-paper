# Shepherd — Beginner's Guide

Ce guide explique comment le code fonctionne de l'intérieur, pour pouvoir le modifier ou l'étendre en toute confiance.

---

## Comment Paper reconnaît nos items custom

Minecraft ne permet pas de créer de "vrais" nouveaux items — tout repose sur des items existants qu'on personalise. Le problème : comment distinguer un `BLAZE_ROD` normal d'un Shepherd's Staff ?

La réponse : **PersistentDataContainer (PDC)**. C'est un système de métadonnées clé/valeur attaché aux entités, items, blocs, etc., qui persiste après un redémarrage serveur.

On utilise 4 clés, toutes définies comme constantes dans `Shepherd.java` :

```
shepherd:staff          → sur l'ItemStack, byte 1 = "c'est un staff"
shepherd:charge         → sur l'ItemStack, byte 1 = "c'est une charge"
shepherd:linked_villager → sur l'ItemStack du staff, UUID du villageois linké
shepherd:owner          → sur l'entité Villager, UUID du joueur propriétaire
```

Pour vérifier si un item est un staff :
```java
// StaffItem.java
meta.getPersistentDataContainer().has(Shepherd.KEY_STAFF, PersistentDataType.BYTE);
```

C'est tout. Pas besoin de vérifier le nom ou le type de matériau.

---

## Le cycle de vie d'un villageois

Un villageois passe par ces états :

```
[Libre]
   │ joueur clic droit avec staff + charge
   ▼
[Linké]  ← shepherd:owner dans PDC du villageois
          ← shepherd:linked_villager dans PDC du staff
          ← setGlowing(true), setAI(false) → figé sur place
   │ joueur clic droit sur un bloc
   ▼
[En chemin]  ← setAI(true), moveTo(targetLocation)
              ← BukkitRunnable actif → surveille l'arrivée
   │ BukkitRunnable détecte !hasPath() × 2
   ▼
[Linké]  ← setAI(false) → re-figé à la destination
   │ sneak + clic droit sur le villageois
   ▼
[Libre]  ← PDC nettoyé, setGlowing(false), setAI(true)
```

Si le villageois meurt à n'importe quelle étape → `VillagerDeathListener` nettoie tout.

---

## Le problème de l'AI et le BukkitRunnable

`setAI(false)` gèle complètement l'entité : plus de mouvement, plus de gravité. C'est ce qu'on veut quand le villageois attend. Mais pour le faire marcher, il faut obligatoirement `setAI(true)`.

Le problème : avec l'AI activée, le brain interne du villageois (schedules, POI, lit...) peut reprendre la main et ignorer notre `moveTo()`.

La solution dans `handleRedirect` :
1. `setAI(true)` pour activer la navigation
2. `moveTo(targetLocation, speed)` pour donner l'ordre initial
3. Lancer un `BukkitRunnable` toutes les 2 ticks qui :
   - **Réémet `moveTo()`** tant que le chemin est actif → empêche le brain de prendre la main
   - **Compte les ticks sans chemin** (`noPathTicks`) → quand `hasPath()` retourne `false` 2 fois de suite, le villageois est arrivé (ou bloqué), on appelle `setAI(false)`

Le double-check sur `noPathTicks` est important : le pathfinder peut brièvement retourner `false` entre deux segments de chemin. Sans ce compteur, on freezerait le villageois en plein milieu de son trajet.

---

## Où chaque fichier intervient

### `Shepherd.java` — Point d'entrée et état partagé
Enregistre les listeners et la commande dans `onEnable()`. Contient la `Map<UUID, BukkitTask> activePathTasks` — une référence à la tâche de surveillance active pour chaque villageois en chemin. Cette map est accédée depuis plusieurs classes via `JavaPlugin.getPlugin(Shepherd.class)`.

### `StaffItem.java` / `ChargeItem.java` — Factories d'items
Méthodes statiques uniquement. `create()` fabrique l'item avec ses métadonnées. `isStaff()` / `isCharge()` identifient un item via PDC. `setLinkedVillager()` / `getLinkedVillager()` / `removeLinkedVillager()` gèrent le lien stocké dans le PDC du staff (+ glint visuel).

### `RecipeManager.java` — Recettes de craft
Enregistre les deux `ShapedRecipe` au démarrage. Si tu veux changer les ingrédients, c'est ici.

### `StaffInteractListener.java` — Logique principale
Deux events écoutés :
- `PlayerInteractEntityEvent` → clic sur un villageois → `handleLink` ou `handleUnlink`
- `PlayerInteractEvent` → clic sur un bloc → `handleRedirect`

Les deux vérifient `event.getHand() != EquipmentSlot.HAND` en premier pour éviter le double-fire (Paper fire les events une fois par main).

### `VillagerDeathListener.java` — Nettoyage sur mort
Scanne tous les inventaires des joueurs en ligne pour trouver les staves liés au villageois mort et les délinker. Annule aussi la BukkitTask si elle était active.

### `ShepherdCommand.java` — Commandes admin
Implémente `CommandExecutor` et `TabCompleter` sur la même classe. Les trois sous-commandes (`link`, `unlink`, `give`) sont réservées à `shepherd.admin`. Le tab completion filtre les suggestions selon ce qui est déjà tapé.

---

## Les deux permissions

| Permission | Défaut | Rôle |
|---|---|---|
| `shepherd.use` | `true` (tout le monde) | Utiliser le staff en jeu (link, redirect, unlink) |
| `shepherd.admin` | `op` | Commandes `/shepherd` |

Si tu veux restreindre l'usage du staff à un groupe spécifique, mets `shepherd.use: false` dans ton plugin de permissions et accorde-la manuellement.

---

## Configuration (`config.yml`)

```yaml
speed: 1.0        # Vitesse du villageois en déplacement (1.0 = marche normale)
max-distance: 20  # Distance 2D max entre le joueur et le villageois pour un redirect
```

Les valeurs sont lues à chaque appel de `handleRedirect` via `plugin.getConfig().getDouble(...)`. Pour recharger la config sans redémarrer le serveur, ajoute `/shepherd reload` avec `reloadConfig()` dans `ShepherdCommand` si besoin dans le futur.

---

## Ajouter une nouvelle fonctionnalité — checklist

1. **Nouveau item custom** → créer une classe dans `items/` sur le modèle de `StaffItem` ou `ChargeItem`, ajouter une `NamespacedKey` dans `Shepherd.java`, enregistrer la recette dans `RecipeManager`
2. **Nouveau comportement sur interaction** → ajouter un `@EventHandler` dans un listener existant ou créer un nouveau listener dans `listeners/`, l'enregistrer dans `Shepherd#onEnable()`
3. **Nouvelle sous-commande admin** → ajouter un `case` dans `ShepherdCommand#onCommand()` et mettre à jour `onTabComplete()` + le `usage` dans `plugin.yml`
4. **Nouvelle valeur de config** → ajouter la clé dans `config.yml` avec sa valeur par défaut, la lire avec `plugin.getConfig().getX("key", default)`
