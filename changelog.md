### Additions
* Added `ValidatedField` mapping. Validation can be mapped to validation of any other convertible type with the new `map` methods.
  * New helper class `ValidatedRegistryType` maps `ValidatedIdentifier` to registry objects, allowing for easy direct implmentation of setting based on registry objects (Items, Blocks, etc) without having to later map the identifiers yourself.
* `ValidatedField` now has a helper `codec` method for generating a Codec of the underlying type.
* `ValidatedField` now has a listener system that triggers on any `set` of the field. This listener is a `Consumer<ValidatedField<T>>` and is added with `withListener`
* Added `EventApi` and corresponding direct implementations in the `Config` class
  * like other sub-apis, access this Api through the `ConfigApi`
  * `onSyncClient` - fires when a config is synced to a client
  * `onUpdateClient` - fires when a config is updated in-game on the client side
  * `onUpdateServer` - fires when a config is updated in-game on the server side
* Added `ConfigAction`, which can be used to add arbitrary on-click buttons in the Config GUI. Use them to link to your wiki, open a patchouli guide book, give the player an item, etc.
* Added extremely basic `PlatformApi` for simple cross-loader tasks like checking if the game state is client-sided or not.

### Changes
* Specialized widgets no longer internally extend `PressableWidget` or `ButtonWidget`, which was causing issues with Visual Overhaul or any other mod that leaks the constructor of the widget. It is recommended to use the `Custom` variants of those classes for any custom validation implementation.

### Fixes
* `ValidatedIdentifier` can now bind to dynamic registries using the `ofRegistryKey` initializer methods.