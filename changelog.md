### Additions
* Port to 24w40a
* Added a decimal format to `ValidatedFloat` and `Double`; the values within will now be formatted like `#.##`, instead of showing the entire fractional part.
* Added exception if a `ValidatedNumber` is provided with a min >= max.
* Added `Item`, `Block`, `EntityType`, and `Fluid` as types that can be automatically wrapped with validation without having to specify it yourself.

### Changes
* Using `@Translation` at the config-level will add the option to use the prefix itself as a lang key for the config title.

### Fixes
* Fixed tooltip alert for the `RELOAD_RESOURCES` action on config-level alerts
* Fixed `@Translation` not working for config sections
* Inherited config classes fully work again
* Fixed mapped values not instancing themselves properly; which was breaking mapped lists, maps, and other collection views in the GUI.
* Restore config ordering from alphabetical to declared-order in the TOML outputs. Superclasses will appear after the direct class fields.