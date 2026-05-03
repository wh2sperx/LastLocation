# LastLocation

A Paper plugin that automatically saves a player's exact position when they disconnect from designated worlds and lets them teleport back using a simple command. Designed for servers with a lobby/hub architecture where players travel between a central hub and gameplay worlds.

## Overview

LastLocation solves a common problem on multi-world servers: when a player logs out in a gameplay world and later reconnects, they typically spawn in the lobby. This plugin records their logout coordinates and allows them to return to exactly where they left off.

### How It Works

1. A player disconnects while in a world listed under `save-worlds`.
2. The plugin stores their exact coordinates, direction, and world name.
3. When the player reconnects (landing in the lobby), they run `/lastlocation` to teleport back.
4. After a successful teleport to the saved position, the record is cleared.
5. If no saved location exists, or the saved world is no longer available, the player is sent to the spawn point of the configured `default-world` instead. The saved data is preserved for future attempts.

### Requirements

| Dependency              | Required | Purpose                                  |
|-------------------------|----------|------------------------------------------|
| Paper 1.21+             | Yes      | Server platform                          |
| Multiverse-Core         | Yes      | World management and safe teleportation  |

## Commands

The plugin provides a single command with multiple forms:

### `/lastlocation`

| Usage                    | Description                                            |
|--------------------------|--------------------------------------------------------|
| `/lastlocation`          | Teleport yourself to your saved location               |
| `/lastlocation <player>` | Teleport another online player to their saved location |
| `/lastlocation reload`   | Reload `config.yml` and `messages.yml`                 |

Alias: /ll

### Behavior Details

**Self-teleport** (`/lastlocation` with no arguments):
- Must be executed by a player (not console).
- The player must be in one of the world's listed in `command-worlds`. If the list is empty, the command can be used from any world.
- If a saved location exists and its world is loaded, the player is teleported there and the saved record is deleted.
- If no saved location exists or the world is unavailable, the player is teleported to the `default-world` spawn point. The saved record is kept.

**Teleport others** (`/lastlocation <player>`):
- Can be executed by a player or console.
- The target must be online.
- Does not enforce the `command-worlds` restriction on the executor.
- Follows the same saved-location-or-default-spawn logic as self-teleport.

**Reload** (`/lastlocation reload`):
- Reloads both `config.yml` and `messages.yml` without restarting the server.

## Configuration

### config.yml

```yaml
# Worlds where the player's logout position will be saved.
# Only when a player disconnects in one of these worlds,
# their exact coordinates and look direction are recorded.
save-worlds:
  - world
  - world_nether
  - world_the_end

# Worlds where the /lastlocation command can be used.
# Players must be standing in one of these worlds to teleport back.
# Typically set to lobby/hub worlds.
# Leave empty to allow usage from any world.
command-worlds:
  - lobby

# Default fallback world.
# If a player has no saved location or the saved world
# is unavailable, they are teleported to this world's spawn point.
default-world: qhuyy
```

| Key              | Type         | Description                                                                                       |
|------------------|--------------|---------------------------------------------------------------------------------------------------|
| `save-worlds`    | String list  | World names where logout positions are recorded. Case-insensitive matching.                       |
| `command-worlds` | String list  | World names where `/lastlocation` can be executed. Empty list allows usage from any world.        |
| `default-world`  | String       | Fallback world name. Players with no valid saved location are sent to this world's spawn point.   |

### messages.yml

All messages use [MiniMessage](https://docs.advntr.dev/minimessage/format.html) format and support placeholder tags. The file is generated on first run and can be edited freely.

| Key                             | Placeholders   | When it is shown                                         |
|---------------------------------|----------------|----------------------------------------------------------|
| `no-permission-reload`          | --             | Player lacks `arclyx0.admin`                             |
| `reload-success`                | --             | Config reloaded successfully                             |
| `no-permission-teleport-others` | --             | Player lacks `arclyx0.teleport.others`                   |
| `no-permission-teleport-self`   | --             | Player lacks `arclyx0.teleport.self`                     |
| `console-must-specify-player`   | `<label>`      | Console runs `/lastlocation` without a player name       |
| `player-not-online`             | `<player>`     | Target player is not online                              |
| `wrong-world`                   | `<worlds>`     | Player is not in an allowed `command-worlds` world       |
| `default-world-not-found`       | `<world>`      | The configured `default-world` does not exist            |
| `teleport-success`              | --             | Player teleported to their saved location                |
| `teleport-default-spawn`        | --             | Player teleported to default world spawn                 |
| `teleport-failed`               | --             | Self-teleport failed                                     |
| `teleport-others-success`       | `<player>`     | Successfully teleported another player to saved location |
| `teleport-others-default-spawn` | `<player>`     | Successfully teleported another player to default spawn  |
| `teleport-others-failed`        | `<player>`     | Teleporting another player failed                        |
| `save-failed`                   | `<player>`     | Failed to save a player's location                       |
| `missing-dependency`            | `<dependency>` | A required plugin dependency is missing                  |

## Permissions

| Permission                  | Default  | Description                                                      |
|-----------------------------|----------|------------------------------------------------------------------|
| `arclyx0.teleport.self`     | `true`   | Use `/lastlocation` to teleport yourself                         |
| `arclyx0.teleport.others`   | `op`     | Use `/lastlocation <player>` to teleport another player          |
| `arclyx0.admin`             | `op`     | Use `/lastlocation reload`. Inherits all other permissions.      |

## Data Storage

Player location data is stored in a local H2 database file (`plugins/LastLocation/playerdata.mv.db`). No external database setup is required. The database is created automatically on first run.

## Building

```bash
./gradlew build
```

The compiled JAR is output to `build/libs/LastLocation-v1.0.0.jar`.

---

## License

This project is released under the MIT License, which permits copying, modification, use, and distribution.
