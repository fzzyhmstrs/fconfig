## Scheduled API removal in 0.6.0: `ValidatedEntityAttribute`. Reimplement usages of this class as needed.

### Additions
* New `isDev` method in `PlatformApi` for checking if the instance is running in a development environment.
* Implemented a barebones `Registrar` system for platform-agnostic registration of objects. This API is experimental, with scheduled stability by 0.6.0.
  * Registers objects into `RegistrySupplier` instances, much like the (Neo)Forge deferred registry system.
* Added `onRegisteredClient` and `onRegisteredServer` to the `EventApi` for listening to config registrations and only acting after the config has been successfully registered.
* New version of `ValidatedField.validateAndSet`, `validateAndSetFlagged` that accepts `EntryFlag.Flag` (and inspects the field own flags) and changes set behavior based on flags present.
  * `EntryFlag.Flag.QUIET`: flagged field won't call listeners on change
  * `EntryFlag.Flag.STRONG`: field `validateAndSet`/`validateAndSetFlagged` will use strong validation. Weak validation is standard.
  * `EntryFlag.Flag.UPDATE`: field will update its current sync state when the value is set.
* Added overload to `ValidationResult.report` that takes a string consumer directly for immediate reporting of issues.
* New `Codecs` helper class in PortingUtils for handling version-agnostic Packet Codecs. Thanks Mojang.

### Changes
* `ConfigApi.isConfigLoaded(scope)` has been deprecated in favor of `ConfigApi.isConfigLoaded(scope, type)`. This new overload can check for client configs; the now-deprecated method only checked synced ones.
  * Also consider the new register event instead of trying to check for config load yourself.
* Scheduled `ValidatedEntityAttribute` for removal in **0.6.0**. It is completely unused as far as I can tell with a github-wide search, and quite an unstable concept in general. If needed, a similar validation can be built for your own usage.

### Fixes
* Fixed inaccurate docs in `ValidatedEntityAttribute.Builder` and `EntityAttributeInstanceHolder`
* Attempt fix on Github/#24, which appears to be caused by concurrent access of some kind. Client scopes are now synchronized. 