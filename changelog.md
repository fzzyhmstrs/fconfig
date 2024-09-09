### Additions
* Created new networking API for facilitating cross-platform play phase networking.
* added new `configure_update list` command to list the current quarantined updates, to help get the proper id names now that the argument for the command is a simple string.

### Changes
* The `configure_update` command now uses a string argument to avoid needing to serialize a custom argument. Fzzy config should work with vanilla clients again.

### Fixes
* Fixed forge 1.20.1 not actually initializing properly