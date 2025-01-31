## Reminder of breaking changes in 0.6.x
* `ValidatedEntityAttribute` is removed
* `Custom[Widgets]` are moved from the internal widget package to the custom package
* Several widgets and other classes have been deleted
* `PopupWidget` has many deprecations, and probably at least one breaking change despite my best efforts
* As of 0.6.3, `ActiveButtonWidget` is unused and deprecated, marked for removal by 0.7.0
* As of 0.6.3, `TextlessActionWidget` is unused and deprecated, marked for removal by 0.7.0
* Possibly more, I didn't take great notes

## Registrar is still marked experimental with anticipated stability by 0.7.0

## As of 0.6.0, 1.20.4 and 1.20.6 will no longer be receiving active updates.

-------------------------------------

### Additions
* New `TextureProvider` interface for generic provision of textures based on active/hovered state.
* `Single` and `Quad` variants of `TextureSet` added for sets with one texture regardless of state, and sets with all four textures different, respectively.

### Changes
* `TextureSet` now implements the `TextureProvider` interface
* `CustomPressableWidget` and therefore `CustomButtonWidget` now use `TextureProvider` instead of directly `TextureSet` in their implementations.
* `textureSet` method of `SpriteDecorated` is now deprecated and defaulted, with a scheduled removal of 0.7.0.
  * `SpriteDecoration` and `SmallSpriteDecoration` constructors with `TextureSet` are likewise deprecated, with new overloads taking `TextureProvider`
* The texture sets in `TextureIds` are now explicitly typed as `TextureProvider` instead. 
* Improved the lazy-loading and/or reduced duplicate loading of certain elements in lists and screens. This effort will expand in the future to further FCs goal of only ever loading a resource upon it being needed.
* `ValidatedChoiceList` now shows highlights around "unselected" options to better indicate that they are still active button elements.

### Fixes
* Fixed `ValidatedChoiceList` popup widgets being too narrow for the popup with very short choice titles.
* Remove stray dev-environment logging from `LayoutWidget`