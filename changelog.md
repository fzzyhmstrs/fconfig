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
* Several new utilities added to `FcText`
* Added a basic `Provider` system to Validated Fields
  * Use `ValidatedField.translationProvider` to attach a custom translation provider function to a field.
  * Use `ValidatedField.descriptionProvider` to attach a custom description (tooltip) provider function to a field.
  * Use `ValidatedField.attachProvider` to attach an arbitrary value provider. This system is experimental and currently largely unused except for some widget names
  * For validation with titles like "Edit Map..." you can attach a `WIDGET_TITLE` provider to create custom widget labels
  * There will be much more work put into this and related systems in 0.8.0 and beyond
* New `ConfigDeprecated` annotation. Use this to mark a setting as deprecated in the config.
  * It won't appear in GUIs
  * It won't be serialized to save files or networking
  * It WILL still be read in from files
  * This can be used in combination with a `Version` annotation to update a setting while still being able to use the old setting to update/inform the content of the new one.

### Changes
* `ThreadingUtils` (the file watcher utility) now uses kotlin coroutines internally and has more robust startup and shutdown processes
* Updated change detection system; may introduce niche regressions. Please open an issue if you encounter any strange behavior with "actions" 

### Fixes
* Fixed restart detection when syncing for mapped settings (`ValidatedCondition`, `ValidatedMapped`, etc.)
* Fixed action reporting for changes made inside `ValidatedAny`
* (1.20.1) fixed tooltips not showing up in config screens until you "click into" them (or tab in)
* (1.20.1) fixed sliders "stealing" input outside their widget bounds