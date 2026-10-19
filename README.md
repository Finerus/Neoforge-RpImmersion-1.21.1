# RpImmersion

**RpImmersion** is a server-side companion mod for [RpEssentials](https://github.com/), designed for immersive Roleplay servers running Minecraft 1.21.1 on NeoForge. It gives server administrators fine-grained control over the atmosphere experienced by players — fog, camera effects, weather immersion, and a fully custom RP calendar — all configurable in real-time without restarts.

> Current version: **1.0.1**

---

## Requirements

| Dependency        | Version                | Side   | Required   |
|:------------------|:-----------------------|:-------|:-----------|
| Minecraft         | 1.21.1                 | Both   | ✅          |
| NeoForge          | 21.1.219+              | Both   | ✅          |
| RpEssentials      | 4.0.0+                 | Both   | ✅          |
| ImmersiveMessages | neoforge-1.21.1:1.0.18 | Client | ⬜ Optional |
| TxniLib           | neoforge-1.21.1-1.0.24 | Client | ⬜ Optional |

> ImmersiveMessages and TxniLib are only required on the **client** if you use `weatherMessageMode = IMMERSIVE` or `scheduleDeathHoursNotifyMode = IMMERSIVE`. The server runs fine without them, and clients without them will gracefully fall back to action bar display.

---

## Features

### Fog System

Server-controlled fog fully replaces vanilla fog for players when active. Everything is configured in `rpimmersion-fog.toml`.

- **Named presets** — define fog colors (RGB), start/end distances, and fade transition duration. Apply them anywhere.
- **Circular zones** — assign a preset to a zone defined by center coordinates and radius. Higher-priority zones win when overlapping.
- **Biome-based fog** — automatically apply a preset when a player enters a specific biome. Wildcard support (`minecraft:*`, `*:dark_*`).
- **RpEssentials zone overrides** — piggyback on zones already defined in RpEssentials's named zone system. No need to redefine coordinates.
- **Day/Night variants** — every zone, biome, and global preset supports separate `dayPreset` and `nightPreset`. Transitions are animated.
- **Global fog** — a fallback preset applied to all players when no zone or biome matches.
- **World border fog** — a dedicated high-priority preset activates automatically when a player steps outside the world border. Useful for dramatic boundary effects.
- **Per-player override** — staff can instantly set a specific fog for one player via command, overriding all other rules.
- **Smooth transitions** — all fog changes ease in and out with a configurable duration. No abrupt jumps.
- **Optimized sync** — the server only sends a packet when the fog state actually changes for a player (checked every N ticks, configurable). No network spam.
- **Dimension fog**: assign a preset to a whole dimension (Nether, End, custom dimensions).
- **Biome tags**: biome fog accepts tags such as `#minecraft:is_forest` in addition to wildcards.
- **Zone dimension**: a circular zone can be restricted to one dimension.
- **Config warnings**: unknown preset names are reported in the log at every reload.

---

### Weather Immersion

Enhances Minecraft's weather system with atmosphere and control. Configured in `rpimmersion-weather.toml`.

- **Weather fog integration** — CLEAR, RAIN, and THUNDER each map to a fog preset. Applied as a low-priority fallback when no zone or biome fog is active, creating a natural weather atmosphere.
- **Lightning frequency control** — multiply or reduce the rate of lightning strikes during thunderstorms. Extra bolts are placed randomly near online players within a configurable radius.
- **Camera shake on lightning** — when lightning strikes, nearby players feel a camera shake proportional to their distance from the bolt. Close strike = strong shake, distant strike = subtle shake. The effect uses a smooth decay envelope.
- **Mainstream mod compatibility** — shake is applied *additively* to camera angles (not a replacement), so it stacks cleanly with Sodium, Embeddium, Optifine, Oculus, and any other mod that touches the camera. Never causes conflicts.
- **Client shake override** — the server can allow clients to reduce or disable camera shake via their own client config (Options → Mods). The server can also lock it, enforcing the configured intensity for all players.
- **Automatic weather cycle** — define a sequence of weather steps (CLEAR → RAIN → THUNDER → CLEAR…) with durations in real-time minutes. The server cycles through them automatically.
- **Narrative messages** — when weather changes, a configurable message is broadcast to all players. Supports CHAT, ACTION_BAR, and IMMERSIVE (with ImmersiveMessages) display modes.

---

### RP Calendar

A fully persistent, server-driven roleplay calendar. Configured in `rpimmersion-calendar.toml`.

- **Completely custom structure** — define your own month names, number of months, days per month (uniform or per-month), and optionally day-of-week names.
- **Configurable time ratio** — control how fast RP time passes relative to real server time. `ticksPerRpMinute` × `rpMinutesPerHour` × `rpHoursPerDay` gives you full control.
- **Persistent** — the current date and pause state are saved in `world/data/rpimmersion/calendar.json`. The calendar survives server restarts.
- **Pauseable** — staff can freeze the calendar in place. The date is still displayed (with a configurable pause indicator), but time stops accumulating.
- **Flexible display** — show the date as a HUD overlay, in the action bar, both, or nowhere. The format string accepts any combination of `{day}`, `{dayName}`, `{month}`, `{monthName}`, `{year}`, `{hour}`, `{minute}` placeholders.
- **Live commands** — set the date to any value, advance it by N minutes, pause/resume, or force a sync to all players — all without restarting.
- **Configurable HUD**: position (five anchors), scale and background, all decided by the server and sent to the clients.
- **Date events**: broadcast a message when the calendar reaches a given date (holidays, festivals). Every display mode is supported, including `IMMERSIVE:presetName`.
- **Minecraft day sync**: the RP hour follows the Minecraft time of day, the RP day advances with each Minecraft day. `/rpimmersion calendar set` and `advance` move the Minecraft time too.

---

### HUD Control and Camera

Server-driven interface control, configured in `rpimmersion-hud.toml` and synced to every client at login and on reload.

- **Hidden HUD elements**: hide the crosshair, hotbar, health, armor, food, air, experience, jump bar, vehicle health, item name, effects, boss bars, scoreboard, tab list, subtitles and the F3 debug screen (no more coordinates metagaming).
- **First person lock**: disable the F5 camera so every player keeps a first person view.
- **Staff exemption**: optionally let staff members keep the full HUD and camera.
- **Live commands**: `/rpimmersion hud hide|show <element>` writes to the config and syncs instantly.
- **Staff camera shake**: `/rpimmersion shake <targets> <intensity> <seconds>` for earthquakes, explosions and other narrative events.

## Commands

All commands require staff permission (as defined by RpEssentials).

### Fog
| Command                                                | Description                                                                           |
|:-------------------------------------------------------|:--------------------------------------------------------------------------------------|
| `/rpimmersion fog set global <preset\|clear>`          | Apply a fog preset to all players, or clear the global override.                      |
| `/rpimmersion fog set player <player> <preset\|clear>` | Apply a fog preset to one player specifically.                                        |
| `/rpimmersion fog clear global`                        | Remove the global fog override.                                                       |
| `/rpimmersion fog clear player <player>`               | Remove the per-player fog override.                                                   |
| `/rpimmersion fog list`                                | List all loaded fog presets with their parameters.                                    |
| `/rpimmersion fog status`                              | Display the current fog state (global override, world border, zone and biome counts). |
| `/rpimmersion fog reload`                              | Reload fog config from disk and re-sync all players.                                  |

### Weather
| Command                                           | Description                                     |
|:--------------------------------------------------|:------------------------------------------------|
| `/rpimmersion weather set <CLEAR\|RAIN\|THUNDER>` | Force a specific weather state on the server.   |
| `/rpimmersion weather status`                     | Show the current weather and active fog preset. |
| `/rpimmersion weather reload`                     | Reload weather config from disk.                |

### Calendar
| Command                                                          | Description                                                   |
|:-----------------------------------------------------------------|:--------------------------------------------------------------|
| `/rpimmersion calendar status`                                   | Display the current RP date, time, and pause state.           |
| `/rpimmersion calendar pause <true\|false>`                      | Pause or resume the calendar.                                 |
| `/rpimmersion calendar set <year> <month> <day> [hour] [minute]` | Set the calendar to a specific date and time.                 |
| `/rpimmersion calendar advance <minutes>`                        | Advance (or rewind if negative) the calendar by N RP minutes. |
| `/rpimmersion calendar reload`                                   | Force a sync of the current date to all connected players.    |

### General
| Command                                              | Description                                                               |
|:-----------------------------------------------------|:--------------------------------------------------------------------------|
| `/rpimmersion reload`                                | Reload fog, weather, calendar and HUD settings and re-sync every player.  |
| `/rpimmersion shake <targets> <intensity> <seconds>` | Trigger a camera shake. Intensity 0.0 to 1.0, duration 0.1 to 30 seconds. |

### HUD
| Command                           | Description                                                          |
|:----------------------------------|:---------------------------------------------------------------------|
| `/rpimmersion hud hide <element>` | Hide a HUD element for every player (enables HUD control if needed). |
| `/rpimmersion hud show <element>` | Show a HUD element again.                                            |
| `/rpimmersion hud status`         | Display the current HUD control state.                               |
| `/rpimmersion hud reload`         | Re-send the HUD settings to every player.                            |

---

## Configuration

Three server-side config files are generated automatically under `config/rpimmersion/` on first launch. One client-side config is generated in the standard NeoForge client config location.

| File                        | Contents                                                                                           |
|:----------------------------|:---------------------------------------------------------------------------------------------------|
| `rpimmersion-fog.toml`      | Presets, zones, biomes, RpEssentials overrides, world border fog, day/night settings, optimization |
| `rpimmersion-weather.toml`  | Weather fog mapping, lightning frequency & shake, auto cycle, narrative messages                   |
| `rpimmersion-calendar.toml` | Month/day structure, time ratio, start date, display format                                        |
| `rpimmersion-client.toml`   | Client-side shake intensity override, fog disable toggle                                           |
| `rpimmersion-hud.toml`      | Hidden HUD elements, staff exemption, first person lock                                            |

All server configs are reloaded automatically when the file changes on disk, or manually with `/rpimmersion reload` (or the per-module reload commands). No server restart required.

### Fog Preset Format

```toml
# name;red;green;blue;start;end;transitionSeconds
presets = [
    "night_fog;0.05;0.05;0.1;5.0;30.0;3.0",
    "morning_mist;0.7;0.75;0.8;2.0;20.0;5.0",
    "border_void;0.0;0.0;0.0;1.0;8.0;2.0"
]
```

### Fog Zone Format

```toml
# zoneName;centerX;centerZ;radius;preset;priority[;dayPreset;nightPreset]
zones = [
    "dark_woods;100;-200;80;cursed_forest;10;morning_mist;night_fog"
]
```

### Weather Fog Format

```toml
# weatherType;fogPreset
weatherFogPresets = [
    "RAIN;morning_mist",
    "THUNDER;purge"
]
```

### Calendar Structure Example

```toml
monthNames    = ["Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                 "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"]
daysPerMonth  = [30]   # A single value applies to all months
dayNames      = ["Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"]
dateFormat    = "§7{dayName} {day} {monthName}, An {year} — {hour}h{minute}"
```

---

## Data Storage

| File                                   | Contents                        |
|:---------------------------------------|:--------------------------------|
| `world/data/rpimmersion/calendar.json` | Current RP date and pause state |

---

## Technical Notes

- **Fog transitions** use a fixed start/target interpolation model. The "from" value is captured at the moment a new fog is set, ensuring a clean linear ease-in/out regardless of how many transitions overlap.
- **Camera shake** adds delta values to the existing camera angles rather than replacing them, making it additive and fully compatible with Sodium, Embeddium, Optifine, Oculus, and any other rendering mod.
- **No continuous packet spam** — fog state is recalculated every N ticks (configurable, default 3 seconds) per player, and a packet is only sent if the resolved state differs from what was last sent.
- **RpEssentials integration** reads named zones through cached reflection, and uses `RpEssentialsPermissions`, `RpEssentialsScheduleManager` and `ImmersivePresetHelper` directly. A missing or incompatible method never crashes the tick: the calls are guarded.
- **Client sync**: HUD, camera lock and override permissions travel in a single `ClientSettingsPacket`. The calendar packet carries its own display mode and HUD options, the client never reads server config files.