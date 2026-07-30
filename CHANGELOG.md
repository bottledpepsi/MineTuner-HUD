# Changelog

All notable changes to PerfHUD are documented in this file.

## [1.1.0] - Unreleased

### Added
- Keybind to open the PerfHUD editor (default: **H**), in addition to `/perfhud gui`.
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