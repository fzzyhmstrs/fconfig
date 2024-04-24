### Additions
* Added `ValidatedString.fromList()` for easy construction of a ValidatedString from a known list of options or a supplier of potential options.
* Added new Entry interface `EntrySuggester` which has the SAM `getSuggestions`.
* Added `AllowableStrings` utility, much like `AllowableIdentifiers`. Implements `EntryChecker` and the new `EntrySuggester`.
* ValidatedString now utilizes `SuggestionBackedTextFieldWidget` if an AllowableStrings is passed as its `EntryChecker`. Using the new `fromList()` automatically does this.
* Added `ValidatedIdentifier.ofSuppliedList()` to construct validation with lists that aren't tags/registries but may not be fully known at validation time.

### Changes
* `AllowableIdentifiers` now implements `EntryValidator` and the new `EntrySuggester`.

### Fixes
* None.
