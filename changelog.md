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
* Updated to 1.21.5
* Searches now propagate through sub-menus and other "children" results that aren't themselves valid but contain sub-entries that are valid will show a dashed outline and the tooltip will list the valid sub-entries
  * Searches can now be automatically passed to sub-menus. By default, alt-click will pass the search
  * The main search bar now has some buttons! A menu button which opens the new search config menu, and a clear button to quickly clear the search bar.
* Added new `SaveType` method in `Config`
  * `OVERWRITE` - Client configs will be overwritten when receiving a sync from a server. Default and previous behavior
  * `SEPARATE` - Client configs will not be saved locally when updated from a server. Actions that modify gamestate before sync can't be included in these config type (`Action.RESTART` and `Action.RELOG`), as they won't be able to properly sync up this game state if they can't overwrite the local file.
* New `Translatable.ResultProvider` super-class for more nuanced and efficient storage of translation results. Currently, half-wired-in until 0.7.0.
  * Scope-based `Result` can be cached with the new `Translatable.createScopedResult`
* New function utilities for suppliers, functions, and predicates that always return the same value

### Changes
* `Translatable.Result` now implements `Searcher.SearchContent` directly, and is now deprecated in favor of the new `ResultProvider`
  * In 0.7.0, all Result constructors will be made internal in favor of using `Translatable.createResult`/`Translatable.createScopedResult`
* `ConfigEntry` can now process searches using the `ContentBuilder.searchResult` method. This presents valid "child" search results when the parent list is searched.
* `ValidatedAny` now has its own search bar
* `ConfigScreenManager` passes Config and entry Content misc. context to entry creators

### Fixes
* Fixed unnecessary re-saving of configs on single player configuration
* Fix a variety of edge cases and niche issues involving searching
* Popups for settings that are translated with `@Translation` (or the entire class is marked with `@Translation`) will now properly render the translated name in the popup header
* Search filtering now properly resets when a screen is returned to. The search bar menu has a setting to enable caching behavior where the search will be maintained when the screen is re-opened from a child.
* Fixed accidental niche API break of `ConfigGroup` involving kotlin constructors.