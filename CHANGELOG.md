# Changelog — RpImmersion

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
