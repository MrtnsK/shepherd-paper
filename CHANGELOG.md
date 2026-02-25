# Shepherd — Changelog

## v1.5.3

**Bug fixes**

- Freeze mid-air au redirect (bis) : suppression du snap téléport ; si `!isOnGround()` au moment du redirect, une wait-task attend l'atterrissage naturel (timeout 3s) avant de lancer `moveTo()`. Le runnable de monitoring n'incrémente `noPathTicks` que si `isOnGround()`, évitant de freeze le villageois en chute libre d'une falaise
- Villageois tourne en rond à destination : ajout d'un check de proximité dans le runnable (`distance <= 0.8 blocs`) ; freeze immédiat dès que le villageois est assez proche, sans attendre que `hasPath()` passe à false

## v1.5.2
**Bug fixes**
- Intermittent mid-air freeze : le compteur `noPathTicks` exige 2 checks consécutifs sans chemin avant de freeze, évitant les faux-négatifs de `hasPath()` pendant une recalculation
- Villager qui ignorait l'ordre : le `moveTo()` est réémis à chaque tick du runnable pour empêcher le brain AI natif (schedules, POI, lit) de reprendre la main

## v1.5.1
**Bug fixes**
- AI ne s'éteignait pas fiablement à l'arrivée : intervalle de polling réduit de 10 ticks (0.5s) à 2 ticks (0.1s)

**Improvements**
- Fichier `config.yml` avec `speed` (défaut: `1.0`) et `max-distance` (défaut: `20`) ; valeurs lues dynamiquement dans `handleRedirect`

## v1.4.0
**Improvements**
- Tab completion pour `/shepherd` : propose `link`, `unlink`, `give` au premier argument et `staff`, `charge` au second ; masquée sans `shepherd.admin`

## v1.3.1
**Bug fixes**
- Villager figé en l'air au redirect : snap au sol par descente bloc par bloc avant `moveTo()` si `!isOnGround()`

## v1.3.0
**Bug fixes**
- Version `plugin.yml` restée à `1.0.0` malgré les bumps de `pom.xml`

**Improvements**
- Permission `shepherd.use` (défaut: `true`) ajoutée dans `plugin.yml` ; vérifiée dans `StaffInteractListener` pour toutes les actions vanilla (link, redirect, unlink via staff)

## v1.2.0
**Improvements**
- `/shepherd link` : regarde un villageois, force le lien et donne un staff pré-linké à l'admin (sans consommer de charge — bypass intentionnel)
- `/shepherd give <staff|charge>` : donne l'item directement à l'admin
- `/shepherd unlink` : délie de force le villageois regardé et nettoie tous les staves en ligne
- Détection de blocage dans le `BukkitRunnable` : si `!hasPath()` et distance > 3 blocs du target, message envoyé au propriétaire
- Cleanup dans `onDisable()` : tous les `activePathTasks` sont annulés proprement

## v1.1.0
**Improvements**
- Staff non-stackable (`setMaxStackSize(1)`)
- AI du villageois freeze à l'activation (`setAI(false)`) et dégèle uniquement pendant un redirect
- Distance de redirect réduite de 100 à 20 blocs (2D)
- JAR de sortie versionné : `Shepherd-${project.version}.jar`

## v1.0.0
Implémentation initiale :
- Shepherd's Staff (blaze rod) craftable, identifié par PDC
- Shepherd Charge (fire charge) craftable, consommée au link
- Link (clic droit sur villageois) : consomme la charge, stocke l'owner UUID dans le PDC du villageois, glowing ON
- Redirect (clic droit sur un bloc) : `moveTo()` vers le bloc ciblé, BukkitRunnable re-freeze l'AI à l'arrivée
- Unlink (sneak + clic droit sur villageois) : libère le villageois, retire le glowing
- Détection de mort du villageois : nettoie le staff de tous les joueurs en ligne
- Protection multi-joueur : un villageois déjà linké ne peut pas être re-linké
