# Changelog — RpImmersion

## [1.1.0] RpImmersion 

Requires RpEssentials 5.0.1 or newer.

### Added

**Atmospheric events (rpimmersion-events.toml):**
* Events are defined in one line each: duration, fog preset, forced weather, screen tint, darkness overlay, fade time, camera shake, ambient particle, start and end messages.
* Five default events: bloodmoon, storm_omen, ash_rain, dense_fog, silent_sky.
* Staff commands: `/rpimmersion event start <id> [minutes]`, `stop`, `status`, `list`, `reload`.
* Optional random events (disabled by default): chance rolled once per real minute, restricted to day, night or any time, with a configurable cooldown between events.
* Event fog takes priority over zones, biomes, global and weather fog.
* Four new default fog presets used by the events: blood_sky, green_sky, ash_fall, dense_fog. Existing rpimmersion-fog.toml files keep their old defaults, add these presets by hand.

**Player states (rpimmersion-effects.toml):**
* Temperature: cold biomes tint the screen, narrow the field of view, make hands shiver and show breath clouds. Hot biomes add a warm tint and a slow sway.
* Low health: red pulsing vignette, breathing camera and a speed reduction that grows as health drops.
* Fatigue: builds up when a player stays awake too long (vanilla time since last rest). Dark vignette, slower movement, reduced damage and occasional micro sleeps.
* Respawn: players wake up immobile with a fade in from black, then weakened for a configurable time.
* Exertion: long sprints and sprint jumps build exhaustion, with visible breath (also for nearby players), then slowness and weakness. Raise `jumpExertion` to punish bunny hopping harder.
* Micro animations: stumbling on soft blocks (snow, mud, configurable list) and a damped camera impact after a fall.
* Slowness and weakness use transient attribute modifiers, so they are never saved with the player.
* Staff commands: `/rpimmersion effects status`, `reload`, `enable <true|false>`, `particles <true|false>`, `reset <player>`.

### Potion particles
* New option `hidePotionParticles` hides the particles of every potion effect for everyone, to avoid HRP caused by visible effects. Applies as effects are applied or refreshed.

### Config Manager integration
* RpImmersion adds 6 purple tabs to the RpEssentials Config Manager GUI: Fog, Weather, Calendar, HUD, Effects and Events.
* Changes applied from the GUI run the same reload as `/rpimmersion reload`, so the managers are reloaded and every player is re-synced.

### Client options
* New client option `effectsIntensityMultiplier` (0.0 to 1.0) scales camera movement and screen overlays, useful against motion sickness. Only active if the server allows it with `allowClientEffectsOverride`.

###  Changed
* Screen overlays are blended in priority order: low health dims the temperature tints, and the red and dark vignettes are merged into a single one.
* Camera effects are additive, like the lightning shake, so they stack with other camera mods.
* RpEssentials dependency raised to 5.0.1.
* `/rpimmersion reload` and config file changes now also reload the events and re-sync the effects settings.

### Known limitations
* Real desaturation and heat haze need a post processing shader, they are approximated with tints and a slight camera roll.
* Shader packs (Iris, Oculus) do not affect server fog, as before.

## 1.0.1

### Added
- HUD control: hide any vanilla HUD element for all players, including the F3 debug screen (`rpimmersion-hud.toml`, `/rpimmersion hud`).
- First person lock (`forceFirstPerson`) and optional staff exemption.
- Calendar HUD options: position, scale and background, sent by the server.
- Calendar date events: broadcast a message when a given RP date is reached.
- Fog per dimension, biome tags (`#minecraft:is_forest`), optional dimension for circular zones.
- Day/night fog changes now use `dayNightTransitionSeconds`.
- `/rpimmersion reload` and `/rpimmersion shake`.
- Server configs reload automatically when the file changes on disk.
- Client config screen available in Options, Mods.
- New option `allowClientFogOverride` (the client `disableServerFog` option could never work before).

### Fixed
- Commands now work from the console, command blocks and rcon.
- Weather fog was never applied with the default config.
- `enableFogSystem` and `worldBorderFogPriority` are now honored.
- `/rpimmersion fog set global` was ignored inside zones and biomes.
- An unknown preset name no longer blocks lower priority fog layers, a warning is logged instead.
- RpEssentials zone overrides now respect their priority and the zone dimension.
- Fog flash at login, and wrong target value at the end of a fog transition.
- Lightning camera shake only triggered when an entity was hit, once per entity, in every dimension.
- `lightningFrequencyMultiplier` below 1.0 now reduces lightning.
- Rain effects no longer apply in other dimensions, nor to spectators.
- Weather cycle: first step applied at start, step duration respected, rain no longer stuck on clients after a clear step.
- Calendar `set` and `advance` were overwritten in Minecraft day sync mode.
- Calendar paused whenever the RpEssentials schedule hours were closed, even with the schedule disabled.
- Calendar display mode was read from a client copy of the server config, it is now sent by the server.
- Calendar action bar no longer overrides action bar messages from other systems.
- Calendar `set` validates the date, and a huge year no longer freezes the server.
- Calendar file is saved atomically and synchronously on shutdown.
- Messages: `&` no longer eats the next character, `TITLE` and `IMMERSIVE:presetName` modes are supported.
- Event handlers and commands were registered twice.

### Changed
- Network protocol version bumped to 2: update the server and the clients together.
- `displayMode` is validated (`HUD`, `ACTION_BAR`, `BOTH`, `NONE`).
- **RpEssentials** is now required with the **5.0.0** version (and higher).

### Personal notes
Hi, long time no see here, isn't it? This is just little update to add and fix things.
I plan to add more things now that RpEssentials is almost done. See you soon!

## [1.0.0]

1st Release, not in beta anymore!

### Calendar

- **New**: `pauseWhenServerClosed` option — the calendar automatically pauses when the RpEssentials schedule marks the server as closed, and resumes when it opens.
- **New**: `syncDaylightCycle` option — the `doDaylightCycle` gamerule is automatically managed based on the calendar pause state.
- **New**: `minecraftDaySync` option — the calendar reads `getDayTime()` directly instead of running its own tick logic. 1 MC day = 1 calendar day, hours and minutes reflect the current MC time in real time.
- **Fixed**: calendar was no longer visible on the client side — sync was only triggered once per RP hour instead of once per RP minute.
- **Fixed**: `doDaylightCycle` was never re-enabled after unpausing — `updateDaylightCycle` was not called on server start.
- **Fixed**: `setDate` and `advance` in `minecraftDaySync` mode were overwritten on the next tick by the `getDayTime()` calculation.
- **Fixed**: `updateSchedulePause` was never called — `CalendarServerEventHandler` was missing the `scheduleCheckTick` counter.
- **Changed**: default values for `ticksPerRpMinute` (20) and `rpMinutesPerHour` (50) — 20 × 50 × 24 = 24,000 ticks = exactly 1 MC day per calendar day.

### Weather

- **New**: extra lightning now always strikes at the surface (`MOTION_BLOCKING_NO_LEAVES`) — no more bolts spawning underground or beneath players in caves.
- **New**: `rainEffectsEnabled` option and `rainEffects` list — configurable potion effects applied to players exposed to rain or thunderstorms. Configurable per effect, level, weather type, and sky exposure.
- **Fixed**: weather fog was never applied with the default priority (`zonePriorityOverGlobal = true`) — `resolveWeather()` is now called as a last resort in both priority branches.
- **Fixed**: rain effects are now read directly from config on each tick — no reload needed, changes to the TOML file are picked up immediately.
- **Fixed**: fog exit transitions were using vanilla distances frozen at the moment the transition started instead of tracking them in real time.

### Fog

- **New**: `overrides` field on fog presets — each preset now declares which vanilla fog types it replaces, as a comma-separated list: `TERRAIN`, `SKY`, `WATER`, `LAVA`, `POWDER_SNOW`, or `ALL`. Defaults to `TERRAIN` if absent. The flag is transmitted in `FogSyncPacket` and evaluated client-side before any override is applied.
- **Fixed**: vanilla distances are now sampled every frame regardless of whether the server fog is active, fixing exit transitions toward a different biome or time of day.

### Config
- Changed a lot of default values in config files to match with new options and mod wanted behavior.

### Known issues

- Rain and thunder fogs are known to not always render.

---

## [0.0.2]

- Initial public release.
- Fog system: named presets, circular zones, biome-based fog with wildcard support, RpEssentials zone integration, day/night cycle, world border fog, per-player override, smooth transitions.
- Weather system: weather-based fog, lightning frequency control, camera shake, auto cycle, narrative messages.
- RP calendar: fully custom structure, configurable time ratio, persistence, pause, HUD/action bar display, live commands.
