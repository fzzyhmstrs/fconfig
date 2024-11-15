### Additions
* None.

### Changes
* None.

### Fixes
* Fixed `ValidatedField.toSet(collection)` returning a list, not a set.
  * WARNING: This may potentially be a breaking change, if any mods have used `toSet` and accepted that it returns a `ValidatedList` instead, rather than raising an issue with me. Their field will now not match the type returned.