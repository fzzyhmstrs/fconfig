## Breaking changes in 0.7.0
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
* New `@IgnoreCommentsForDesc` annotation. Use on your config class to have the translation parser ignore `@Comment` and `@TomlComment` for in-game descriptions.

### Changes
* Configs marked with `SaveType.SEPARATE` can now be opened out of the world (if it's entries can be) without caring about permissions checking, because the clients version will be considered separately anyway

### Fixes
* Fixed issue with config parser incorrectly ignoring transients in certain cases
* Config GUI entries no longer show their tooltips from behind the header/footer
* Config groups now scroll correctly when collapsed/opened
* Fixed potential concurrency issue with config registration on Neoforge. All versions have the fix just in case.
* Expressions now print their constants properly in "plain" format (0.0000003) vs (3E-7) so they properly back-parse their printed version.