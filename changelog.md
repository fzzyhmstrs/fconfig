### Additions
* `IgnoreVisiblity` annotation. A config marked with ignore visibility can have private/protected etc. fields/props. This was added for backwards compatibility with configs that may be using restricted visibility to store non-config information.

### Changes
* The changes button narration was changed from "Manage Changes" to "(Number of changes) Changes Made Button"
* The main de/serializers will now access-widen fields when applicable based on `IgnoreVisiblity` state
* try/catch blocks of the main de/serializers expanded to catch more issues without crashing
* Config list entries will narrate their name on hover as well as focus
* Increased max character length of the search box in case of needing to search for long names
* API BREAK - `OnClickTextFieldWidget` now takes an `OnInteractAction` instance instead of a `Consumer<OnClickTextFieldWidget>`. This lets the text field pass key presses to whatever the widget desires (generally a newly pushed PopupWidget element)

### Fixes
* Permission level is now rechecked on GUI opening, preventing you from being locked out of a config if you opened a server config while in the main menu.
* Tab now completes suggestions as it should.
* Changes button now narrates the number of changes made as it should have
* Fixed lang issue with the default descriptions for min- and max-bounded numbers
* The Narrator now properly narrates config list entry tooltips
* Fixed the search bar "trapping" arrow based keyboard navigation even when the search box is empty. Now will pass navigation when it's empty