### Additions
* None.

### Changes
* Updated the `ValidatedIngredient` interface to a new popup with two list editors, list viewers, and "Clear" buttons.
* Added a `supplyTooltipOnOverflow` method to `SuppliedTextWidget` that allows for provision of a tooltip in case the text widget overflows and "trims" the input. This can be identical to the text supplier (can be the text supplier instance itself), or can be a separate supplier with, for example, newlines instead of commas separating text elements

### Fixes
* `ValidatedIngredient` now supports both tags and item IDs, instead of just item ids. The constructor now accepts `Set<Object>`, which can be composed of TagKeys and Identifiers (for tags and items, respectively)