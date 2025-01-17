## Reminder of breaking changes in 0.6.x
* `ValidatedEntityAttribute` is removed
* `Custom[Widgets]` are moved from the internal widget package to the custom package
* Several widgets and other classes have been deleted
* `PopupWidget` has many deprecations, and probably at least one breaking change despite my best efforts
* Possibly more, I didn't take great notes

## Registrar is still marked experimental with anticipated stability by 0.7.0

## As of 0.6.0, 1.20.4 and 1.20.6 will no longer be receiving active updates.

-------------------------------------

### Additions
* None.

### Changes
* `Registrar` and it's implementations now expect `Supplier<? extends T>` instead of `Supplier<T>`

### Fixes
* None.