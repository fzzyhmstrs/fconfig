### Additions
* Added `ValidatedString.fromList()` for easy construction of a ValidatedString from a known list of options.
* Added new Entry interface `EntrySuggester` which has the SAM `getSuggestions`
* Added `AllowableStrings` utility, much like `AllowableIdentifiers`. Implements `EntryChecker` and the new `EntrySuggester`
* ValidatedString now utilizes `SuggestionBackedTextFieldWidget` if an AllowableStrings or other Checker that implements `EntrySuggester` is passed as its `EntryChecker`. Using the new `fromList()` automatically does this.

### Changes
* `AllowableIdentifiers` now implements `EntryValidator` and the new `EntrySuggester`

### Fixes
* None.