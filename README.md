# MineTuner Statistics Server (MTSS)

**A fully customisable, multi-list performance overlay for Fabric**

---

## What is MineTuner Statistics Server?

MineTuner Statistics Server puts a clean, configurable HUD on your screen showing real-time performance and world data. Unlike the vanilla debug screen, MineTuner Statistics Server is designed to live on your screen permanently, unobtrusive, readable, and completely yours to configure.

You decide what stats appear, where they appear, and how many separate stat panels you want. Everything is controlled via a drag-and-drop in-game GUI. No config file editing required.

---

## Stats

MineTuner Statistics Server tracks **16 metrics** across server, client, player, and system categories:

| Stat | Description | Notes |
|---|---|---|
| **TPS** | Server ticks per second | Color-coded green/yellow/red. On singleplayer, calculated from live MSPT. On multiplayer, read from the server's `TickingState` packet. |
| **MSPT** | Milliseconds per tick | Singleplayer / LAN only — automatically hidden on remote servers where the data is unavailable. |
| **FPS** | Client frames per second | Color-coded: ≥60 green, ≥30 yellow, <30 red. |
| **Ping** | Round-trip latency in ms | Color-coded: ≤80ms green, ≤150ms yellow, >150ms red. |
| **Memory** | JVM heap usage (used / max MB) | Color-coded by heap fill percentage. |
| **CPU** | JVM process CPU load % | Polled every 500ms via `OperatingSystemMXBean`. HotSpot/OpenJDK only — shows "N/A" on other JVM vendors. |
| **Entities** | Loaded entity count in your dimension | — |
| **Chunks** | Loaded chunk count | — |
| **Rendered Sections** | Number of chunk sections in the render pass | Pulled directly from `LevelRenderer`. |
| **Coords** | Your block XYZ position | Floor-rounded integers. |
| **Facing** | Cardinal + intercardinal direction | Full 8-way: N, NE, E, SE, S, SW, W, NW. |
| **Speed** | Horizontal movement speed in blocks/second | Calculated from `deltaMovement` × 20 ticks/s. |
| **GC Time** | Cumulative JVM garbage collection time in ms | Sums all GC beans via `GarbageCollectorMXBean`. |
| **Biome** | Biome at your current position | — |
| **Light Level** | Local light level at your block position | — |
| **Dimension** | Current dimension ID | e.g. `overworld`, `the_nether`, `the_end`. |

Stats that render a number (**TPS, MSPT, CPU, Speed**) support a configurable decimal-places setting, adjustable per-list via each stat's settings panel (⚙).

---

## Features

### Multiple independent stat lists
Create as many separate HUD panels as you want. Each list is independently positioned, configured, and styled. Run a performance-focused panel in one corner and a coordinates/facing panel in another — entirely your call.

### Drag-and-drop positioning
Open the editor (`/mtss gui`) and drag any list anywhere on screen. Positions are saved relative to the nearest screen corner, so your layout survives window resizes and resolution changes without drifting.

### Snap-to-centre
While dragging, lists snap to the vertical and horizontal centre lines of your screen with a visible guide line and hit marker. The snap axis is saved — centred panels stay centred regardless of window size.

### Per-list stat control
Right-click any list in the editor to open its context menu:
- **⚙ Edit Stats** — enable or disable individual stats, move them up/down, and open per-stat settings: toggle the label prefix (e.g. hide "TPS: " and show just the value), set decimal places for numeric stats (TPS, MSPT, CPU, Speed), switch graphable stats (TPS, MSPT, FPS, CPU, Ping, Memory, Speed) between text and a rolling history graph, and override the good/warn color thresholds for TPS, FPS, Ping, Memory, and CPU
- **▤ Appearance** — a sub-panel bundling:
  - **Rename** — give the list a custom name
  - **Background** — toggle the semi-transparent dark background
  - **Text Shadow** — toggle text shadow
  - **Color / Scale** — override the normal color-coding with a custom color, and scale the list's text from 0.5x to 2.0x
  - **Template Mode** — switch the list into freeform template lines (see [Template Mode](#template-mode-1) below)
- **⧉ Duplicate** — clone a list (including all its stats and settings) as a starting point for a variant layout
- **✕ Delete** — remove the list entirely

### Right-click to create
Right-click on any empty area of the editor screen to instantly create a new stat list at that location.

### Keybind to open the editor
In addition to `/mtss gui`, a keybind (default: **H**) opens the editor directly — rebindable in **Options → Controls → Key Binds → MineTuner Statistics Server**.

### Keybind to toggle the overlay
A separate keybind (unbound by default, rebindable in **Options → Controls → Key Binds → MineTuner Statistics Server**) instantly shows or hides the live overlay without opening the editor — handy for streaming or screenshots. It's independent of the editor: opening the editor still works, and still shows list previews, even while the overlay is hidden.

### Color-coded values
TPS, FPS, Ping, Memory, CPU, and Speed all render in context-aware colors (green / yellow / red) based on thresholds — you can tell at a glance whether something's wrong without reading the number. For TPS, FPS, Ping, Memory, and CPU, these thresholds can be customized per list via each stat's settings panel (⚙ → Custom Thresholds »); Speed uses its own fixed movement-based coloring instead.

### Zero overhead when not needed
The HUD renderer is skipped entirely when the vanilla debug screen (F3) is open, and the overlay is suppressed while the editor GUI is active. Slow metrics (CPU, GC) are polled on a 500ms throttle to avoid hammering `OperatingSystemMXBean` every frame.

### Frame-coherent line cache
All stat string building is cached per-frame in a generation-keyed `HashMap`. The renderer and the editor GUI share the same cache, so the same strings are never built twice in one frame.

### Template Mode
Every list defaults to **classic mode** — one stat per line, exactly as MineTuner Statistics Server has always worked. Flip a list into **Template Mode** and you take direct control of its lines instead: write your own text and drop in stat tokens like `{tps}` or `{fps}` anywhere you want, mixing multiple stats and literal text on a single line (e.g. `FPS: {fps} | TPS: {tps:2} | {ping}ms`). See the [Template Mode](#template-mode-1) section under Usage for the full token table and editing flow.

---

## Installation

1. Install [**Fabric Loader**](https://fabricmc.net/use/installer/) (≥ 0.18)
2. Install [**Fabric API**](https://modrinth.com/mod/fabric-api)
3. Drop the MineTuner Statistics Server `.jar` into your `mods` folder
4. Launch the game

**Requirements:**
- Minecraft ≥ 26.1
- Java 25 or newer
- Fabric API

> MineTuner Statistics Server is a **client-side only** mod. It does not need to be installed on servers.

---

## Usage

### Opening the editor
Run the command `/mtss gui` in chat, or press the **MineTuner Statistics Server** keybind (default: **H**, rebindable in Controls).

### Hiding the overlay
Press the **Toggle MineTuner Statistics Server Overlay** keybind (unbound by default, bind it in **Options → Controls → Key Binds → MineTuner Statistics Server**) to instantly show or hide the HUD without opening the editor. An actionbar message confirms whether the overlay is now shown or hidden. This only affects the live overlay — the editor (`/mtss gui` or the open-editor keybind) still opens and previews your lists normally even while the overlay is hidden.

### Controls in the editor
| Action | Result |
|---|---|
| **Left-click + drag** | Move a stat list |
| **Right-click on a list** | Open context menu (edit stats, appearance, duplicate, delete) |
| **Right-click on empty space** | Create a new list at that position |
| **Escape** | Close the editor and save |

### Edit Stats panel
Inside a list's context menu, click **⚙ Edit Stats** to open the stat panel for that list:
- Click a stat row to **toggle it on or off**
- Click **▲ / ▼** to move a stat up or down in the display order
- Click **⚙** to open per-stat settings:
  - **Show Prefix** — show/hide the label prefix (e.g. hide "TPS: " and show just the value)
  - **Decimals** — for numeric stats (TPS, MSPT, CPU, Speed), adjust the number of decimal places with **- / +**
  - **Render as Graph** — for graphable stats (TPS, MSPT, FPS, CPU, Ping, Memory, Speed), swap the text line for a rolling history graph
  - **Custom Thresholds »** — for TPS, FPS, Ping, Memory, and CPU, opens a sub-panel to override that stat's green/yellow/red color cutoffs for this list only:
    - **Use Custom Thresholds** toggle — when off, the stat falls back to its built-in default thresholds
    - **Good** / **Warn** steppers — adjust with **- / +** (0.5 steps for TPS, whole-number steps for everything else). A subtitle under the stat name reminds you whether the stat is "(higher is better)" (TPS, FPS) or "(lower is better)" (Ping, Memory, CPU), since the two work opposite to each other
    - The good/warn values are automatically kept in the correct order for that stat's direction, so you can't accidentally create an inverted range
    - Click **◀ Back** to return to the per-stat settings panel
- Click **✕ Close** to return to the context menu

### Appearance panel
Inside a list's context menu, click **▤ Appearance »** to open:
- **Rename** — give the list a custom name
- **Background** — toggle the semi-transparent dark background
- **Text Shadow** — toggle text shadow
- **Color / Scale »** — opens a sub-panel:
  - Toggle **Use Custom Color** to override the normal threshold-based coloring
  - Click **Cycle Color** to step through a curated color palette
  - Use **- / +** to adjust the list's text scale between 0.5x and 2.0x
  - Click **◀ Back** to return to Appearance
- **Template Mode** — toggle the list into freeform template lines (see [Template Mode](#template-mode-1) below)
- Click **◀ Back** to return to the context menu

### Template Mode
Classic mode (the default) renders one line per enabled stat, in `statOrder` order, exactly as described above. **Template Mode** is an opt-in alternative for a list: instead of a fixed list of stat rows, you write your own line(s) of text with stat tokens interpolated in — closer to a small hypertext markup than a strict stat list. This is entirely per-list; a list not in Template Mode behaves identically to every prior version.

#### Turning it on
Right-click a list → **▤ Appearance »** → **Template Mode** to toggle it on for that list. Once on, the context menu's **⚙ Edit Stats** row becomes **⚙ Edit Template Lines**.

#### Editing template lines
Click **⚙ Edit Template Lines** to open the line list for that list:
- Click an existing line to open a text-entry box for it — type your template, press **Enter** to confirm, or **Esc** to cancel and keep the previous text
- Click **✕** next to a line to delete it
- Click **+ Add line** to append a new (initially empty) line, then click it to edit
- Click **✕ Close** to return to the context menu

Each `templateLines` entry becomes one rendered line, in list order.

#### Token syntax
Wrap a stat's token name in curly braces to interpolate it: `{tps}` inserts the current TPS value using its default decimal count. Everything else in a template line is literal text, rendered exactly as typed.

| Token | Stat | Notes |
|---|---|---|
| `{tps}` | TPS | Supports `:N` decimals suffix |
| `{mspt}` | MSPT | Supports `:N` decimals suffix; renders nothing on remote servers, same as classic mode |
| `{fps}` | FPS | — |
| `{ping}` | Ping | — |
| `{mem}` | Memory | — |
| `{cpu}` | CPU | Supports `:N` decimals suffix |
| `{entities}` | Entities | — |
| `{chunks}` | Chunks | — |
| `{rendered}` | Rendered Sections | — |
| `{coords}` | Coords | — |
| `{x}` | X | Block-rounded, individual coordinate |
| `{y}` | Y | Block-rounded, individual coordinate |
| `{z}` | Z | Block-rounded, individual coordinate |
| `{facing}` | Facing | — |
| `{yaw}` | Yaw | Supports `:N` decimals suffix; normalized to 0-360 |
| `{pitch}` | Pitch | Supports `:N` decimals suffix; -90 (up) to 90 (down) |
| `{speed}` | Speed | Supports `:N` decimals suffix |
| `{gc}` | GC Time | — |
| `{biome}` | Biome | — |
| `{light}` | Light Level | — |
| `{dimension}` | Dimension | — |
| `{lookingat}` | Looking At | Block or entity name under the crosshair; empty string when nothing's targeted |
| `{moving}` | Moving | On/off — true when the player has meaningful horizontal movement |
| `{gpu_temp}` | GPU Temp | Supports `:N` decimals suffix. Opt-in — see [Hardware Sensors](#hardware-sensors-opt-in-via-librehardwaremonitor). Empty when unavailable. |
| `{gpu_clock}` | GPU Clock | Supports `:N` decimals suffix. Opt-in, same as above. Empty when unavailable. |
| `{gpu_usage}` | GPU Usage | Supports `:N` decimals suffix. Opt-in, same as above. Empty when unavailable. |
| `{vram_used}` | VRAM Used | "used/maxMB". Opt-in, same as above. Empty when unavailable. |

Add `:N` after any decimals-capable token to override its decimal places, e.g. `{tps:2}` for two decimal places, `{cpu:0}` for a whole number. Omit it to use that stat's normal default (the same default classic mode uses).

Example: `FPS: {fps} | TPS: {tps:2} | {ping}ms` might render as `FPS: 144 | TPS: 19.86 | 42ms`.

Example: `{x} {y} {z} | Yaw {yaw:0} Pitch {pitch:0}` might render as `123 64 -456 | Yaw 180 Pitch 0`.

#### Graph tokens
Add `graph=true` after `:` on any graphable token (the same stats that support **Render as Graph** in classic mode's per-stat settings) to render that stat as a rolling history graph instead of text, e.g. `{fps:graph=true}`. It uses that stat's normal graph settings — width/height, smoothing, scale mode, color mode, and so on — the same `GraphStyle` the per-stat settings panel edits (⚙ → Render as Graph), so a stat's graph looks and behaves the same whether it's turned on there or written as a template token.

Combine it with a decimals override using a comma, e.g. `{cpu:0,graph=true}`.

A graph token must be the **entire** line — no literal text and no other tokens alongside it. `{fps:graph=true}` on its own line renders a graph; `FPS: {fps:graph=true}` does not — the token still parses fine and renders FPS's plain number, but the surrounding "FPS: " text means the line no longer qualifies as graph-only, so it's drawn as a normal text row instead. This mirrors classic mode, where a stat row is always either all text or all graph, never both on the same line.

`graph=false` is also accepted (the default, so writing it explicitly is rarely needed) for symmetry with `graph=true`.

Example: a list with two lines, `{tps:graph=true}` and `{ping:graph=true}`, renders two stacked graphs instead of any text rows.

#### Literal braces
To show a literal `{` or `}` in a template line (rather than starting a token), double it: `{{` renders as `{`, and `}}` renders as `}`.

#### Unrecognized tokens
A typo'd or unknown token — `{tsp}`, `{ping:2}` (Ping has no decimals), `{tps:abc}`, `{ping:graph=true}` (Ping has no graph history), `{tps:graph=yes}` (only `true`/`false` are recognized) — is not silently dropped or treated as an error. It renders back out as literal text (braces included, e.g. `{tsp}` shows up on screen exactly like that), so a mistake is visible and easy to spot and fix rather than quietly disappearing.

#### Coloring
By default, a template line renders in a single flat color: either the list's custom color (**Color / Scale... → Use Custom Color**) if enabled, or plain white otherwise. Add `color=#RRGGBB` (or the 3-digit shorthand `color=#RGB`, same as CSS) to any token's modifiers to override just that token's color, independent of the rest of the line — e.g. `TPS: {tps:color=#55FF55}` renders `TPS: ` in the line's normal color and the TPS number in green, no matter what the line's own color is set to. Literal text and any token without a `color=` modifier keep using the line's normal color.

Combine it with other modifiers using a comma, e.g. `{cpu:0,color=#FF5555}` for zero decimals and a fixed red. `color=` and `graph=true` on the same token don't combine — a graph row draws its own graph-native coloring (see Graph tokens above), so `color=` is only meaningful on a token rendering as text.

Example: `HP {health:color=#FF5555} | Ping {ping:color=#55FFFF}ms` always shows HP in red and Ping in cyan, regardless of either stat's actual value — useful when you want a consistent color scheme rather than threshold-based coloring. (Per-token *threshold* coloring — i.e. `{health}` automatically turning red at low HP, the way classic mode's Health row does — isn't available in Template Mode; `color=` is a fixed override, not a live threshold. If you want threshold-reactive coloring, use classic mode for that stat instead.)

Classic mode's Show Prefix setting doesn't apply in Template Mode; since you're writing the literal text yourself, you simply don't type a label if you don't want one.

### Config file
Settings are saved automatically to `.minecraft/config/mtss.json`. You can inspect or back up this file, but there's no need to edit it manually for anything the in-game GUI exposes.

One exception: a graph's `GraphStyle` (panel background, gridlines, peak markers, value-display mode, smoothing, scale mode, color mode, width/height) is data model + rendering only for now — GUI controls for these follow in a later step. Until then, tweaking them means editing the relevant stat's `graphStyle` block in the config file directly.

---

## Hardware Sensors (opt-in, via LibreHardwareMonitor)

Every stat MTSS shows by default is read straight from the JVM or Minecraft's own state. GPU temperature, GPU clock speed, GPU usage, and VRAM usage aren't — no JVM API exposes them — so this is an **optional, off-by-default** integration with [LibreHardwareMonitor](https://github.com/LibreHardwareMonitor/LibreHardwareMonitor) (LHM), a free, open-source (MPL 2.0) hardware monitoring tool for Windows.

### How it works

MTSS polls LHM's built-in **Remote Web Server** — a plain local HTTP JSON endpoint LHM can optionally serve — on a background thread, at most every 1.5 seconds. It never talks to any driver, SDK, or shared memory directly, and never touches the render thread: if LHM is slow, unreachable, or not running, the affected stats just don't render a line, the same way MSPT is hidden on a remote server. Nothing about this feature can crash the game, hang a frame, or show a stale reading as if it were current.

There's no bundled native code and no vendor GPU SDK involved — this is currently a **Windows-usage pattern** in practice (that's where LHM's hardware sensor coverage is strongest), but nothing in MTSS hardcodes an OS check, so it will also work under Wine/Proton or a future non-Windows LHM build, provided the Remote Web Server is reachable at the configured URL.

### Setup

1. Install and run [LibreHardwareMonitor](https://github.com/LibreHardwareMonitor/LibreHardwareMonitor/releases).
2. In LHM, go to **Options → Remote Web Server → Run**. By default this serves `http://localhost:8085/data.json`.
3. In `.minecraft/config/mtss.json`, set:
   ```json
   "hardwareSensorsEnabled": true,
   "hardwareSensorBaseUrl": "http://localhost:8085"
   ```
   (Change the port in `hardwareSensorBaseUrl` if you configured LHM's Remote Web Server to use a non-default one.) There's no GUI toggle for this yet — same as `GraphStyle` above, it's config-file-only for now.
4. Restart Minecraft (or rejoin the world) so the background poller picks up the new setting. Add `{gpu_temp}`, `{gpu_clock}`, `{gpu_usage}`, or `{vram_used}` to a Template Mode list, or enable the equivalent stats from the Edit Stats panel, to see them.

### New stats

| Stat | Description | Notes |
|---|---|---|
| **GPU Temp** | GPU core temperature in °C | Color-coded: <70°C green, <85°C yellow, ≥85°C red — a conservative "getting warm / hot" signal, not a specific card's actual throttle point (LHM's tree doesn't expose that). |
| **GPU Clock** | GPU core clock speed in MHz | No threshold coloring — clock varies by workload/boost behavior, not health. |
| **GPU Usage** | GPU utilization % | No threshold coloring — high usage during gameplay is expected, not a warning sign. |
| **VRAM Used** | VRAM used / total, in MB | Renders only once both used and total are available from this card's sensor tree. |

### If a stat doesn't show up

This feature degrades silently by design, which means a missing stat usually isn't a bug — check, in order:
- `hardwareSensorsEnabled` is `true` in `mtss.json`.
- LHM is running and its Remote Web Server shows as active in LHM's own UI.
- `hardwareSensorBaseUrl` matches the port LHM's Remote Web Server is actually using.
- Nothing local (firewall, antivirus) is blocking the loopback connection.
- Your GPU's specific sensor tree in LHM actually exposes that category — coverage varies by vendor/driver. Multi-GPU systems use the first matching sensor found; picking a specific GPU is not currently supported.

---

## Compatibility

- **Client-side only** — works on any server (vanilla, Paper, Fabric, etc.)
- **MSPT** is only displayed on singleplayer and LAN worlds — it's silently hidden on remote servers where the data isn't accessible
- **GPU/VRAM sensor stats** are opt-in and require [LibreHardwareMonitor](#hardware-sensors-opt-in-via-librehardwaremonitor) running separately with its Remote Web Server enabled; they're silently hidden if that isn't set up
- Does not conflict with other HUD mods — MineTuner Statistics Server registers its overlay via Fabric API's `HudElementRegistry` and attaches before the chat layer

---

## Adding a stat (for contributors)

Every stat's formatting, coloring, decimals, and graph/threshold support live in one place: `bottled.mtss.stat.StatDefinition`. The GUI, HUD renderer, and Template Engine all read from `bottled.mtss.stat.StatRegistry` — none of them switch on individual stats, so a new stat needs no changes in those files.

1. Add a constant to `MtssConfig.Stat`.
2. Write a class in `bottled.mtss.stat.stats` implementing `StatDefinition`. Copy the smallest existing one (`EntitiesStat`) for a plain text stat, or a threshold stat like `PingStat` if it needs graph/color support.
3. Register an instance of it in `StatRegistry`'s static block.
4. Add its lang keys (`stat.mtss.<name>` and `mtss.stat.<name>`) to `en_us.json`.
5. If it's raw game/JVM state, pull it from `MtssDataHolder`'s sampling loop, same as the existing stats — `StatDefinition` implementations should stay thin delegates, not do their own polling.
6. Add its token row to the Template Mode table above.

That's the whole surface area — no `switch (stat)` blocks to update.
