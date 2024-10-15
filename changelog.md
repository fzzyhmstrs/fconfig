### Additions
* new `ValdiatedCondition` boolean validation. Gate the base boolean value behind secondary checks added via `withCondition` when `getConditionally` is called. If checks fail, the boolean will be locked to false in the config GUI and getConditionally output.
* new `Text.isEmpty()` and `Text.isNotEmpty()` extension functions in FcText

### Changes
* Config settings with names that don't fit into the row (truncated with ellipses) will now have the full name appear in the tooltip

### Fixes
* Translatable things now properly have their translations respected if they are wrapped with automatic validation.
* Fix context menu (right click menu) showing under text in some cases
* Config entries ('rows') are better at compositing their tooltip information with any native tooltip the containing widget wants to render.