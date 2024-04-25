### Additions
* `Translatable` now has a `hasTranslation()` and `hasDescription` method in case an inheritor can have valid descriptions or translations other than via the typical I18n key check.

### Changes
* Number settings now have a default description that describes the valid range for the selection.

### Fixes
* Fixed blank line at top of description tooltips.
* Fixed `MapListWidget` not properly firing `mouseReleased` on its children when the mouse is released out of that child's bounds.
* Fixed suggestion lists not being scrollable or clickable in certain situations.