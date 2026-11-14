# In-game acceptance checklist

Run the development client with `./gradlew run` using Java 11. For a Jagex account, follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). Only the user should operate the game. Record RuneLite version, OS/display scale, game layout, enabled plugins and exact steps for a failure. Keep your current settings/profile backed up before testing migration or reset behavior.

Start with the [PR #5 regression retest](PR5_REGRESSION_FOLLOWUP.md#manual-retest): stationary drawer clicks, tooltip duplication and compact sidebar density were missed in the original acceptance run.

## Required golden path

1. Use Resizable Modern. Enable CGT with Stretched already enabled. Click every Main tab and every Drawer tab, including Combat Options and Magic. The visible button alone must respond; former unscaled locations must behave as normal game space. Confirm Combat opens combat settings, not sailing reorder.
2. Repeat with CGT enabled first, then Stretched. Toggle Stretched twice. Test 100%, fractional and 200% display stretch, including unlocked aspect ratio. No re-enable/logout workaround should be needed.
3. Right-click custom tabs and exercise their existing native options, including other plugins' legitimate tab menu additions. Open minimenu across a tab and confirm the menu owns the click. Try existing F-key tab shortcuts and closing the selected native panel.
4. In Freeform, Alt-drag one tab, a sticky group, and the drawer handle. Release Alt mid-drag, leave/re-enter the canvas, and release the mouse. No camera movement or accidental tab activation should occur. Restart and verify placement persisted.
5. Open/close the drawer repeatedly and reverse it during animation. Click overlapping animated buttons. The visible top button/handle must win; empty spacing must pass input through. Test close-after-selection.

## Coverage matrix

| Dimension | Cases | Expected result |
| --- | --- | --- |
| Lifecycle | Enable before login; enable while logged in; logout/login; hop world; disable/re-enable quickly | No stale targets, duplicate listeners/navigation, lost placement or hidden vanilla tabs after disable |
| Rendering | Minimize/restore; resize rapidly; loading screens; hide/change minimap layer using another plugin | No invisible retained input surface; native controls recover on disable |
| Layout | Docked and Freeform; one/14 Main tabs; no Main tabs; all Drawer; all Hidden | Consistent availability and no unrelated click capture |
| Viewport | 765×503 logical; large window; very narrow; sidebar open/closed; maximum size/spacing | Dock/drawer wrap to fit when physically possible; saved preference unchanged; document impossible freeform-group overflow |
| Drawer | Auto/Up/Down/Left/Right; every screen edge; 1/14 tabs | Reachable handle and matching visual/hit positions after flipping/wrapping |
| Native UI | Inventory, bank, shop, equipment, spellbook and side-panel replacements | Native UI wins overlaps; no stale custom menu entries or panel lifetime changes |
| Persistence | Working layout; slots1–3; old v1/v2; current v3; reset/recovery; profile change | Appearance preserved for old presets; v3 restores saved appearance; dock absolute position remains global |
| Appearance | Interface Styles and resource packs toggled while running; widget/global sprite overrides | Icons update and restore correctly without restarting CGT |
| Other overlays | Compact Orbs; RuneLite native overlay dragging; minimap/overlay-hiding plugins individually | No unexpected input theft; record layer conflicts with exact settings |
| Direct conflict | Interface Layout tab management | Test with its tab feature disabled first; simultaneous tab ownership is not certified |
| Accessibility | Keyboard-only sidebar traversal; long labels; OS/UI scaling; tooltips; custom colors | Names/state understandable, controls reachable, focus visible and retained across edits |
| Unsupported layout | Fixed and Resizable Classic, then back to Modern | No interception of unsupported native tabs; sidebar explains support |

## Acceptance gate

Automated regressions cannot verify current RuneScape scripts, display-server behavior, or every plugin combination. Do not mark #1/#2 resolved or publish a Hub update until their reported paths have been tested. If sailing reorder still appears, record the native widget/action involved and any client exception; do not guess a replacement script ID. Report any visual/hit mismatch with logical canvas dimensions and stretched dimensions.
