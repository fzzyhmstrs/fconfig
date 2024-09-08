### Additions
* **Port to (Neo)Forge! This is an initial port, please notify me of any problems you run into in my issue tracker or discord**
* Added `@WithCustomPerms` and `@AdminLevel` for definition of user permissions using LuckPerms/Forge Permissions API "Node" style permissions
  * Added new update quarantine. If an update is suspect, Fzzy Config will quarantine it instead of applying it or outright deleting it. Admins will be notified and have the opportunity to inspect it with a new `/configure_update` command, accepting or denying the update as needed.
* Added new `@RequiresAction` annotation that supplants the functionality of the now-deprecated RequiresRestart
  * RESTART - same functionality from RequiresRestart
  * RELOG - prompts the user to disconnect and reconnect from the world or server
  * RELOAD_BOTH - prompts a reload of both datapacks and resource packs
  * RELOAD_DATA - prompts a reload of datapacks
  * RELOAD_RESOURCES - prompts a reload of resource packs
* New icons for the new Actions alert system that will appear on the left side of the config, replacing the one "!" symbol
* Added `ValidatedString#fromValues` for creation of a Validated string with a vararg set of allowable strings, no list wrapper needed.

### Changes
* Deprecated `@RequiresRestart`
* Permission checks are better at ignoring single player games.
* Tooltips for required actions are now presented when hovering over the icons themselves, rather than as a header in the main setting tooltip. The tooltip when using keyboard navigation now appends action alerts after the setting description, not before, to avoid having to sit through a bunch of alerts before learning what the setting even does.
* Internally refactored all fabric-related code into common access points.
* If a setting forward fails, Fzzy Config now alerts the sender of the problem.
* Removed environment annotations. Modders pay attention to your source sets! This is to facilitate multiloader more effectively.
* Updated or tweaked KDoc in many places

### Fixes
* `ValidatedChoice` now properly updates its tooltip after each selection change.
* Added `ConfigApiJava` into the most recent versions.
* Added missing widget and translationProvider optional parameters in `ValidatedSet#toChoices`