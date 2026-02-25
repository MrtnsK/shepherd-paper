# Shepherd — Review History

<!-- markdownlint-disable MD024 -->

## v1.6.0 — Claude's turn

### Bug

(aucun trouvé)

### Improvement

### Notes

---

## v1.5.2

### Bug

- [x] **Freeze mid-air intermittent** — à 2L ticks, `hasPath()=false` momentané pendant une recalculation de chemin pouvait déclencher un freeze prématuré. Fix : compteur `noPathTicks`, on ne freeze qu'après 2 checks consécutifs sans chemin.

- [x] **Villager ignore l'ordre** — le brain AI du villageois (schedules, POI, lit) peut écraser le `moveTo()` initial après quelques ticks. Fix : dans la branche `hasPath()=true` du runnable, on réémet `moveTo(targetLocation, speed)` à chaque tick pour maintenir notre goal prioritaire.

### Improvement

### Notes

---

## v1.5.1

### Bug

- [x] **AI ne s'éteint pas fiablement à l'arrivée** — le polling toutes les 10 ticks (0.5s) laissait une fenêtre où le villageois pouvait s'éloigner après avoir atteint sa destination. Fix : intervalle réduit de `10L` à `2L` ticks (0.1s).

### Improvement

- [x] **Fichier de configuration** — `config.yml` créé avec `speed: 1.0` et `max-distance: 20`. `saveDefaultConfig()` appelé dans `onEnable()`. `handleRedirect` lit les valeurs via `plugin.getConfig()`.

### Notes

---

## v1.4.0

### Bug

(aucun trouvé)

### Improvement

- [x] **Tab completion pour `/shepherd`** — `ShepherdCommand` implémente maintenant `TabCompleter`. `/shepherd <tab>` propose `link`, `unlink`, `give` ; `/shepherd give <tab>` propose `staff`, `charge`. Suggestions filtrées par ce qui est déjà tapé, et masquées aux joueurs sans `shepherd.admin`.

### Notes

Everything work as intended.

---

## v1.3.1

### Bug

- [x] **Villager frozen mid-air** — quand `setAI(false)` intervient pendant que le villageois monte/descend, il reste suspendu. `moveTo()` échoue depuis une position flottante. Fix : dans `handleRedirect`, si `!isOnGround()`, on descend bloc par bloc jusqu'au premier bloc solide et on `teleport()` le villageois au sol avant d'appeler `moveTo()`.

### Improvement

### Notes

---

## v1.2.0

### Bug

- [x] **`plugin.yml` version pas mise à jour** — restée à `1.0.0` alors que `pom.xml` était à `1.2.0`. Corrigé.

### Improvement

- [rejected] **`/shepherd link` ne consomme pas de charge** — la commande admin crée un lien sans charge. C'est intentionnel pour le testing, mais si on veut être cohérent avec le gameplay, il faudrait soit consommer une charge, soit documenter que c'est une commande admin qui bypass volontairement. -> c'est une commande admin donc c'est ok

- [x] **Amélioration des permissions** — ajout de `shepherd.use` (défaut: `true`) dans `plugin.yml` + vérification dans `StaffInteractListener` pour les actions vanilla (link/redirect/unlink via staff). Les commandes `/shepherd` restent `shepherd.admin`.

### Notes

---

## v1.1.0

### Bug

- [x] ~~**Charge not consumed**~~ — INVALIDÉ : confirmé fonctionnel en jeu. `getContents()` retourne des `CraftItemStack` miroirs en Paper, pas des copies. `setAmount(0)` modifie bien l'inventaire réel.

### Improvement

- [x] **Cleanup dans `onDisable()`** — implémenté dans `Shepherd.java`
- [x] **Add staff and charge to creative menu** — implémenté via `/shepherd give <staff|charge>` (admin)
- [x] **Display a message if after a while the villager can't reach the destination** — détection de blocage dans le `BukkitRunnable` : si `!hasPath()` et distance > 3 blocs du target, message envoyé au propriétaire
- [x] **Add a link command** — `/shepherd link` : regarde un villageois, donne un staff pré-linké sans consommer de charge (admin)

### Notes
