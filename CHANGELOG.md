# Changelog

## Unreleased

- Replaced `Client.menuAction` tab activation with RuneScape's native top-level tab `OnOp` listener.
- Reduced repeated hidden-rail widget writes while preserving the existing restore behavior.
- Removed unused layout/index helpers and aligned the Gradle build more closely with RuneLite's example plugin.
- Replaced the old assertion-only self-test with real JUnit tests and added a Plugin Hub compliance test.
- Added the official RuneLite Gradle wrapper and plugin-development `AGENTS.md`.
- No user-facing features were removed.

## 1.0.0

- Movable grouped and individual game-tab layouts.
- Adjustable button size, spacing, order, and visibility.
- Button snapping for custom rows and columns.
- Custom colors, opacity, borders, and hover labels.
- Optional replacement of the original game-tab rails.
- Side-panel lock and last-panel restoration.
