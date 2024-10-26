### Additions
* Added a flag system `EntryFlag` to `ValidatedField` and `ConfigAction`. Currently only has one flag `REQUIRES_WORLD`. A field marked with REQUIRES_WORLD marks that the player needs to be in-game for the setting to work properly. The config screen will show Not in Game instead of the setting, when not in game.
* Added `ofDynamicKey` builder methods to `ValidatedIdentifier`. Use of `ofRegistryKey` for non-synced dynamic registries (Loot registries, or modded dynamic registries that don't sync, primarily) is now deprecated and will log a warning. `ofDynamicKey` handles synchronization of predicated registry lists more robustly.
* Added zh_tw translations

### Changes
* `ValidatedIdentifier` now automatically applies `REQUIRES_WORLD` to validation for dynamic registries, which require the world to be loaded. They will now not be available out of game.
  * Validation wrappers (lists, ValidatedCondition, etc.) will inherit their delegates flags automatically
* ConfigActions based on `ClickAction.RUN_COMMAND` or `ClickAction.SUGGEST_COMMAND` now automatically apply `REQUIRES_WORLD`

### Fixes
* `ValidatedIdentifier` without validation no longer tries to "force" you to use the minecraft namespace in the text box
* Enums revert to their proper default when automatically validated (plain field in the config)
* Config actions now respect permissions