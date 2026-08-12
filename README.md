<div align="center">

<img src="src/main/resources/assets/mtss/icon.png" width="96" height="96" alt="MineTuner Statistics Server icon">

# MineTuner Statistics Server

**Customisable performance and stats HUD for Fabric.**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.2%2B-brightgreen.svg)](#requirements)
[![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-0.19.3%2B-lightgrey.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25%2B-orange.svg)](#requirements)

</div>

---

## What is MineTuner Statistics Server?

MineTuner Statistics Server (**MTSS**) replaces the vanilla debug screen with an in-game HUD that shows only the stats you care about.

MTSS is a **client-side-only** Fabric mod. It works on any server and installing it doesn't require anything on the server side.

---

## Features

- **Multiple stat lists** — create as many separate HUD panels as you want, each positioned, styled, and configured independently.
- **Drag-and-drop editor** (default keybind **H**) — drag lists anywhere on screen; right-click a list for its context menu; right-click empty space to create a new one.
- **Snap-to-center** while dragging with a visible guide line.
- **46 tracked stats** across four categories — performance, player, world, and position — including four optional hardware-sensor stats. See the [full stat table](#stat-reference) below.
- **Rolling history graphs** — Certain stats can render as a graph.
- **Template Mode** — an alternative to the fixed stat-list layout: write your own lines of text and interpolate stat tokens like `{tps}` and `{fps}` anywhere, mixing multiple stats and literal text freely.
- **Cloth Config screen** (`/mtss config`, or via ModMenu if installed) — a searchable, single-scroll settings screen covering every field in `mtss.json`, including tuning options with no control in the custom editor.
- **Optional LibreHardwareMonitor integration** — opt-in GPU temperature, GPU clock, GPU usage, and VRAM stats, polled from [LibreHardwareMonitor](https://github.com/LibreHardwaRemonitor/LibreHardwareMonitor)'s Remote Web Server on a background thread.

---

## Requirements

| Component | Version |
|---|---|
| Minecraft | ≥ 26.2 |
| Fabric Loader | ≥ 0.19.3 |
| Fabric API | required |
| Java | ≥ 25 |
| [Cloth Config API](https://www.curseforge.com/minecraft/mc-mods/cloth-config) | ≥ 26.2.155 (hard dependency) |
| [ModMenu](https://modrinth.com/mod/modmenu) | optional |

> MTSS is **client-side only**. Nothing needs to be installed on the server.

---

## Installation

1. Install [**Fabric Loader**](https://fabricmc.net/use/installer/).
2. Install [**Fabric API**](https://modrinth.com/mod/fabric-api) and [**Cloth Config API**](https://modrinth.com/mod/cloth-config) into your `mods` folder.
3. (Optional) Install [**ModMenu**](https://modrinth.com/mod/modmenu) for a GUI entry point into MTSS's settings.
4. Drop the MTSS `.jar` into your `mods` folder.
5. Launch the game.

---

## Usage

### Opening the editor

Run `/mtss gui`, or press the **Open Editor** keybind (default: **H**).

### Editor controls

| Action | Result |
|---|---|
| Left-click + drag | Move a stat list |
| Right-click on a list | Open its context menu |
| Right-click on empty space | Create a new list at that position, or open the full config screen |
| Escape | Close the editor and save |

### List context menu

Right-clicking a list opens four actions:

- **⚙ Edit Stats** — a category-grouped, searchable panel (Performance / Player / World / Position) to toggle stats on/off, reorder them, and open per-stat settings: show/hide the label prefix, decimal places, render-as-graph, and custom color thresholds where supported.
- **▤ Appearance »** — rename the list, toggle its background and text shadow, override its color and text scale (0.5×–2.0×), toggle Template Mode, and configure horizontal/vertical snap.
- **⧉ Duplicate** — clone the list, including all its stats and settings, as a starting point for a variant.
- **✕ Delete** — remove the list.

### Toggling the overlay

Press the **Toggle Overlay** keybind (unbound by default) to instantly show or hide the HUD without opening the editor. This doesn't affect the editor: `/mtss gui` still opens and previews your lists normally even while the overlay is hidden.

### Full settings screen

Run `/mtss config`, open MTSS from ModMenu's mod list (if installed), or right-click empty space in the editor and choose **Open Full Config**. This opens a Cloth Config screen covering every field in `mtss.json` in one place — general settings, hardware-sensor settings, editor GUI tuning, and every list's full configuration (including graph styling, which has no dedicated control in the custom editor).

---

## Template Mode

Every list defaults to **classic mode**: one line per enabled stat, in the order you set in Edit Stats. **Template Mode** is an opt-in, per-list alternative — instead of a fixed stat list, you write your own line(s) of text with stat tokens interpolated wherever you like.

Turn it on via a list's **Appearance »** panel. Once enabled, the context menu's **Edit Stats** row becomes **Edit Template Lines**, where each line is edited as free text with **Enter** to confirm and **Esc** to cancel.

### Token syntax

Wrap a stat's token in curly braces to interpolate it: `{tps}` inserts the current TPS. Everything else in the line is literal text.

```
FPS: {fps} | TPS: {tps:2} | {ping}ms
```

might render as:

```
FPS: 144 | TPS: 19.86 | 42ms
```

**Modifiers** (comma-separated after a colon):

| Modifier | Effect | Example |
|---|---|---|
| `:N` | Decimal places, for stats that support them | `{tps:2}` |
| `graph=true` | Render as a rolling history graph instead of text (must be the line's only content) | `{fps:graph=true}` |
| `color=#RRGGBB` | Color just this token, independent of the line's own color (3-digit shorthand accepted) | `{tps:color=#55FF55}` |

Modifiers combine via commas, e.g. `{cpu:0,color=#FF5555}`. `color=` and `graph=true` don't combine on the same token — a graph draws its own coloring.

To show a literal brace, double it: `{{` → `{`, `}}` → `}`.

An unrecognized or malformed token (`{tsp}`, `{ping:2}`, `{tps:graph=yes}`) is never dropped — it renders back as literal text, braces included, and logs a one-time warning to the client log so the mistake is visible.

By default a template line renders in one flat color: the list's custom color if set, otherwise white. Classic mode's Show Prefix setting doesn't apply here — you simply don't type a label if you don't want one.

### Full token table

See the [Stat Reference](#stat-reference) table below — the **Token** column is the exact text to use inside `{ }`.

---

## Stat Reference

MTSS tracks **46 stats**, grouped into the same four categories the in-game Edit Stats panel uses. The four GPU/VRAM stats live in the **Performance** category but are opt-in — see [Hardware Sensors](#hardware-sensors-opt-in-via-librehardwaremonitor) below. **Decimals** = supports the `:N` template modifier / decimals stepper. **Graph** = can render as a rolling history graph. **Threshold** = has a user-configurable good/warn color threshold.

### Performance

| Stat | Token | Decimals | Graph | Threshold | Notes |
|---|---|:---:|:---:|:---:|---|
| TPS | `{tps}` | ✅ | ✅ | ✅ | Server ticks per second. Singleplayer/LAN: derived from live MSPT. Multiplayer: read from the server's `TickingState` packet. |
| MSPT | `{mspt}` | ✅ | ✅ | — | Milliseconds per tick. Singleplayer/LAN only — hidden where the data isn't available. Colored using TPS's color scale. |
| FPS | `{fps}` | — | ✅ | ✅ | Client frames per second. |
| Ping | `{ping}` | — | ✅ | ✅ | Round-trip latency in ms. |
| Memory | `{mem}` | — | ✅ | ✅ | JVM heap used/max MB. Graphed as heap-used percentage. |
| CPU | `{cpu}` | ✅ | ✅ | ✅ | JVM process CPU load %, polled every 500ms via `OperatingSystemMXBean`. HotSpot/OpenJDK only — shows "N/A" on other JVM vendors. |
| GC Time | `{gc}` | — | — | — | Cumulative JVM garbage-collection time in ms, summed across all GC beans. |
| Rendered Sections | `{rendered}` | — | — | — | Chunk sections in the current render pass, pulled from `LevelRenderer`. |
| Players Online | `{players}` | — | — | — | Number of players currently online. |
| GPU Temp *(opt-in)* | `{gpu_temp}` | ✅ | ✅ | ✅ | GPU core temperature in °C, via LibreHardwareMonitor. Thresholds default to <70° green, <85° yellow, ≥85° red — a general "hot GPU" signal, not a card-specific throttle point. |
| GPU Clock *(opt-in)* | `{gpu_clock}` | ✅ | ✅ | — | GPU core clock speed in MHz, via LibreHardwareMonitor. No threshold coloring — clock varies with workload/boost behavior. |
| GPU Usage *(opt-in)* | `{gpu_usage}` | ✅ | ✅ | — | GPU utilization %, via LibreHardwareMonitor. No threshold coloring — high usage during gameplay is expected. |
| VRAM Used *(opt-in)* | `{vram_used}` | — | ✅ | — | VRAM used/total MB, via LibreHardwareMonitor. Renders only once both values are available. |

### Player

| Stat | Token | Decimals | Graph | Threshold | Notes |
|---|---|:---:|:---:|:---:|---|
| Health | `{health}` | — | ✅ | ✅ | Current/max health. |
| Hunger | `{hunger}` | — | ✅ | ✅ | Hunger level. |
| Saturation | `{saturation}` | ✅ | — | — | Saturation level. |
| Armor | `{armor}` | — | ✅ | ✅ | Armor points. |
| Air | `{air}` | — | — | — | Remaining air/breath. |
| XP Level | `{xplevel}` | — | — | — | Current experience level. |
| XP Progress | `{xpprogress}` | ✅ | — | — | Progress toward the next level, as a percentage. |
| Game Mode | `{gamemode}` | — | — | — | Current game mode. |
| Selected Slot | `{slot}` | — | — | — | Currently selected hotbar slot. |
| Held Item | `{helditem}` | — | — | — | Name of the currently held item. |
| Speed | `{speed}` | ✅ | ✅ | — | Horizontal movement speed in blocks/second, from `deltaMovement × 20` ticks/s. Colored/graphed by its own fixed movement-based scale, not a user threshold. |
| Vertical Speed | `{vspeed}` | ✅ | — | — | Vertical movement speed in blocks/second. |
| Moving | `{moving}` | — | — | — | On/off — true when horizontal movement exceeds the same "stationary" epsilon used by Speed's coloring. |

### World

| Stat | Token | Decimals | Graph | Threshold | Notes |
|---|---|:---:|:---:|:---:|---|
| Entities | `{entities}` | — | — | — | Loaded entity count in your dimension. |
| Chunks | `{chunks}` | — | — | — | Loaded chunk count. |
| Biome | `{biome}` | — | — | — | Biome at your current position. |
| Dimension | `{dimension}` | — | — | — | Current dimension ID (e.g. `overworld`, `the_nether`, `the_end`). |
| Weather | `{weather}` | — | — | — | Clear / Rain / Thunder. |
| Difficulty | `{difficulty}` | — | — | — | World difficulty. |
| Light Level | `{light}` | — | — | — | Combined light level at your block. |
| Sky Light | `{skylight}` | — | — | — | Sky light component at your block. |
| Block Light | `{blocklight}` | — | — | — | Block light component at your block. |
| Can See Sky | `{canseesky}` | — | — | — | Whether your position has an unobstructed view of the sky. |
| Looking At | `{lookingat}` | — | — | — | Name of the block or entity under the crosshair (empty when nothing's targeted); reads vanilla's own `hitResult`. |

### Position

| Stat | Token | Decimals | Graph | Threshold | Notes |
|---|---|:---:|:---:|:---:|---|
| Coords | `{coords}` | — | — | — | Combined X/Y/Z, floor-rounded. |
| X | `{x}` | — | — | — | Individual, block-rounded coordinate. |
| Y | `{y}` | — | — | — | Individual, block-rounded coordinate. |
| Z | `{z}` | — | — | — | Individual, block-rounded coordinate. |
| Facing | `{facing}` | — | — | — | 8-way cardinal/intercardinal direction. |
| Yaw | `{yaw}` | ✅ | — | — | Raw yaw, normalized to 0–360°. |
| Pitch | `{pitch}` | ✅ | — | — | Raw pitch, -90 (up) to 90 (down). |
| Chunk Pos | `{chunkpos}` | — | — | — | Chunk coordinates. |
| Distance from Spawn | `{distance}` | ✅ | — | — | Horizontal distance from world spawn. |

---

## Hardware Sensors (opt-in via LibreHardwareMonitor)

Every other stat MTSS shows comes straight from the JVM or Minecraft's own state. GPU temperature, clock, usage, and VRAM usage don't. no JVM API exposes them. So these four are an **optional** integration with [LibreHardwareMonitor](https://github.com/LibreHardwareMonitor/LibreHardwareMonitor).

### How it works

MTSS polls LHM's built-in **Remote Web Server**, roughly every 1.5 seconds by default. It never touches a GPU driver, SDK, or shared memory directly, and never touches the render thread: if LHM is unreachable, slow, or not running, the affected stats simply don't render, the same way MSPT is hidden on a remote server. There's no bundled native code or vendor SDK involved.

### Setup

1. Install and run [LibreHardwareMonitor](https://github.com/LibreHardwareMonitor/LibreHardwareMonitor/releases).
2. In LHM, go to **Options → Remote Web Server → Run**. By default this serves `http://localhost:8085/data.json`.
3. Enable it in MTSS via the Cloth Config screen (`/mtss config` → **Hardware Sensors**) and set the base URL there.

### If a stat doesn't show up

This feature degrades silently by design, so check, in order:

- `hardwareSensorsEnabled` is `true`.
- LHM is running and its Remote Web Server shows active in LHM's own UI.
- `hardwareSensorBaseUrl` matches the port LHM's Remote Web Server actually uses.
- Nothing local (firewall, antivirus) is blocking the loopback connection.

---

## Configuration file

Settings are saved automatically and atomically to `.minecraft/config/mtss.json`. You can inspect or back up this file, but there's no need to edit it manually for anything the in-game editor or the Cloth Config screen already exposes. If a config file fails to parse, MTSS backs it up as `mtss.json.bak-<timestamp>` and starts fresh, rather than discarding it silently.

---

## Compatibility

- **Client-side only** — works against any server (vanilla, Paper, Fabric, etc.).
- **MSPT** is only available on singleplayer and LAN worlds — it's silently hidden on remote servers where the data isn't exposed.
- **GPU/VRAM sensor stats** are opt-in and require LibreHardwareMonitor running separately with its Remote Web Server enabled; they're silently hidden otherwise.
- MTSS registers its overlay through Fabric API's `HudElementRegistry`, attached just before the chat layer, so it doesn't conflict with other HUD mods.

---

## Architecture

For contributors and the curious, MTSS's client code is organized into a few focused packages:

| Package | Responsibility |
|---|---|
| `bottled.mtss.sample` | `StatSource` implementations gather raw data (player state, world state, JVM metrics) at a declared [`Cadence`](src/client/java/bottled/mtss/sample/Cadence.java) — `PER_FRAME`, `PER_TICK`, `THROTTLED`, or `EVENT_PUSHED`. `SamplingDriver` runs every registered source once per frame, isolating failures so one broken source can't take the whole overlay down. |
| `bottled.mtss.stat` | `StatDefinition` is the single source of truth per stat — formatting, decimals, graph/threshold support, and coloring all live in one implementation per stat, registered in `StatRegistry`. The GUI, HUD renderer, and Template Engine all read through this registry; none of them switch on individual stats. |
| `bottled.mtss.hud` | Rendering: `MtssRenderer` draws the overlay, `LineBuilder`/`LineCache` build and cache each list's rendered lines per frame, `TemplateEngine` parses and renders Template Mode lines, `GraphRenderer` draws history graphs. |
| `bottled.mtss.gui` | The custom drag-and-drop editor screen and its panels (reorder/toggle, appearance, thresholds, template line editing). |
| `bottled.mtss.config` | `MtssConfig` — the persisted data model, atomic save/load, and backward-compatible field backfilling. `bottled.mtss.config.cloth` hosts the Cloth Config screen and optional ModMenu integration. |
| `bottled.mtss.command` | Registers `/mtss gui` and `/mtss config`. |

Server tick rate arrives via a mixin (`ClientPacketListenerMixin`) injected into `ClientPacketListener#handleTickingState`, rather than being polled, it's pushed the moment the server sends a `TickingState` packet.

### Adding a new stat

1. Add a constant to `MtssConfig.Stat`.
2. Implement `StatDefinition` in `bottled.mtss.stat.stats`. Copy the smallest existing example (`EntitiesStat`) for a plain text stat, or a stat like `PingStat` if it needs graph/threshold support.
3. Register an instance in `StatRegistry`'s static block.
4. Add its lang keys (`stat.mtss.<name>` and `mtss.stat.<name>`) to `en_us.json`.
5. If it reads live game/JVM state, source it from a `StatSource` in `bottled.mtss.sample.sources` (or `MtssDataHolder`), keeping the `StatDefinition` itself a thin delegate rather than doing its own polling.
6. Add its row to this README's [Stat Reference](#stat-reference) table.

No `switch (stat)` blocks need updating anywhere else, the registry pattern is the entire extension point.

---

## Building from source

MTSS is built with [Fabric Loom](https://github.com/FabricMC/fabric-loom) and Gradle.

```bash
git clone https://github.com/bottledpepsi/MineTuner-Statistics-Server.git
cd MineTuner-Statistics-Server
./gradlew build
```

This mod uses Loom's `splitEnvironmentSourceSets()`, so `src/client` compiles and runs client-side only, while `src/main` is shared.

---

## Contributing

Issues and pull requests are welcome. If you're adding a new stat, follow the [Adding a new stat](#adding-a-new-stat) steps above so the GUI, Template Engine, and Cloth Config screen all pick it up automatically through the registry, no other files need editing. For anything else, feel free to open an issue to discuss the change before submitting a pull request.

---

## License

MineTuner Statistics Server is licensed under the [GNU General Public License v3.0](LICENSE).
