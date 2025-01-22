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
* new validation `ValidatedChoiceList`. Similar to `ValidatedChoice`, but the list version allows for enabling/disabling of none to all of the possible options while the Choice is one and always one of the choices from the options.
  * New `toChoiceSet` helper method in `ValidatedList`, `ValidatedSet`, and `ValidatedChoice` for creation of choice lists from the backing validation.
* Added `testVersion` method to `PlatformApi` for platform-agnostic testing of MC or mod version

### Changes
  * Removed internal `ListListWidget` and `MapListWidget` and replaced them with `ValidatedList.ListListEntry` and `ValidatedMap.MapListEntry` using `DynamicListWidget`s for those widget presentations.

### Fixes
* Fixed suggestion windows not being properly linked up in the config screen.
