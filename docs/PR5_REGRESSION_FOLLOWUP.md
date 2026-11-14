# PR #5 live-test regression follow-up

The first live test exposed unreliable hovering/clicking, duplicate labels, and an excessively tall editor. The original 52 headless tests did not exercise the intervals and render ordering responsible. This follow-up remains on the same draft PR; it is not a release or a claim of live acceptance.

## Diagnosis and corrections

| Report | Evidence | Correction |
| --- | --- | --- |
| Intermittent clicks/hover | `onBeforeRender` emptied all hit regions while the previously presented frame remained visible. AWT input can run before the overlays republish them. | Begin-frame now marks coverage without deleting the previous immutable snapshot. Successful rendering publishes a replacement; RuneLite `DrawManager` completion expires regions for omitted overlays. Logout, removal and unsupported-root guards still revoke input. Hover refreshes each frame even when the cursor is stationary. |
| Other listeners can steal input | Appending CGT put ordinary consuming plugin listeners ahead of its controls. Returning to slot zero would again bypass Stretched Mode. | Insert at slot one, reserving the first slot for RuneLite's translator or core overlay listener. Enabling Stretched later still inserts its translator first. Test both startup orders, disable/re-enable, and an earlier ordinary consuming listener. Arbitrary plugins that themselves force priority-zero consumers still require conflict testing. |
| Duplicate Magic/Prayer labels | CGT previously queued its tooltip at the minimap manual layer. Mouse Highlight subsequently adds the native tab action in ABOVE_WIDGETS without checking the existing queue. | Make the presenter one late dynamic ABOVE_WIDGETS overlay, after Mouse Highlight and before TooltipOverlay. Resolve the hovered tab from current rendered geometry and normal input ownership. Existing tooltip/native-tooltip state wins; CGT supplies a fallback only when the queue is empty. No synthetic mouse motion, menu deletion, global tooltip clearing or suppression of game mouse movement. |
| Drawer direction disagrees with wrapping | Direction selection still used `count * step`, while rendering used wrapped dimensions. | `DrawerPlacement` evaluates each direction with `DrawerGeometry`, including cross-axis size and the exact dock-handle gap. Prefer requested direction when it fits; otherwise minimize the translation needed to fit the same rendered extent. Opening/closing does not change that footprint. |
| Sidebar too tall | Every tab occupied a label line plus controls and a 56px row cap. | One 30px row per tab, flexible ellipsized label, full-name tooltip, 26px-high controls and named accessible reorder buttons. Existing config keys, preset formats and appearance choices remain intact. |

Native side panels retain precedence because they draw over CGT. Consuming through them would be a new defect. The tooltip preference continues to control CGT's own fallback only; Mouse Highlight's independent preference is preserved. Unrelated third-party overlays that enqueue duplicate tooltips after this presenter are not universally controlled.

## Regression coverage

- Actual MouseManager + TranslateMouseListener, both enable orders and toggling, with an earlier ordinary consuming listener.
- Real DrawerOverlay and actual mouse dispatch: six open/close clicks at one fixed coordinate, deliberately between frame start and render; same handle location after each render.
- An omitted overlay remains interactive during frame preparation, then loses its targets at draw completion; a rendered overlay retains them.
- Actual Mouse Highlight implementation and RuneLite overlay comparator: either registration order produces one tooltip; CGT fallback still works with Mouse Highlight's interface tooltips disabled, and yields to native tooltip state.
- Wrapped drawer directions account for both dimensions at screen edges.
- Sidebar layout asserts each tab row stays at or below 32px while retaining accessible names and usable arrow targets.

## Verification

All 57 tests pass locally on Java 11 / RuneLite 1.13.6. The standard plugin jar builds successfully. The sidebar was rendered and inspected at 225px width; its tab rows are 30px high and long labels use ellipsis with full-name tooltips. The five added tests cover listener precedence, stationary drawer/frame-boundary input, real Mouse Highlight ordering, wrapped direction selection, and handle menu occlusion; existing lifecycle and sidebar tests were strengthened. Live acceptance remains pending.

## Manual retest

Use the same profile that exposed the regression. Test Stretched off first, then on at your usual scaling. Hover over Magic/Prayer with Mouse Highlight enabled; expect one label. Repeatedly open and close the drawer without moving the cursor, then test all directions near each screen edge. Repeat with GPU on/off, resize, logout/login, disable/re-enable, and native inventory/bank overlap. Inspect sidebar density and keyboard focus. Report the enabled overlay plugins if input still disappears; do not merge until this passes.
