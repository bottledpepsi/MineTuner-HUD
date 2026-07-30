# PerfHUD

**A fully customisable, multi-list performance overlay for Fabric**

---

## What is PerfHUD?

PerfHUD puts a clean, configurable HUD on your screen showing real-time performance and world data. Unlike the vanilla debug screen, PerfHUD is designed to live on your screen permanently, unobtrusive, readable, and completely yours to configure.

You decide what stats appear, where they appear, and how many separate stat panels you want. Everything is controlled via a drag-and-drop in-game GUI. No config file editing required.

---

## Stats

PerfHUD tracks **16 metrics** across server, client, player, and system categories:

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
Open the editor (`/perfhud gui`) and drag any list anywhere on screen. Positions are saved relative to the nearest screen corner, so your layout survives window resizes and resolution changes without drifting.

### Snap-to-centre
While dragging, lists snap to the vertical and horizontal centre lines of your screen with a visible guide line and hit marker. The snap axis is saved — centred panels stay centred regardless of window size.

### Per-list stat control
Right-click any list in the editor to open its context menu:
- **Reorder / Toggle** — enable or disable individual stats, and move them up/down within the list
- **Per-stat settings** — toggle the label prefix (e.g. hide "TPS: " and show just the value), and for numeric stats (TPS, MSPT, CPU, Speed) set the number of decimal places
- **Rename** — give each list a custom name
- **Background** — toggle the semi-transparent dark background per list
- **Text Shadow** — toggle text shadow per list
- **Color / Scale** — override the normal color-coding with a custom color, and scale the list's text from 0.5x to 2.0x
- **Duplicate** — clone a list (including all its stats and settings) as a starting point for a variant layout
- **Delete** — remove the list entirely

### Right-click to create
Right-click on any empty area of the editor screen to instantly create a new stat list at that location.

### Keybind to open the editor
In addition to `/perfhud gui`, a keybind (default: **H**) opens the editor directly — rebindable in **Options → Controls → Key Binds → PerfHUD**.

### Color-coded values
TPS, FPS, Ping, Memory, CPU, and Speed all render in context-aware colors (green / yellow / red) based on thresholds — you can tell at a glance whether something's wrong without reading the number.

### Zero overhead when not needed
The HUD renderer is skipped entirely when the vanilla debug screen (F3) is open, and the overlay is suppressed while the editor GUI is active. Slow metrics (CPU, GC) are polled on a 500ms throttle to avoid hammering `OperatingSystemMXBean` every frame.

### Frame-coherent line cache
All stat string building is cached per-frame in a generation-keyed `HashMap`. The renderer and the editor GUI share the same cache, so the same strings are never built twice in one frame.

---

## Installation

1. Install [**Fabric Loader**](https://fabricmc.net/use/installer/) (≥ 0.18)
2. Install [**Fabric API**](https://modrinth.com/mod/fabric-api)
3. Drop the PerfHUD `.jar` into your `mods` folder
4. Launch the game

**Requirements:**
- Minecraft ≥ 26.1
- Java 25 or newer
- Fabric API

> PerfHUD is a **client-side only** mod. It does not need to be installed on servers.

---

## Usage

### Opening the editor
Run the command `/perfhud gui` in chat, or press the **PerfHUD** keybind (default: **H**, rebindable in Controls).

### Controls in the editor
| Action | Result |
|---|---|
| **Left-click + drag** | Move a stat list |
| **Right-click on a list** | Open context menu (configure, rename, color/scale, duplicate, delete) |
| **Right-click on empty space** | Create a new list at that position |
| **Escape** | Close the editor and save |

### Reorder / Toggle panel
Inside a list's context menu, click **Reorder / Toggle stats** to open the stat panel for that list:
- Click a stat row to **toggle it on or off**
- Click **▲ / ▼** to move a stat up or down in the display order
- Click **⚙** to open per-stat settings — show/hide the label prefix, and for numeric stats (TPS, MSPT, CPU, Speed) adjust decimal places with **- / +**
- Click **✕ Close** to return to the context menu

### Color / Scale panel
Inside a list's context menu, click **Color / Scale...** to:
- Toggle **Use Custom Color** to override the normal threshold-based coloring
- Click **Cycle Color** to step through a curated color palette
- Use **- / +** to adjust the list's text scale between 0.5x and 2.0x

### Config file
Settings are saved automatically to `.minecraft/config/perfhud.json`. You can inspect or back up this file, but there's no need to edit it manually — the in-game GUI covers everything.

---

## Compatibility

- **Client-side only** — works on any server (vanilla, Paper, Fabric, etc.)
- **MSPT** is only displayed on singleplayer and LAN worlds — it's silently hidden on remote servers where the data isn't accessible
- Does not conflict with other HUD mods — PerfHUD registers its overlay via Fabric API's `HudElementRegistry` and attaches before the chat layer
