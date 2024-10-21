### Additions
* None.

### Changes
* None.

### Fixes
* `ValidatedIdentifier.ofRegistryKey` now works properly with the three types of tables that apparently aren't in the normal dynamic registry manager (loot, functions, predicates)
* ValidatedMaps have better popup error reporting
* Error catching overall improved
* Fixed class-level `@Translation` negation not being respected
* `ValidatedAny` recognizes and prioritizes `Translatable` entries like normal config settings do