### Additions
* New `ConfigScreenWidget` for easily creating a textless 20x20 widget to open your config from another screen.
  * Pass in your config's base scope (mod_id usually)
  * Define a position absolutely, or choose a corner to anchor the widget to, and an optional padding (default 4px)
  * Remember to add your new widget to the screens drawable children!

### Changes
* `TextlessButtonWidget` is no longer final; make a custom implementation if you want!

### Fixes
* Fixed `ValidatedField.toSet(collection)` returning a list, not a set.
  * WARNING: This may potentially be a breaking change, if any mods have used `toSet` and accepted that it returns a `ValidatedList` instead, rather than raising an issue with me. Their field will now not match the type returned.
  * Not marking this as a major patch since any mods that did this were working with broken state, not stable API