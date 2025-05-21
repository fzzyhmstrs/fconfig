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
* New `Translatable.Name`, `Translatable.Desc`, and `Translatable.Prefix` annotations for data generation of lang files
  * Corresponding `ConfigApi.buildTranslations` and `ConfigApiJava.buildTranslations` methods for hooking a config into a data generator
  * Also created a simple registered objects translation builder at `ConfigApi.platform().buildRegistryTranslations` Used for either `RegistrySupplier` objects built by a `Registrar` or `Identifier` used in a traditional registration system.
* Added `ConfigScreenProvider`, allowing for registering of custom screen implementations in place of the Fzzy Config built in
  * API Call `registerScreenProvider` added for registering your provider
* `afterClose` event in `PopupWidget`
* `isPressed` method in `Relevant` interface (which is used by `FzzyKeybind` and `ValidatedKeybind`), which allows for assertive checking for a key state, above the existing reactive response method `relevant`
* Overhauled the error handling system in `ValidationResult` with a new `ErrorEntry` system and dramatically improved process flow for building complex errors and passing exceptions and other context information
  * Also introduced more functional methods like `inmap`, `outmap`, and `bimap`

### Changes
* Upgraded `CustomMultilineTextWidget` to handle click and hover events, as well as now implementing a custom `MultilineText` implementation
* `ValidatedField` bails out of deserialization only on critical errors, now letting correction take its course more often
* The `Registrar` system is no longer marked as experimental. Any further changes to the system will follow the standard released content deprecation and update system.
* Methods using the old error handling system of string lists are marked for removal 0.8.0.

### Fixes
* `ConfigGroup.Pop` properly pops multiple times if attached to one setting multiple times
* Clicking off of a context menu into a slider properly updates the slider value
* Search and Restore Defaults options in the context menu work again