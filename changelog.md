### Additions
* New `isDev` method in `PlatformApi` for checking if the instance is running in a development environment.
* Implement a barebones `Registrar` system for platform-agnostic registration of objects
  * Registers objects into `RegistrySupplier` instances, much like the (Neo)Forge deferred registry system.
* Added `onRegisteredClient` and `onRegisteredServer` to the `EventApi` for listening to config registrations and only acting after the config has been successfully registered.

### Changes
* `ConfigApi.isConfigLoaded(scope)` has been deprecated in favor of `ConfigApi.isConfigLoaded(scope, type)`. This new overload can check for client configs; the now-deprecated method only checked synced ones.
  * Also consider the new register event instead of trying to check for config load yourself.

### Fixes
* None.