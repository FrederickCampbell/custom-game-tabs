# Changelog

## 2.2.2 - 2026-09-15
- Upgrade new activity presets to snapshot v3 so **Save/Load** restores button size/spacing, opacity, tooltip visibility, and normal/hover/active/border colors exactly as saved.
- Keep v1/v2 presets backward-compatible without inventing appearance they never recorded; loading them preserves the current appearance until they are re-saved as v3.
- Treat each sticky Freeform cluster as one rigid resize component so narrowing the client cannot collapse grouped tabs into the same corner.
- Preserve nearest-edge affinity on live client resize: left-half components keep their left gap, right-half components keep their right gap, with the same behavior vertically.
- Add live canvas-resize detection that reapplies the canonical working snapshot before transient RuneLite overlay clamping can become the new layout.
- Never clamp resolved sticky members independently during snapshot restore/resize.
- Preserve sticky membership across resize and add regression tests for right-edge growth/shrink, independent left/right groups, and too-small canvases.

## 2.2.1 - 2026-09-15
- Own Freeform Alt-drag before RuneLite's generic overlay mover so moving custom tabs no longer leaks into camera movement or independent overlay-origin anchoring.
- Make Stick Together and Snap Buttons coexist: sticky groups move live as rigid units, can snap as units, and are re-detected after drops so newly attached tabs join immediately.
- Re-anchor Freeform snapshots as one rigid shape on client resize instead of scaling each tab coordinate independently, preserving exact internal spacing.
- Add a short eased drawer open/close animation, keep the handle geometrically stable, and migrate only the old isolated top-right default handle beside the Main cluster.
- Add targeted `[CGT 2.2.1]` runtime logging for input ownership, drag groups, snaps, drawer state, and legacy-handle migration.

## 2.2.0 - 2026-09-15
- Turn the Tab Layouts sidebar into the live activity-layout editor while leaving global appearance/behavior controls in RuneLite's normal plugin config.
- Upgrade the three preset slots from position-only snapshots to complete activity layouts: mode, row count, order, Main/Drawer/Hidden state, drawer behavior, freeform positions, drawer-handle position, and Stick Together groups.
- Add Main / Drawer / Hidden tab states so drawer-only tabs remain accessible without forcing permanently hidden tabs back into view.
- Add an OSRS-style drawer with Auto / Up / Down / Left / Right expansion, optional close-after-selection, remembered open state, and a movable handle in Freeform mode.
- Add the one-click Stick Together toggle: currently snapped/touching freeform clusters move as independent groups and separate again without changing position when disabled.
- Preserve legacy 2.1.x position-only presets and upgrade them naturally when re-saved.
- Reapply the canonical working Freeform layout after loading screens/top-level interface rebuilds so tabs do not fall back to RuneLite's transient top-right/default anchor.
- Keep drawer and main-tab activation on the native backing-widget OnOp path; no `client.menuAction` invocation is used.

## 2.1.2 - 2026-09-15
- Fix freeform preset loads collapsing tabs into the bottom-right corner by clearing RuneLite's persisted overlay origin before applying absolute snapshot coordinates.
- Use RuneLite's real overlay dimensions for freeform snapshot scaling/clamping.
- Persist a dedicated working freeform snapshot and restore it on startup/login so loose tabs return to the layout the user actually left.
- Normalize loose-tab placement after Alt-drag so future restarts and preset loads are not affected by mixed RIGHT/BOTTOM overlay origins.
- Preserve current freeform positions when button visibility/order changes rebuild the loose overlays.

## 2.1.1 - 2026-09-15
- Fix RuneLite startup failure caused by the package-private `TabLayoutMode` config enum being inaccessible to RuneLite's dynamic config proxy.
- Add a regression test requiring plugin-defined return types exposed by `CustomGameTabsConfig` to be public.

## 2.1.0 - 2026-09-14
- Replace the legacy Move Separately toggle with explicit Docked and Freeform layout modes while keeping Buttons Per Row for vertical, horizontal, and grid docks.
- Entering Freeform now unlocks the currently visible dock geometry in place instead of reviving historical loose-overlay coordinates.
- Add a lightweight RuneLite sidebar panel with three explicit Freeform preset slots plus non-destructive Reset Working Layout and Restore Previous Layout actions.
- Save preset/recovery positions as resolution-aware snapshots; visibility and button order remain global settings rather than preset state.
- Move Button Order into the Buttons section and remove the obsolete Advanced section.
- Make Show Tab Names off by default for new installs and route optional names through RuneLite's shared tooltip manager, yielding to native or already-queued tooltips.
- Keep loose overlay names stable for RuneLite position persistence even when the game's live tab action label changes.

## 2.0.2 - 2026-09-14
- Keep reconstructed native tab actions as real `CC_OP` / `CC_OP_LOW_PRIORITY` entries so RuneScape executes secondary tab operations normally.
- Use native static-widget menu coordinates (`param0 = -1`, backing STONE component id in `param1`) while preserving `MenuEntryAdded` replay for RuneLite-injected widget options.
- Cache each backing STONE's live primary action label and use it for hover names, with the existing tab definition only as fallback.
- Show the same native hover name for grouped and individually moved buttons.

## 2.0.1 - 2026-09-14
- Fix AWT/client-thread crash by caching native side-panel bounds on PostClientTick.
- Mouse hit-testing no longer calls RuneLite Widget visibility APIs off the client thread.
- Simplify overlay hit-testing to consume render-state snapshots.

## 2.0.0 - local development

- Refactored Java/package naming to `CustomGameTabs*` under `com.freddy.customgametabs`.
- Removed local release-candidate/test naming from the maintained source tree.
- Kept RuneScape responsible for tab and side-panel lifetime; no panel locking, restoration, or interface blocklists.
- Kept native `OnOp` forwarding for left-click actions.
- Added native-widget menu projection for right-click compatibility, including RuneLite-managed widget menu options keyed to the backing tab stone.
- Custom tab surfaces suppress scene/object menu entries underneath them.
- Changed tab rendering to a manual hook after the Resizable Modern minimap layer so the real side panel renders above overlapping custom tabs.
- Kept stateless one-shot snapping and RuneLite's own overlay persistence as the sole position state.
- Preserved only the historical config-group string as an intentional compatibility identifier for existing RuneLite settings.
