### Additions
* Added `ValidatedEntityAttribute` for configuring paired EntityAttribute/EntityAttributeModifier instances.
* Added `SuppliedTextWidget`, a generic TextWidget that renders text from a Supplier of text rather than a static text input.
* Added `toSet()` methods in `ValidatedField`, allowing for quick wrapping of sets in the same manner as the pre-existing `toList()` methods.

### Changes
* `OnClickTextFieldWidget` now renders it's displayed text from the beginning, instead of the end.
* `ValidatedString` will throw an exception from the Regex constructor if the regex can't match to the default value given

### Fixes
* None.