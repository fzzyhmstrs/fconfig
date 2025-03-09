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
* New `registerLenientS2C` and `registerLenientC2S` methods in the `NetworkApi`. These methods will be propagated to all versions of Fzzy Config in 0.6.6, though in most versions will have no behavior difference compared to `registerS2C/C2S`

### Changes
* Internal Fzzy Config packet registrations now register leniently.
* `NetworkApi.canSend` now checks that one of it's registered methods can in fact send.


### Fixes
* Fixed clients without Fzzy Config being prevented from joining a server with it. Obviously configs will not be synced in this circumstance, FC will simply pretend that client doesn't exist. Mods that can allow this behavior should of course also handle this circumstance.