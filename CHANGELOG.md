# Changelog

All notable changes to MineTuner Statistics Server are documented in this file.

## [Unreleased]

### Added
- **Toggle-overlay keybind** (`key.mtss.toggle_overlay`, unbound by default)
  that instantly shows or hides the entire live HUD without opening the
  editor — separate from the existing open-editor keybind (`key.mtss.open_gui`,
  default **H**). Backed by a new root-level `overlayEnabled` flag in
  `mtss.json`, persisted across restarts and defaulting to `true` so
  pre-existing configs keep showing their HUD after upgrading. Toggling
  shows a short actionbar confirmation (`mtss.toggle.on` / `mtss.toggle.off`)
  and has no effect on the editor — `/mtss gui` and the open-editor keybind
  still open the editor and preview your lists normally even while the
  overlay is hidden, since the editor reads cached lines directly rather
  than going through the live renderer.
- **Rolling history graphs** for TPS, MSPT, FPS, CPU, Ping, Memory, and Speed.
  Each of these stats now has a `renderAsGraph` setting that swaps its text
  line for an 80×28px rolling graph, sourced from a 128-sample ring buffer
  fed once per frame. Memory graphs heap-used as a percentage of heap-max
  (the same basis its threshold coloring already used), so the graph is
  legible without tracking a separately-changing max alongside it. The graph
  is colored by the stat's existing threshold-color function and normalizes
  against the min/max of its own visible window. Old configs without the
  field continue to load and render as text, unchanged.
- **Graph redesign:** each graph now frames its plot area with a border and
  a faint 50%-mark reference line, overlays the current formatted value
  (same text, same decimals/prefix settings, as text mode would show) in
  the top-left corner, and labels the visible window's min/max in the
  bottom corners — so the shape of the graph is no longer the only
  information on screen. The box automatically widens if a stat's label
  (e.g. "Mem: 8192/16384MB") needs more room than the graph's base width.
- **Render as Graph** toggle in each graphable stat's settings panel (⚙ →
  same panel as Show Prefix / Decimals), so the rolling graph can be turned
  on or off per-stat without editing `mtss.json` by hand. Only shown for
  the seven graphable stats above — other stats' settings panels are
  unchanged.

## [1.1.0] - Unreleased

### Changed
- **Renamed the mod from PerfHUD to MineTuner Statistics Server (MTSS).** This is a breaking change for existing installs:
  - Mod ID changed from `perfhud` to `mtss`.
  - Java package changed from `bottled.perfhud` to `bottled.mtss`; all classes renamed accordingly (e.g. `PerfHudConfig` → `MtssConfig`, `PerfDataHolder` → `MtssDataHolder`).
  - Client command changed from `/perfhud gui` to `/mtss gui`.
  - Config file renamed from `.minecraft/config/perfhud.json` to `.minecraft/config/mtss.json` — **existing configs will not be picked up automatically**; back up and manually rename `perfhud.json` to `mtss.json` if you want to keep your current layout.
  - Translation keys renamed from the `perfhud`/`gui.perfhud`/`stat.perfhud` namespaces to `mtss`/`gui.mtss`/`stat.mtss`.
  - Mixin config files renamed (`perfhud.mixins.json` → `mtss.mixins.json`, `perfhud.client.mixins.json` → `mtss.client.mixins.json`).

### Added
- Keybind to open the MineTuner Statistics Server editor (default: **H**), in addition to `/mtss gui`.
- Three new stats: **Biome**, **Light Level**, **Dimension**.
- Per-stat decimal-place setting for numeric stats (TPS, MSPT, CPU, Speed), adjustable via each stat's settings panel (⚙).
- Per-list **Color / Scale** panel:
  - Custom color override that replaces normal threshold-based coloring.
  - Text scale, adjustable from 0.5x to 2.0x.
- **Duplicate list** context menu action — clones a list's full configuration (stats, order, appearance, position) as a starting point for variants.

### Fixed
- Removed a duplicate/redundant GitHub Actions workflow (`gradle.yml`) that ran every CI job twice on each push and PR; its useful dependency-submission step was folded into the main `build.yml` workflow.
- The editor screen now force-saves the config on close (`onClose`) as a safety net, in addition to saving after each individual mutation.
- Consolidated the reorder/toggle panel's height calculation (previously duplicated across three methods) into a single shared helper, preventing future edits from going out of sync.
- Documented that CPU% relies on the HotSpot/OpenJDK-specific `com.sun.management.OperatingSystemMXBean` API and degrades gracefully (shows "N/A") on other JVM vendors.
- Fixed `error: release version 25 not supported` from `:compileJava` when Gradle is launched from an IDE-configured JDK older than 25 (e.g. VS Code's Java extension). `build.gradle` previously set `sourceCompatibility`/`targetCompatibility`/`options.release` to 25 but never declared a Gradle Java toolchain, so Gradle compiled using whatever JDK launched it instead of selecting or provisioning a JDK 25. Added a `toolchain { languageVersion = JavaLanguageVersion.of(25) }` block, the `foojay-resolver-convention` plugin in `settings.gradle` so Gradle can auto-download a JDK 25 if none is installed, and explicit auto-detect/auto-download properties in `gradle.properties`.