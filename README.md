<div align="center">

# 📍 LastLocation

**Auto-save logout position & teleport back — for Paper servers with lobby architecture.**

[![Paper 1.21+](https://img.shields.io/badge/Paper-1.21%2B-232a2d?style=for-the-badge&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAOCAYAAAAfSC3RAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAABfSURBVDhPY/wPBAxUBExQmmpg1EZCAGsjIyPjfxAbGUNNJASgNh4A4v9QTBBAbjshG8lxI1V8SsgNVA0OWjkVn+epFhyj0UE1G6kaHOSkkZL4pLqNpOQnsgADAwMAIR0XB2fMoFoAAAAASUVORK5CYII=&labelColor=1a1a2e)](https://papermc.io)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7f52ff?style=for-the-badge&logo=kotlin&logoColor=white&labelColor=1a1a2e)](https://kotlinlang.org)
[![Multiverse-Core](https://img.shields.io/badge/Multiverse--Core-5.0-4caf50?style=for-the-badge&labelColor=1a1a2e)](https://github.com/Multiverse/Multiverse-Core)
[![License: MIT](https://img.shields.io/badge/License-MIT-f5c542?style=for-the-badge&labelColor=1a1a2e)](LICENSE)
[![H2 Database](https://img.shields.io/badge/H2-Embedded-0288d1?style=for-the-badge&labelColor=1a1a2e)](https://h2database.com)

---

*A lightweight Paper plugin that saves players' exact positions when they disconnect or leave gameplay worlds,<br>and lets them return with a single command.*

</div>

## ✨ Overview

LastLocation solves a common problem on multi-world servers: when a player logs out in a gameplay world and later reconnects, they typically spawn in the lobby. This plugin records their coordinates and allows them to return to exactly where they left off.

### Dual-Slot Location System

The plugin uses **two independent storage slots** to track why a player left their gameplay world:

| Slot | Trigger | Example |
|---|---|---|
| 🔴 **Disconnect** | Player quits the server while in a save-world | Alt+F4, `/quit`, kicked |
| 🔵 **World Change** | Player leaves a save-world to a lobby/feature world | Portal to lobby, death respawn, teleport command |

This dual-slot system ensures players can always return to their gameplay world — even after going to the lobby and back multiple times.

### How It Works

```
┌─────────────┐    quit/kick     ┌──────────────────┐
│             │ ───────────────► │ Disconnect Slot 🔴│
│  Gameplay   │                  └──────────────────┘
│   World     │    tp/death      ┌──────────────────┐
│ (save-world)│ ───────────────► │ World Change Slot🔵│
│             │                  └──────────────────┘
└─────────────┘
                                         │
                    /lastlocation         ▼
               ┌──────────────────────────────────────┐
               │  Priority 1: 🔵 World Change slot    │
               │  Priority 2: 🔴 Disconnect slot      │
               │  Priority 3: 🏠 Default world spawn  │
               └──────────────────────────────────────┘
```

**Key rule:** After teleporting from a disconnect record, **all records are cleared** so the next time the player leaves the gameplay world, a fresh world-change record is created.

### World Types

| Type | Purpose | Examples |
|---|---|---|
| `save-worlds` | Gameplay worlds — positions auto-saved on leave | `world`, `world_nether`, `world_the_end` |
| `command-worlds` | Lobby/Hub — where `/lastlocation` can be used | `lobby` |
| `feat-worlds` | Feature worlds — treated like command-worlds | `afk`, `crate`, `shop`, `pvp_arena` |
| `default-world` | Fallback spawn when no saved location exists | `qhuyy` |

### Requirements

| Dependency | Required | Purpose |
|---|---|---|
| Paper 1.21+ | Yes | Server platform |
| Multiverse-Core 5.0 | Yes | World management and safe teleportation |

---

## 🔧 Commands

The plugin provides a single command with multiple forms:

### `/lastlocation`

| Usage | Description |
|---|---|
| `/lastlocation` | Teleport yourself to your saved location |
| `/lastlocation <player>` | Teleport another online player to their saved location |
| `/lastlocation reload` | Reload `config.yml` and `messages.yml` |

> **Alias:** `/ll`

### Behavior Details

**Self-teleport** (`/lastlocation` with no arguments):
- Must be executed by a player (not console).
- The player must be in one of the worlds listed in `command-worlds` or `feat-worlds`. If both lists are empty, the command can be used from any world.
- Teleport priority: world-change location → disconnect location → default spawn.
- After a successful teleport, the used record is deleted.

**Teleport others** (`/lastlocation <player>`):
- Can be executed by a player or console.
- The target must be online.
- Does not enforce the world restriction on the executor.
- Follows the same priority-based logic as self-teleport.

**Reload** (`/lastlocation reload`):
- Reloads both `config.yml` and `messages.yml` without restarting the server.

---

## ⚙️ Configuration

### config.yml

```yaml
# Worlds where the player's logout position will be saved.
save-worlds:
  - world
  - world_nether
  - world_the_end

# Worlds where the /lastlocation command can be used.
command-worlds:
  - lobby

# Feature worlds — treated like command-worlds.
# World change from save-world to feat-world will also save location.
feat-worlds:
  - afk
  - crate
  - shop
  - pvp_arena

# Default fallback world.
default-world: qhuyy
```

| Key | Type | Description |
|---|---|---|
| `save-worlds` | String list | World names where positions are recorded. Case-insensitive. |
| `command-worlds` | String list | World names where `/lastlocation` can be executed. |
| `feat-worlds` | String list | Feature worlds — also allow `/lastlocation` and trigger world-change saves. |
| `default-world` | String | Fallback world. Players with no valid saved location are sent to its spawn. |

### messages.yml

All messages use [MiniMessage](https://docs.advntr.dev/minimessage/format.html) format and support placeholder tags. The file is generated on first run and can be edited freely.

| Key | Placeholders | When it is shown |
|---|---|---|
| `no-permission-reload` | — | Player lacks `arclyx0.admin` |
| `reload-success` | — | Config reloaded successfully |
| `no-permission-teleport-others` | — | Player lacks `arclyx0.teleport.others` |
| `no-permission-teleport-self` | — | Player lacks `arclyx0.teleport.self` |
| `console-must-specify-player` | `<label>` | Console runs `/lastlocation` without a player name |
| `player-not-online` | `<player>` | Target player is not online |
| `wrong-world` | `<worlds>` | Player is not in an allowed world |
| `default-world-not-found` | `<world>` | The configured `default-world` does not exist |
| `teleport-success` | — | Player teleported to their saved location |
| `teleport-default-spawn` | — | Player teleported to default world spawn |
| `teleport-failed` | — | Self-teleport failed |
| `teleport-others-success` | `<player>` | Teleported another player to saved location |
| `teleport-others-default-spawn` | `<player>` | Teleported another player to default spawn |
| `teleport-others-failed` | `<player>` | Teleporting another player failed |
| `save-failed` | `<player>` | Failed to save a player's location |
| `missing-dependency` | `<dependency>` | A required plugin dependency is missing |
| `location-saved-temp` | — | Player's position saved when leaving a save-world |

---

## 🔑 Permissions

| Permission | Default | Description |
|---|---|---|
| `arclyx0.teleport.self` | `true` | Use `/lastlocation` to teleport yourself |
| `arclyx0.teleport.others` | `op` | Use `/lastlocation <player>` to teleport another player |
| `arclyx0.admin` | `op` | Use `/lastlocation reload`. Inherits all other permissions. |

---

## 💾 Data Storage

Player location data is stored in a local **H2 embedded database** (`plugins/LastLocation/playerdata.mv.db`). No external database setup is required — the file is created automatically on first run.

The database contains two tables:

| Table | Purpose |
|---|---|
| `disconnect_locations` | Positions saved when a player quits the server in a save-world |
| `world_change_locations` | Positions saved when a player leaves a save-world to a lobby/feature world |

Runtime flags (`disconnect_flag`, `world_change_flag`) are held in memory only and reset on server restart. After a restart, players without flags will be sent to default spawn.

---

## 🏗️ Building

```bash
./gradlew build
```

The compiled JAR is output to `build/libs/LastLocation-v1.0.0.jar`.

---

## 📄 License

This project is released under the [MIT License](LICENSE), which permits copying, modification, use, and distribution.
