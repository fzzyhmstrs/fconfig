### Additions
* new `ValdiatedCondition` validation wrapper. Gate the wrapped value behind conditional checks added via `withCondition` when `get` is called. If checks fail, a fallback will be supplied instead of the stored "live" value, and the setting will be locked in-GUI.
  * New methods `toCondition` in `ValidatedField` to convert one to a ValidatedCondition
* `Text.isEmpty()` and `Text.isNotEmpty()` extension functions in FcText
* `toLinebreakText` in FcText to convert a list of text into one text split by newlines

### Changes
* Config settings with names that don't fit into the row (truncated with ellipses) will now have the full name appear in the tooltip
* Archived documentation versions below 0.4.0. If you need to reference a specific older version for some reason, feel free to contact me.

### Fixes
* Translatable things now properly have their translations respected if they are wrapped with automatic validation.
* Fixed context menu (right click menu) showing under text in some cases
* Config entries ('rows') are better at compositing their tooltip information onto any native tooltip the containing widget wants to render.
* Improved narration of config entry tooltips; stacking action narrations after the tooltip narration as needed.