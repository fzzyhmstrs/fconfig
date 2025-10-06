## Dependency has been changed to KotlinLangForge temporarily. I intend to revert back if KFF is updated. Or maybe I'll just package the kotlin libs myself, I'm getting tired of Forge kotlin libs.

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
* `ValidatedColor` has a new presets feature. These presets will be displayed in a "sub-pane" to the right of the main color popup.
  * There are pre-defined presets based on MC color enums, or you can define your own presets using `ValidatedColor#withColorPresets`
* New `ValidatedNumber#setFormat` extension function lets you define a custom `DecimalFormat` for your number settings
* PopupWidgets can now have "child" layouts that appear as sub-panes either to the right or below the main popup.
  * Create a child layout with a pair of `pushChildLayout` and `popChildLayout` in the Builder.
  * This child layout can have completely different spacing, padding, and so on.
  * All the elements added to this child will be navigable alongside the main popup.

### Changes
* Configs marked with `SaveType.SEPARATE` can now be opened out of the world (if it's entries can be) without caring about permissions checking, because the clients version will be considered separately anyway
* Deprecated the constructor of `LayoutWidget` in favor of a builder pattern. Migrate any custom LayoutWidget impls asap; removal scheduled for 0.8.0

### Fixes
* (1.21.6+) the Changes widget now properly displays its number of changes
* (1.21.6+) popups properly blur the underlying screen content again
* Fixed `ValidatedCondition` not passing widget size changes to it's delegate widget
* 