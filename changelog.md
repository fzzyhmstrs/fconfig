## Reminder of breaking changes in 0.6.x
* `ValidatedEntityAttribute` is removed
* `Custom[Widgets]` are moved from the internal widget package to the custom package
* Several widgets and other classes have been deleted
* `PopupWidget` has many deprecations, and probably at least one breaking change despite my best efforts. Deprecations scheduled for removal 0.7.0
* As of 0.6.3, `ActiveButtonWidget` is unused and deprecated, marked for removal by 0.7.0
* As of 0.6.3, `TextlessActionWidget` is unused and deprecated, marked for removal by 0.7.0
* Possibly more, I didn't take great notes

## As of 0.6.0, 1.20.4 and 1.20.6 will no longer be receiving active updates.

-------------------------------------

### Additions
* Fzzy Config's wiki is now hosted with ModdedMC! Check it out:
  * [![Wiki Link Icon](https://i.imgur.com/Ber97Pl.png)](https://moddedmc.wiki/en/project/fzzy-config/docs)
* Added `WidgetEntry` for easy creation of Dynamic Lists wrapping a collection of widgets.
* New widget type `SCROLLABLE` for `ValidatedChoiceList` and `ValidatedChoice` which opens a scrollable and searchable widget list
* `ValidatedChoice` now includes the `INLINE` widget type previously only available on the list version
* `ValidatedChoiceList` now has its own decorator, distinguishing it from a normal list 

### Changes
* __Registrar System__: `RegistrySupplier` now implements `RegistryEntry` directly, as well as passing its reference entry. This includes a breaking experimental change, `getKey` has changed to `getRegistryKey`
* Improved the narration of `ValidatedChoice` and `ValidatedChoiceList`
* Improved the memory footprint of `DynamicListWidget`, deferring several allocations until needed
* Shortened in-GUI changelogs related to Validated Object changes.
* `ConfigScreenManager` now caches config GUI templates incrementally, instead of front-loading all screen templates at once. This has some side effects, namely that each screen now has a separate Update Manager, so restoring defaults, reverting changes, etc. is now sectioned off per-config instead of global to the namespace. The "Root" screen update manager can see any loaded children managers, so changes can be managed from the root screen into any child screens that have been loaded and modified.


### Fixes
* Fixed done button on config screens saying "back" when they should say "done" in certain circumstances
* Fixed `ValidatedAny` popup saying "Revert Changes" for both the revert and restore defaults button
* Certain validation types now properly determine their defaultness and changed state, namely Validated Objects.
* ValidatedCondition now properly considers its conditions when determining default and changed states. A Validated Condition that has failed conditions will always be considered "default"