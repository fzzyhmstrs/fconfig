## Reminder of breaking changes in 0.6.x
* `ValidatedEntityAttribute` is removed
* `Custom[Widgets]` are moved from the internal widget package to the custom package
* Several widgets and other classes have been deleted
* `PopupWidget` has many deprecations, and probably at least one breaking change despite my best efforts
* Possibly more, I didn't take great notes

## Registrar is still marked experimental with anticipated stability by 0.7.0

## As of 0.6.0, 1.20.4 and 1.20.6 will no longer be receiving active updates.

-------------------------------------

### Additions
* New `drawNineSlice` and `renderBlur` methods in `RenderUtil` for matching method signatures across versions. Part of an ongoing effort to unify the API across all versions.
* Added `open  ContextMenuPopup` to `Popups` for easy third party creation of context menus.
* New `flatBuild` in `ContextResultBuilder` that flattens the context map groups into one map.

### Changes
* None.

### Fixes
* `ContextResultBuilder` build and other utility methods no longer accidentally internal.
* (1.21.4) fixed the included version of Fabric Permissions API being outdated.