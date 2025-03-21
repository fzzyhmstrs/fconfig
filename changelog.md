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
* Configs now support different save file formats beyond just TOML. Current offerings are `TOML`, `JSON`, `JSON5`, `JSONC`. Select your desired format by overriding `fileType` in the config class.
  * JSON5 and JSONC will automatically carry over comments made with `@Comment` or `@TomlComment`

### Changes
* Broke out `PopupController` from `PopupParentElement`, allowing for smoother implementation of popups into existing screens.

### Fixes
* Fixed unnecessary re-saving of configs on single player configuration.