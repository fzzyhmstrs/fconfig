### Additions
* `Translatable` now has a `hasTranslation()` and `hasDescription` method in case an inheritor can have valid descriptions or translations other than via the typical I18n key check.
* `Entry Widget` has gained the method `widgetAndTooltipEntry`, which attempts to apply a description tooltip to a widget, if applicable.

### Changes
* Number settings now have a default description that describes the valid range for the selection.
* `EntryWidget` is no longer a functional interface

### Fixes
* Fixed blank line at top of description tooltips.
* Fixed `MapListWidget` not properly firing `mouseReleased` on its children when the mouse is released out of that child's bounds.
* Fixed suggestion lists not being scrollable or clickable in certain situations.
* Maps, Sets, Lists, and Ingredients properly display their widgets descriptions, if any.
* `PopupWidgetScreen` now correctly shows tooltips on screen even when popups are open.