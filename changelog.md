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
* `@RootConfig` annotation for marking a config as a "root" config. The settings will appear "inline" with the landing page config buttons, instead of in its own sub-GUI. All other aspects of the config interaction remain unchanged; loading, saving, calling from, etc. so an existing config can be marked as root with no breakage.
* Added a greyed-out placeholder button for configs that aren't yet loaded but have been promised via the fabric.mod.json or mods.toml.
* New `ConfigApi.isScreenOpen`/`ConfigApiJava.isSceenOpen` methods for checking if a Config GUI is currently open.
* `DynamicListWidget` has a new `scrollToEntry` method for scrolling directly to a list element.
* `ConfigApi.openScreen` now supports passing in scope args for scrolling to them and opening them as applicable. If you have a config `my_mod:config` with a Object setting `coolObject`, passing `my_mod.config.coolObject` to `openScreen` will open the config GUI, scroll to the object setting, and open the object editing popup.
* New `EntryOpener` interface for entries that have something to open on request. This is typically used for validation that has a popup edit menu.

### Changes
* The networking api methods `registerLenient[side]` are now ported to all versions for usage parity.
* If a config is loaded after screens for a mod have been initialized, the manager will be invalidated and rebuilt (as needed) with the new total loaded config set considered.
* `ValidatedColor` popups now have a submit button for the hex string textbox, and the alpha edit box will be completely missing if the color doesn't support transparency.

### Fixes
* GUI keys are no longer pressable "past" an open popup, and multiple of the same popup can no longer be opened with keybinds.
* `ConfigGroup` now has an optional constructor parameter to start the config collapsed.
* Ingredients and Colors now work as keys/values in validated collections. 
  * Ingredients can no longer be interacted with outside of worlds.
* The screen manager now locks while constructing a requested screen to prevent recursive screen building if the construction process somehow calls for opening the same screen.
* `ValidatedColor` properly shows and accepts only 6-digit hex when it doesn't support transparency.
* Popups for `ValidatedIdentifier` and `ValidatedTagKey` properly focus their textboxes on open again, and their textboxes are aligned properly again.
* Fixed Go-to menu scroll bar disappearing if you were dragging it and moved the mouse off of the menu. The scrollbar disappearing when the menu isn't hovered is intended behavior.
* Right click menus properly pass clicks "past" themselves, allowing actions to be taken when "clicking off" of them (including "moving" the menu to the newly clicked spot.)
* Fixed sliders not capturing the change made if the mouse is released off of the slider itself.