# Custom Game Tabs

Custom Game Tabs replaces the visible Resizable Modern game-tab rail with configurable RuneLite overlays while keeping RuneScape's native tab widgets as the behavioral source of truth.

## Design

- Direct left-clicks reuse the native tab `OnOp` listener. Reconstructed right-click actions remain native `CC_OP` / `CC_OP_LOW_PRIORITY` entries so RuneScape performs the selected widget operation.
- The backing `STONE` widgets remain in RuneScape's normal hierarchy and provide action labels, listeners, and widget identity. The plugin does not maintain its own panel-open state.
- `MenuEntryAdded` is replayed with the backing component id so RuneLite-managed widget menu options can attach to a custom tab.
- Custom tab surfaces suppress scene/object/NPC menu entries underneath them, like native UI controls. Once RuneScape's minimenu is open, it owns mouse input.
- Custom tabs render after the Resizable Modern minimap layer and before the native side-panel layers, so the real side panel visually and interactively wins overlaps.
- `client.menuAction(...)` is never used. Game-tab activation follows the native `OnOp` path accepted during Plugin Hub review.

## Activity layouts

The **Tab Layouts** sidebar is the live editor for activity-specific layouts. Each of the three saved layouts stores the complete setup rather than positions alone:

- Docked or Freeform mode and Buttons per row
- Tab order
- Per-tab **Main / Drawer / Hidden** state
- Drawer enabled state, direction, open/closed state, and close-after-selection behavior
- Freeform tab positions and the Freeform drawer-handle position
- **Stick Together** state and detected snapped groups
- Button size/spacing, opacity, tab-name tooltip visibility, and button/border colors

Loading a newly saved layout restores both its geometry/behavior and its saved visual appearance. Older v1/v2 presets remain readable; because they never recorded appearance, they keep the current appearance when loaded and gain appearance data the next time they are saved.

## Drawer

Tabs assigned to **Drawer** stay out of the main layout but remain available through a compact OSRS-style drawer handle. Truly **Hidden** tabs do not appear in either place.

The drawer supports **Auto / Up / Down / Left / Right** expansion with a short eased slide/fade animation. Auto chooses a useful direction from the current dock/handle geometry and explicit directions flip when necessary to keep the drawer on-screen. In Freeform mode the drawer handle itself can be Alt-dragged.

## Freeform behavior

- Freeform positions are stored in a canonical activity snapshot and reapplied after loading screens, world transitions, and rebuilt top-level interfaces instead of falling back to RuneLite's transient default/top-right anchor. Window-size changes re-anchor the layout as one rigid shape so internal button spacing remains pixel-identical.
- One-shot button snapping remains available with or without **Stick Together**. When enabled, an entire snapped/touching cluster moves and snaps as one rigid unit, and newly attached tabs join the detected cluster immediately.
- Turning **Stick Together** on freezes the currently snapped/touching clusters as independently movable groups. Turning it off leaves every tab exactly where it is and makes them independent again.
- Reset Working Layout changes only the current layout; saved presets are untouched. Restore Previous Layout recovers the complete setup captured before the last reset or preset load.

## Appearance and global behavior

RuneLite's normal plugin configuration remains the live editor for button size/spacing, opacity, colors, and optional tab-name tooltips. Those appearance values are captured into newly saved activity layouts so **Load** reproduces what the layout looked like when it was saved. **Snap Buttons** remains a global interaction preference rather than part of a preset.

Tab-name tooltips are off by default. When enabled, the plugin uses RuneLite's shared tooltip pipeline and yields when the game or another RuneLite plugin already has a tooltip active or queued.

## Layout support

Resizable Modern (`TOPLEVEL_PRE_EOC`) only.

## Development

The public plugin name is **Custom Game Tabs**. Java implementation classes live under `com.freddy.customgametabs`. The persisted config group remains `verticaltabsreplacement` intentionally so existing RuneLite profiles and enable-state behavior are not broken by the code refactor.
