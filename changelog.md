## Reminder of breaking changes in 0.6.x
* `ValidatedEntityAttribute` is removed
* `Custom[Widgets]` are moved from the internal widget package to the custom package
* Several widgets and other classes have been deleted
* `PopupWidget` has many deprecations, and probably at least one breaking change despite my best efforts. Deprecations scheduled for removal 0.7.0
* As of 0.6.3, `ActiveButtonWidget` is unused and deprecated, marked for removal by 0.7.0
* As of 0.6.3, `TextlessActionWidget` is unused and deprecated, marked for removal by 0.7.0
* Possibly more, I didn't take great notes

## As of 0.6.0, 1.20.4 and 1.20.6 will no longer be receiving active updates.

-------------------------------------

* ✅ Parity on the new lenient networking api methods
* ✅ Root config annotation for applying a config to the root screen
* ✅ Proper dismounting/remounting of reloadable validation (Identifier from dynamic keys)
* ✅ Put a placeholder greyed-out button for custom scopes provided by metadata that haven't been loaded yet.
* ❌ Re-implement List and Map widgets with dynamic lists now that adding and removing is impl.
* ❌ Separate out a `PopupWidgetController` interface that can be more easily implemented into an existing screen.
* ✅ Pop popups before opening one with keybind, or consume that key in the popup and do nothing with it.
* ✅ Add option to constructor to start group collapsed.
* ✅ Get ingredients working as key/values in collections.
  * ✅ Add requires world tag to ingredients
* ✅ Get colors working as values in a collection (and keys)
* Check on out-of-bounds clicking for sliders in 1.20.1
* ✅ Add locks to screen opening, and add isScreenOpen method
* ✅ Invalidate config screen manager if scope is added after the manager is loaded the first time
* ✅ Allow for scrolling to arbitrary entries in a DynamicListWidget
* ✅ Allow for opening of popups with config screens
  * ✅ Open the screen and then pass in the remaining scope
  * Have an interface for running an action on scope input
    * Actually could be the start of a framework for a CLI
    * Different "calls" into the interface that perform various actions
    * The entry system is this, basically. Evaluate it's fit for purpose in a CLI type system
    * like imagine `configure edit namespace config value` and it prints all the current values of the config
    * then `configure edit namespace config scope set [input]`
    * In this case, I'm looking for the equivalent of `configure edit namespace config scope open`, which for most validation does nothing, but for `ValidatedAny` opens the popup

### Additions
* None.

### Changes
* None.

### Fixes
* None.