<div align="center">

<img src="src/main/resources/assets/minetuner/icon.png" width="96" height="96" alt="MineTuner HUD icon">

# MineTuner HUD

**Customisable performance and stats HUD for Fabric.**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.2%2B-brightgreen.svg)](#requirements)
[![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-0.19.3%2B-lightgrey.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25%2B-orange.svg)](#requirements)

</div>

---

## What is MineTuner HUD?

MineTuner HUD replaces the vanilla debug screen with an in-game HUD that shows only the stats you care about.

MineTuner is a **client-side-only** Fabric mod. It works on any server and installing it doesn't require anything on the server side.

---

## Features

- **Multiple stat lists** — create as many separate HUD panels as you want, each positioned, styled, and configured independently.
- **Drag-and-drop editor** (default keybind **H**) — drag lists anywhere on screen; right-click a list for its context menu; right-click empty space to create a new one.
- **Snap-to-center** while dragging with a visible guide line.
- **52 tracked stats** across four categories — performance, player, world, and position — including four optional hardware-sensor stats. See the [full stat table](https://bottledpepsi.github.io/MineTuner-HUD/stats.html) below.
- **Rolling history graphs** — Certain stats can render as a graph.
- **Template Mode** — an alternative to the fixed stat-list layout: write your own lines of text and interpolate stat tokens like `{tps}` and `{fps}` anywhere, mixing multiple stats and literal text freely.
- **List Themes** — save any list's full appearance (background, shadow, custom color, text scale, graph styling) as a named, reusable theme; apply it to any list in one click, and pick a default theme new lists start with.
- **Cloth Config screen** (`/minetuner config`, or via ModMenu if installed) — a searchable, single-scroll settings screen covering every field in `minetuner.json`, including tuning options with no control in the custom editor.
- **Benchmark Mode** — a start/stop performance run with its own dedicated screen (`/minetuner benchmark`): tracks Avg FPS, 1% Low, 0.1% Low, Frametime, CPU, and GPU over  the recorded window, independent of the live overlay's rolling stats. See [Benchmark Mode](#benchmark-mode) below.
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

> MineTuner is **client-side only**. Nothing needs to be installed on the server.

---

## Installation

1. Install [**Fabric Loader**](https://fabricmc.net/use/installer/).
2. Install [**Fabric API**](https://modrinth.com/mod/fabric-api) and [**Cloth Config API**](https://modrinth.com/mod/cloth-config) into your `mods` folder.
3. (Optional) Install [**ModMenu**](https://modrinth.com/mod/modmenu) for a GUI entry point into MineTuner's settings.
4. Drop the MineTuner `.jar` into your `mods` folder.
5. Launch the game.

---

## Usage

### Opening the editor

Run `/minetuner gui`, or press the **Open Editor** keybind (default: **H**).

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
- **▤ Appearance »** — rename the list, toggle its background and text shadow, override its color and text scale (0.5×–2.0×), open **Theme »** to browse/apply/save/delete appearance themes, toggle Template Mode, and configure horizontal/vertical snap.
- **⧉ Duplicate** — clone the list, including all its stats and settings, as a starting point for a variant.
- **✕ Delete** — remove the list.

### List Themes

A **theme** is a named, reusable bundle of everything about how a list *looks*: background on/off, text shadow, custom color, text scale, and every stat's graph styling. It does **not** include which stats are shown, their order, the list's name, or where it sits on screen — applying a theme never moves a list or changes its stats.

Open **Appearance » Theme »** from any list's context menu to:

- **Apply** a theme — click any theme in the list to instantly give the current list that appearance.
- **Save as new theme** — click **+ Save as new theme**, type a name, and press Enter to save the list's current appearance as a theme you can apply to any list later, including the one it came from. Typing the name of an existing user-created theme re-saves ("updates") that theme in place instead of making a duplicate.
- **Delete** a theme — click the ✕ next to a user-created theme. Built-in themes are marked **(built-in)** and can't be deleted, renamed, or overwritten.

MineTuner ships three built-in themes:

| Theme | Look                                                                                 |
|---|--------------------------------------------------------------------------------------|
| **Classic** | MineTuner's original default. |
| **Minimal** | No outline, less padding.                                                            |
| **High Contrast** | Black background, white outline, bright yellow text.                                 |

Every appearance field a theme captures is also editable directly (without going through a theme) in the Appearance/Color-Scale panels or the full Cloth Config screen below — themes are just a shortcut for applying several of those fields at once.

### Toggling the overlay

Press the **Toggle Overlay** keybind (unbound by default) to instantly show or hide the HUD without opening the editor. This doesn't affect the editor: `/minetuner gui` still opens and previews your lists normally even while the overlay is hidden.

### Full settings screen

Run `/minetuner config`, open MineTuner from ModMenu's mod list (if installed), or right-click empty space in the editor and choose **Open Full Config**. This opens a Cloth Config screen covering every field in `minetuner.json` in one place — general settings (including **Default List Theme**, the theme applied to every brand-new list — changing it never affects lists that already exist), hardware-sensor settings, editor GUI tuning, and every list's full configuration (including graph styling, which has no dedicated control in the custom editor).

---

## Benchmark Mode

Benchmark Mode is a start/stop performance run, independent from the live overlay's own rolling stats: press Start, play through whatever you want to measure, then press Stop to freeze the result. Unlike the overlay's session Avg/Min/Max FPS (which resets on every world join/disconnect and keeps rolling for as long as you're connected), a benchmark result stays frozen exactly as it was the moment you stopped it, until you start another run.

A finished benchmark reports:

- **Avg FPS** — the average frame rate across the entire recorded run.
- **1% Low** and **0.1% Low** — the same percentile-low FPS calculation the overlay's own `{fps_1pct_low}`/`{fps_01pct_low}` stats use, computed over just the benchmark's window. Shows **N/A** if the run was too short to have enough samples for a statistically meaningful percentile (matching the same minimum-sample thresholds the live stats use).
- **Frametime**
- **CPU**
- **GPU** Shows **N/A** if GPU sampling isn't available (see [Hardware Sensors](#hardware-sensors-opt-in-via-librehardwaremonitor) below
- **Duration** and **sample count** for the run.

### Controls

| Action | Keybind (default) | Also available via |
|---|---|---|
| Start / Reset benchmark | unbound | Start/Reset button in the Benchmark screen |
| Stop / Freeze benchmark | unbound | Stop/Freeze button in the Benchmark screen |
| Open Benchmark screen | unbound | `/minetuner benchmark` |

All three are unbound by default, the same as the **Toggle Overlay** keybind — bind whichever you'll use from Minecraft's Controls settings. Start and Stop work anywhere in-game without needing the Benchmark screen open at all, the same way RTSS's own hotkeys do; the screen itself is there for watching a run live and reviewing the result afterward, not a requirement for starting or stopping one.

Pressing **Start** while a previous result is still showing discards it and begins a fresh run — there's no separate "clear results" step. Pressing **Stop** with nothing running does nothing (no result is generated from an empty run).

### The Benchmark screen

Opened via the **Open Benchmark** keybind or `/minetuner benchmark`. It shows the current state (Idle / Recording / Stopped), Start/Reset and Stop/Freeze buttons:

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

See the [Stat Reference](https://bottledpepsi.github.io/MineTuner-HUD/stats.html) table — the **Token** column is the exact text to use inside `{ }`.

---

## Hardware Sensors (opt-in via LibreHardwareMonitor)

Every other stat MineTuner shows comes straight from the JVM or Minecraft's own state. GPU temperature, clock, usage, and VRAM usage don't. no JVM API exposes them. So these four are an **optional** integration with [LibreHardwareMonitor](https://github.com/LibreHardwareMonitor/LibreHardwareMonitor).

### How it works

MineTuner polls LHM's built-in **Remote Web Server**, roughly every 1.5 seconds by default. It never touches a GPU driver, SDK, or shared memory directly, and never touches the render thread: if LHM is unreachable, slow, or not running, the affected stats simply don't render, the same way MSPT is hidden on a remote server. There's no bundled native code or vendor SDK involved.

### Setup

1. Install and run [LibreHardwareMonitor](https://github.com/LibreHardwareMonitor/LibreHardwareMonitor/releases).
2. In LHM, go to **Options → Remote Web Server → Run**. By default this serves `http://localhost:8085/data.json`.
3. Enable it in MineTuner via the Cloth Config screen (`/minetuner config` → **Hardware Sensors**) and set the base URL there.

### If a stat doesn't show up

This feature degrades silently by design, so check, in order:

- `hardwareSensorsEnabled` is `true`.
- LHM is running and its Remote Web Server shows active in LHM's own UI.
- `hardwareSensorBaseUrl` matches the port LHM's Remote Web Server actually uses.
- Nothing local (firewall, antivirus) is blocking the loopback connection.

---

## Configuration file

Settings are saved automatically and atomically to `.minecraft/config/minetuner.json`. You can inspect or back up this file, but there's no need to edit it manually for anything the in-game editor or the Cloth Config screen already exposes. If a config file fails to parse, MineTuner backs it up as `minetuner.json.bak-<timestamp>` and starts fresh, rather than discarding it silently.

---

## Compatibility

- **Client-side only** — works against any server (vanilla, Paper, Fabric, etc.).
- **MSPT** is only available on singleplayer and LAN worlds — it's silently hidden on remote servers where the data isn't exposed.
- **GPU/VRAM sensor stats** are opt-in and require LibreHardwareMonitor running separately with its Remote Web Server enabled; they're silently hidden otherwise.
- MineTuner registers its overlay through Fabric API's `HudElementRegistry`, attached just before the chat layer, so it doesn't conflict with other HUD mods.

---

### Adding a new stat

1. Add a constant to `MineTunerConfig.Stat`.
2. Implement `StatDefinition` in `bottled.minetuner.stat.stats`. Copy the smallest existing example (`EntitiesStat`) for a plain text stat, or a stat like `PingStat` if it needs graph/threshold support.
3. Register an instance in `StatRegistry`'s static block.
4. Add its lang keys (`stat.minetuner.<name>` and `minetuner.stat.<name>`) to `en_us.json`.
5. If it reads live game/JVM state, source it from a `StatSource` in `bottled.minetuner.sample.sources` (or `MineTunerDataHolder`), keeping the `StatDefinition` itself a thin delegate rather than doing its own polling.
6. Add its row to the docs [Stat Reference](https://bottledpepsi.github.io/MineTuner-HUD/architecture.html#adding-a-stat) table.

No `switch (stat)` blocks need updating anywhere else, the registry pattern is the entire extension point.

---

## Building from source

MineTuner is built with [Fabric Loom](https://github.com/FabricMC/fabric-loom) and Gradle.

```bash
git clone https://github.com/bottledpepsi/MineTuner-HUD.git
cd MineTuner-HUD
./gradlew build
```

This mod uses Loom's `splitEnvironmentSourceSets()`, so `src/client` compiles and runs client-side only, while `src/main` is shared.

---

## Contributing

Issues and pull requests are welcome. If you're adding a new stat, follow the [Adding a new stat](#adding-a-new-stat) steps above so the GUI, Template Engine, and Cloth Config screen all pick it up automatically through the registry, no other files need editing. For anything else, feel free to open an issue to discuss the change before submitting a pull request.

---

## License

MineTuner HUD is licensed under the [GNU General Public License v3.0](LICENSE).
