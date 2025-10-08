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
* Added new `CustomWidget` interface. This will be used for abstracting all FC widgets away from directly interacting with `Element`, `Widget`, etc. to reduce porting headache going forward. Updates related to CustomWidget will be sprinkled into the next versions.
* New `CustomTextWidget` utilizing said CustomWidget interface.
* Added `syncConfig` method to the `NetworkApi` for manually syncing a server-side config to clients.
* Added uk_ua translation

### Changes
* `CustomMultilineTextWidget` now has an align-right method
* `Relevant` has gained three new methods for modifier checks, `needsCtrl`, `needsShift`, `needsAlt`
* `PopupWidget`'s wrapped `LayoutWidget` now contributes to re-sizing the popup based on the dimensions it would like.

### Fixes
* Validated Collections now resolve their contents lazily on serialize, allowing for proper implementation of mapped registry objects (items, blocks, etc.) in loaders that defer their registration.
* `ConfigGroup` now acts properly with nested `collapsedByDefault`
* `ConfigApi.buildTranslations` can now "see" inside objects that may be wrapping a translated object (such as `ValidatedAny`)
* Configs packet size limit increased to avoid problems with serializing large configs.
* (1.21.9) fixed keybinds showing as "Button 70" etc.
* `ValidationResult.reportTo` no longer reports an error context has header information only.