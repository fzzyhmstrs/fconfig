### Additions
* Added `ValidatedEntityAttribute` for configuring paired EntityAttribute/EntityAttributeModifier instances.
* Added `SuppliedTextWidget`, a generic TextWidget that renders text from a Supplier of text rather than a static text input.
* Added `toSet()` methods in `ValidatedField`, allowing for quick wrapping of sets in the same manner as the pre-existing `toList()` methods.

### Changes
* `OnClickTextFieldWidget` now renders its displayed text from the beginning, instead of the end.
* `ValidatedString` will throw an exception from the Regex constructor if the regex can't match to the default value given
* `ValidatedDouble` or `ValidatedFloat` with small ranges now have better slider scaling with keyboard navigation, instead of sticking to increments of 1.0

### Fixes
* Fixed `configure` command including all screen subscopes, not just the relevant subscopes
* PopupWidget fires mouse released on things when dragged out of bounds
* Fixed `ValidatedEnum` client crash related to certain types of Enums.
* Fixed `OnClickTextFieldWidget` improperly trapping keyboard navigation.