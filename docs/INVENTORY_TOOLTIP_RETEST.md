# Inventory hover / native-menu retest (draft fix)

## Scope and evidence

The reported symptom is intermittent missing inventory or bag item hover labels until an item is clicked. Its in-game cause is **unconfirmed**.

A deterministic regression was added for one specific menu-ownership bug: after an inventory side panel becomes visible, `PostMenuSort` may occur before the next `PostClientTick`. Previously CGT used the panel bounds captured on the last tick. A rendered custom button occupying the same screen coordinate could then be treated as menu owner, causing `NativeTabMenuBridge.keepOnlyCancel` to discard the item's native menu entries.

The added `InputPipelineTest.newlyOpenedInventoryPanelWinsBeforeTheNextClientTick` failed on the pre-fix branch (61 tests, 1 failure: [red run](https://github.com/FrederickCampbell/custom-game-tabs/actions/runs/43595183251)). The narrow fix re-reads the native side-panel bounds from widgets **on the client thread in `onPostMenuSort`**, before deciding whether CGT owns the menu. A missing mouse position or unsupported layout now exits without rewriting menus. AWT handlers continue using published bounds and do not read widgets directly.

This test demonstrates a real source-level timing bug, **not** proof that it is the only or actual cause of the reported in-game hover symptom. Do not merge or publish before live confirmation.

## Manual acceptance

Use the same RuneLite profile and bag/inventory items that exhibited the problem. Hold the other plugin settings constant while toggling CGT.

1. With CGT **disabled**, hover the affected inventory/bag item without clicking. Record whether the mouse label appears, and whether its right-click menu contains the normal item actions.
2. Enable this branch's development build. Open/close the inventory, switch tabs, and hover the same item **before clicking**. Check that its label and original right-click menu appear without a delay; test a custom dock or drawer placed next to or partly under the native side-panel rectangle.
3. Repeat in **Docked** and **Freeform** layouts, with the drawer both collapsed and expanded, first with Stretched Mode off and then on. Try GPU on/off, window resizing, and after plugin disable/re-enable.
4. Keep Mouse Tooltips/Mouse Highlight settings unchanged for the direct comparison. If the floating label disappears while the item's native right-click options remain present, repeat with Mouse Tooltips disabled to separate that plugin's fallback from the game menu.
5. Retest Combat Options with Sailing Reorderer enabled: left-click must still open the native tab and right-click must retain its third-party options. Also retest all custom tab click/hover and the drawer at a stationary cursor.

## Interpreting results

- **Menu options disappear only when CGT is enabled:** investigate remaining incorrect ownership, hit-region overlap, and native widget exclusion.
- **Menu options remain correct but the floating label disappears:** inspect RuneLite Mouse Highlight tooltip scheduling, `TOOLTIP_TIME`, `TOOLTIP_BUILT`, and other tooltip-rendering plugins; do not rewrite menu entries as a speculative fix.
- **Symptom persists with CGT disabled:** isolate other plugins and RuneLite itself before attributing it to CGT.
- **Issue reproduces only after opening inventory or changing layouts:** concentrate on panel geometry publication and client-tick/frame boundaries.

## Release gate

Run `./gradlew --no-daemon clean test jar` with Java 11 and complete live acceptance with the actual affected profile. Keep the Plugin Hub update closed. Only after the bug is confirmed resolved should any unrelated architecture refactor begin on a **separate branch/PR**.
