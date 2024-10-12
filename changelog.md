### Additions
* New _**experimental**_ Result Provider api; providing a framework for reflectively accessing any config value via string scopes
  * Create result providers for any configurable type, providers soft fail to fallbacks given during creation
  * Results are dynamically updated as the config changes, no need for relogging, reloading, etc.
  * Built in argument handling system for performing actions on the scope requested - check the size of a list, check if a list contains a value, scale a result, anything you can dream up.
  * Check the [wiki article](https://github.com/fzzyhmstrs/fconfig/wiki/Result-Providers) for an example use case.

### Changes
* None.

### Fixes
* Restart screen now properly shows when a restart-causing change is received.
* Pressing tab on suggestions now properly tabs the result into the setting text field instead of changing focus to the next widget (done button etc.)
* `ValidatedIdentifier.ofRegistryKey` now takes `? extends Registry` like the other helper method types, instead of just `Registry`.