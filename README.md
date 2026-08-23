# Custom Game Tabs

A flexible replacement for RuneScape's Resizable Modern game tabs.

## Features

- Move the tabs as one dock or place each button individually.
- Resize, space, reorder, show, or hide buttons.
- Snap individual buttons into custom rows and columns.
- Customize opacity, colors, borders, and hover labels.
- Hide the original game-tab rails.
- Keep the side panel open and restore the last panel.

## Usage

Enable the plugin, then hold **Alt** and drag the dock or an individual button.

Turn on **Move Separately** to create a custom button layout.

Custom Game Tabs is designed for **Resizable Modern**.

## Interaction model

Custom tab placement and snapping use RuneLite's overlay infrastructure and are presentation-only.

A physical left-click on a custom tab reuses the corresponding RuneScape top-level tab widget's native `OnOp` listener. The plugin does not use `Client.menuAction`, inject mouse or keyboard input, or add a custom server-action menu entry.

When the original Modern tab rails are hidden, their invisible click blockers are neutralized and their original widget state is restored when the plugin is disabled or the layout changes.

## Development

Run the real unit/compliance tests:

```powershell
.\gradlew.bat clean test
```

Build the runnable development jar:

```powershell
.\gradlew.bat shadowJar
```

Launch the RuneLite developer client:

```powershell
.\gradlew.bat run
```

The repository includes RuneLite's official plugin-development `AGENTS.md` guidance and a source-level compliance test to catch common Plugin Hub-forbidden APIs before review.

## License

BSD 2-Clause
