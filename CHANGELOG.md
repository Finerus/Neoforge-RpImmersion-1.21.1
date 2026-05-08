# Changelog — RpImmersion

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
