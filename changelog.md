## Breaking changes in 0.7.x
* `ValidatedEnumMap` is removed
* Removed all methods and properties marked as Deprecated and for removal 0.7.0
  * `SmallSpriteDecoration#<init>`
  * `SpriteDecorated#textureSet` & `textures` is now a required override
  * `SpriteDecoration#<init>`
  * `CustomButtonWidget#<init>` & builder is now only non-override method for adding custom button
  * Removed `ActiveButtonWidget`
  * Removed `TextlessActionWidget`
  * `DynamicListWidget.EntryPos` and implementations made `internal`
  * `PopupWidget` position elements; `LayoutWidget` no longer inherits from the PopupWidget variant
  * Removed deprecated overrides of `PopupController` from `PopupParentElement`
  * `SuggestionWindowListener` and `SuggestionWindowProvider` moved out of `internal` sub-package
  * Removed `ImmutableRelPos`
  * `RenderUtil#renderBlur`
* Throughout FzzyConfig `Translatable.Result` has been replaced with `Translatable.ResultProvider`. This affects `EntrySearcher` as well as `Searcher.SearchContent`

-------------------------------------

### Additions
* Config updates made in the file (.toml, .json5, etc.) will be automatically updated live in-game, and synced as appropriate.
* New `onUpdateServer` events that take a `ServerUpdateContext`. The old events are deprecated. They will stop working (no crashes) in 0.8.0, with full removal scheduled for 0.9.0
  * For registered events, migrate to the v2 API
* New `RegistryBuilder` platform system for creating modded registries in a loader-agnostic way, along with other registry-related utils.
  * Call via `PlatformApi#createRegistryBuilder`. Much like registrars, a builder is created for a specific namespace.

### Changes
* `ValidatedEnum` now has more widget types, including `INLINE` and `SCROLLABLE`
* `ValidationResult.bimap` error nesting order flipped, the output results error context is now the parent context

### Fixes
* Fixed desc and prefix keys being broken for Config classes
* Config groups, especially deeply nested groups, behave properly now when opening and closing repeatedly