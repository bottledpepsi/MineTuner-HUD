# Changelog

All notable changes to MineTuner Statistics Server are documented in this file.

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