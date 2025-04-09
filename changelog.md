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

### Additions
* Improved average comnfig load time by approx. 10%.

### Changes
* `ValidatedKeybind` now stores the keycode in the output file based on a string key, rather than the raw int; but it will still accept the raw int for up-conversion purposes or if the key falls outside the typical set of GLFW keys.
  * The format for the key is the same as the minecraft one with the `key.[type]` prefix removed (except for mouse buttons, those start with `mouse.`)
  * For example, page down is `page.down` versus the minecraft `key.keyboard.page.down`
  * And right click is `mouse.right` versus `key.mouse.right`
* `ValidatedTriState` will now accept boolean inputs from the file, if a user accidentally uses `true` or `false` instead of the intended enum form `"true"` or `"false"`
* "Excess" fields (fields that used to exist in a config but no longer do, for example) are now reported as deserialization errors and removed from the read file. 

### Fixes
* Integer-type text-box number validation no longer accepts decimal inputs, and the text-box in general no longer accepts any characters except numbers, the minus sign, and decimal if it is a floating-point number.
* Fixed the search passing text not dynamically updating based on current pass-fail state of the input test.
* Validated Any now properly translates basic settings (again, don't know when this broke)
* Fixed servers not properly parsing updates sent from the client, introduced in 0.6.7
* Config screen managers are now properly invalidated on joining a new world (with potentially new config values to care about)
* Narration of the search bar and search bar option buttons works better, and can recover better from being "interrupted"
* Deserialization fixes:
  * Basic validation (plain fields) now properly report their errors, leading to a correction of the config file as needed.
  * `ValidatedAny` is now robust against changing the number of fields in the wrapped object. Previously adding fields and then trying to read the pre-existing config file would result in total failure for the object, reverting to defaults.