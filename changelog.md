### Additions
* Added `ConfigApiJava` which includes methods that may cause IDE issues if called by java code directly from ConfigApi.
* Added `translate` helper method to `ValidatedChoice` for shortcutting common translation/description provider creation.
* Added `min(a,b)` and `max(a,b)` to `Expression`

### Changes
* `ValidatedExpression` widget now includes max and min, and the widget buttons have descriptive tooltips.

### Fixes
* Serializer will now ignore a `@ConvertFrom` file candidate if it is missing (already converted, or never existed), rather than tossing an exception into the log.