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
* Added zh_cn lang support (thank you Po-Cu on github)
* Added the ability to copy-paste from/to outside of Minecraft into/out of settings
* Added `platform` method to `PlatformApi`. This returns an enum providing `FABRIC`, `FORGE`, or `NEOFORGE`, which can be checked via their method `forgelike()` to quickly see if the platform is one of the forge-like types.

### Changes
* Improved how Registrar generics work so that registrars now return a `RegistryEntry<F>`, `F` being the specific type of the registered object rather than it's more generic supertype (`Item`, `Block`, etc.)

### Fixes
* Removed unneeded metadata warning in 1.20.1 forge version.
* Crash possibly caused with use of `ValidatedIdentifier.ofRegistryKey`
* Fixed Neo instances seeing blank popups under certain inconsistent circumstances
* Fixed issue with ValidatedSet translation
* Fixed decoder expeption on Neo for very large configs
* Fixed `registerAndLoadNoGuiConfig` not properly hiding the config from the GUI in all circumstances
* Fixed translation issue in datagen
* Fixed issues with some packet registration on some more obscure Neo versions