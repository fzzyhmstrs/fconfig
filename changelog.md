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
* Various documentation updates for the Provider systems, including a wiki update

### Changes
* `ValidatedField.translationProvider`, `ValidatedField.descriptionProvider`, `ValidatedField.attachProvider`, and the associated `Provider` types are now static (as well as instance methods) so java users won't have to call `.Companion.` anymore (but can)

### Fixes
* Deprecated settings are properly read in from file for handling. They are still ignored in network traffic.
* The options in `ValidatedChoiceList` and similar settings with an "inline" widget layout now properly show the tooltip for the individual options as well as the settings overall tooltip