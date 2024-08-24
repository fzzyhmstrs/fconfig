### Additions
* None.

### Changes
* `ValidatedChoice` now has the params `translationProvider` and `descriptionProvider`, BiFunctions that let you convert the choices base translation key and an instance of the choice into a Text instance. Useful for creating translations/Descriptions for strings or other choices that aren't `Translatable`.
* `ValidatedList.toChoices` now lets the user define translation and widget behavior.

### Fixes
* Clean up code internally. No external change in behavior