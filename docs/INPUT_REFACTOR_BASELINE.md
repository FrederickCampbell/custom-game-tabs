# Input-handling refactor: baseline and review plan

## Scope

This change is an intentionally narrow, behavior-preserving first slice of the larger Custom Game Tabs architectural refactor. It begins from the **merged inventory-menu guard** on `main` (`4c6ffe688c5124e334a3f891c70a51f10d1579a5`). It does **not** change the inventory/menu-ownership policy, live hitbox geometry, drawing order, preset encoding, tab activation, mouse-listener registration position, or Plugin Hub descriptor.

The AWT `MouseAdapter` is extracted from `CustomGameTabsPlugin` to `TabMouseInputListener`. The new listener knows only a small target contract and `InputGestureCapture` for transient press/drag/click suppression. All overlay positioning, native tab actions, and layout changes still run through the existing client-thread queue in the plugin. The capture flags intentionally stay independent to preserve the old event sequencing, including unusual overlapping capture states.

## Reproducible source baseline

The figures below are GitHub tree source-file sizes, not JAR sizes. The refactor's goal is clearer ownership and testability, **not** a reduction in total bytes at any cost.

| Metric | Merged `main` at 4c6ffe6 | Refactor branch at 0a24da7 |
| --- | ---: | ---: |
| Production Java source files | 27 | 29 |
| Production Java source bytes | 229,493 | 234,462 |
| `CustomGameTabsPlugin.java` bytes | 78,720 | 76,590 |
| `CustomGameTabsPlugin.java` lines | 2,496 | 2,419 |
| New focused gesture/listener test methods | 0 | 11 |

The 2,130-byte decrease in the coordinator is paired with separate responsibilities and new tests. Total source bytes increase as a normal consequence of extracted interfaces, documentation and explicit state transitions.

**Not yet measured:** standard JAR bytes, runtime CPU time, allocations, and line/branch coverage. No performance improvement is asserted. Build the **standard** `jar` task on both revisions using the same Java 11 / dependency versions before comparing compiled size; do not compare the development `shadowJar` with the Plugin Hub artifact.

## Behavioral invariants

1. Retain `mouseManager.registerMouseListener(1, mouseAdapter)`; RuneLite's Stretched Mode translator must execute first, regardless of plugin activation order.
2. AWT mouse callbacks never query widget state, save layouts or activate native tabs directly. The existing `queueInput` lifecycle guards and client-thread execution remain authoritative.
3. A captured tab's left release and subsequent clicked event are swallowed; unrelated native inventory mouse input passes through.
4. Alt-drag begin, preview and finish preserve their queueing, cancellation, and coordinate paths.
5. Opening a menu abandons active capture without hijacking the native menu.
6. Drawer-handle occlusion and the last-rendered hitbox precedence remain unchanged.
7. The PR #6 inventory exclusion is retained; menu actions outside CGT surfaces are not rebuilt.
8. Existing preset formats v1/v2/v3, config keys, native tab actions, Sailing Reorderer's additional options, and tooltip arbitration are unchanged.

## Automated verification

Run with Java 11: `./gradlew --no-daemon clean test jar`. Existing `InputPipelineTest` checks real RuneLite `MouseManager` / Stretched Mode translation, drawer stationary clicks, and client-thread cancellation. New `InputGestureCaptureTest` covers pending click suppression and cancellation. New `TabMouseInputListenerTest` covers an isolated listener's tab input, normal inventory passthrough, Alt-drag, menu-open cancellation, and right-click behavior.

## Live acceptance before merging

Use the affected development-client profile, in **Source Files → Refresh → Build → Launch**. Test native inventory and bag tooltips without first clicking an item; all 14 tab activations; right-click options on Combat Options with Sailing Reorderer; drawer stationary clicking; freeform Alt-drag and snapping; and Stretched Mode before/after CGT enablement. Repeat while opening/closing native side panels and switching layouts. Keep third-party overlay plugins enabled for compatibility checks.

If the existing tooltip symptom returns or an input path differs, keep this PR unmerged and report the exact reproduction. More invasive menu/geometry and layout/preset refactors belong in separate PRs.
