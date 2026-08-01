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
- **Graph visual & UX overhaul.** Every graph now renders as a proper
  layered mini perf-monitor widget instead of flat single-tone bars:
  - A gradient-faded area fill (more opaque near the top edge, fading
    toward the baseline) with a brighter 1px stroke tracing the top edge,
    so the trend reads as a line rather than a bar-chart silhouette — the
    single biggest visual upgrade in this pass.
  - Low-contrast horizontal gridlines at 25/50/75% of the current scale
    (auto-hidden on very short graphs where they'd just be noise).
  - Peak/min markers: small ticks at the highest and lowest points
    currently visible, so spikes and dips are identifiable at a glance.
  - Optional smoothing (0/2/3/4-sample moving average) computed at render
    time from the raw history — the underlying ring buffer is never
    mutated, so other consumers still see raw samples.
  - Auto-scale (default) now pads the visible range by 10% headroom so the
    line doesn't touch the very top/bottom edge; a fixed min/max mode is
    also available for a stable reference scale (e.g. always show TPS
    0–20) that doesn't recompute bounds every frame.
  - Four selectable color modes: the original whole-graph "current value"
    threshold color, a new per-segment mode that colors each historical
    sample by its own threshold at the time, a fixed single accent color
    for users who find threshold flashing distracting, and a smooth
    blue→green→yellow→red gradient across the visible range.
  - Per-graph width/height, panel background, gridlines, peak markers, and
    value-readout mode (none / current value / current + min + max) are
    all configurable via a new `GraphStyle` object nested in each stat's
    settings (data model only in this pass — GUI controls for these follow
    in a later step, matching how step 1 deferred its own GUI wiring).
  Old configs without `GraphStyle` continue to load and backfill to
  defaults that reproduce the pre-overhaul look (80×28, current-value
  threshold coloring, gridlines and peak markers on) — see the PR
  description for the two visual details where "defaults reproduce the
  old look" and "ship the new features on by default" were in tension and
  how that was resolved.
- **User-configurable per-stat color thresholds (data model + rendering).**
  TPS, FPS, Ping, Memory, and CPU each now have a per-list `ThresholdSettings`
  entry (`enabled`, `goodMin`, `warnMin`) that can override that stat's
  green/yellow/red cutoffs — e.g. "TPS goes yellow below 17 instead of 14" —
  independently of every other list showing that same stat. Previously the
  only per-list color customization was a single blanket override color
  that replaced threshold coloring entirely for every stat in the list;
  this adds fine-grained control without touching that existing behavior.
  - New `MtssConfig.ThresholdSettings` class and `StatListConfig.statThresholds`
    map (keyed by stat name), defaulting to `enabled = false` with
    `goodMin`/`warnMin` matching the exact pre-existing hardcoded values
    (TPS 18/14, FPS 60/30, Ping 80/150, Memory 60%/85%, CPU 50/80) — so
    **zero visual change** for any existing config until a threshold is
    explicitly enabled and edited.
  - `MtssDataHolder`'s six threshold-based color functions
    (`getTpsColor`/`tpsColorFor`, `getFpsColor`/`fpsColorFor`,
    `getPingColor`/`pingColorFor`, `getMemColor`/`memColorForPercent`,
    `getCpuColor`/`cpuColorFor`) now have overloads accepting an optional
    `ThresholdSettings`, falling back to the original hardcoded bands when
    absent or disabled. The old no-arg/value-only overloads are preserved
    and simply delegate with `null`, so no other caller in the codebase
    needed to change.
  - `MtssRenderer.buildLines` looks up each stat's custom threshold and
    passes it through for both classic text-mode coloring and graph mode
    (current-value color and, for the `PER_SEGMENT_THRESHOLD` graph color
    mode, every individual historical sample). The existing precedence is
    unchanged: per-list custom color override still beats per-stat
    thresholds, which still beat the built-in defaults.
  - Speed is **not** part of this system — its existing coloring (gray when
    nearly stationary, yellow above 20 bps, white otherwise) isn't a
    good/warn/bad scale, so folding it in would silently change its
    meaning rather than just make it configurable. See the PR description
    for the full rationale.
  - This step intentionally ships data model + rendering only — no GUI
    controls yet. A `// TODO(step 5)` marks where the editing panel will
    go, matching the same "data model now, GUI later" pattern already used
    for `GraphStyle` above.
- **Custom Thresholds panel** for the five threshold-eligible stats (TPS,
  FPS, Ping, Memory, CPU), resolving the `// TODO(step 5)` left by the
  threshold data model above. Opened from each stat's settings panel (⚙ →
  same panel as Show Prefix / Decimals / Render as Graph) via a new
  **"Custom Thresholds »"** row, following the same nested-panel pattern
  already used for the list context menu's **Color / Scale...** entry:
  - A **Use Custom Thresholds** toggle flips `ThresholdSettings.enabled`;
    when off, the stat keeps using its built-in default thresholds exactly
    as before.
  - **Good** / **Warn** rows let you step the two cutoffs up or down with
    **- / +** (0.5 per click for TPS, whole numbers for FPS/Ping/Memory/CPU),
    clamped to non-negative values.
  - A small gray subtitle under the stat name reads "(higher is better)" for
    TPS/FPS or "(lower is better)" for Ping/Memory/CPU, so the meaning of
    "Good" and "Warn" is never ambiguous for the lower-is-better stats.
  - Adjusting either cutoff automatically corrects the other if needed, so
    Warn can never end up worse than Good for that stat's direction — no
    more nonsensical inverted ranges.
  - Changes apply immediately to the list preview in the same editor screen,
    the same way `decimals` and `Render as Graph` already do, since the
    editor's `MtssRenderer.tickCache()` call already re-derives the cached
    lines every frame — no extra cache-invalidation call was needed.
  - Speed is intentionally absent from this panel, matching its exclusion
    from `THRESHOLD_STATS` in the data model.

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