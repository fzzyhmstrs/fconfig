# FzzyConfig

### The base API for mods created by fzzyhmstrs (that's me!) Fzzy Core provides a framework for several important features of building content mods!
see the [wiki](https://github.com/fzzyhmstrs/fc/wiki) for more detailed information.

### Including FC in your project
Fzzy Config is stored on Modrinth, so you can easily grab it from there with the following maven and dependencies notation:

**Current Latest Versions:**

_Currently under development_

### Upgrade Plan
- Remove the README system. Instead, we are going to build a GUI system.
- For GUI system and syncing, want:
  - `markDirty()` use to define which elements to sync, instead of whole-config sync?
  - `@NoSync` annotation for Full Client options that the servers shouldn't overwrite
  - `@Restrict` annotation for marking the OperatorLevel the option is, should it be available to all, lvl 2 op, lvl 4 op
  - `@Description` replace the README system with a description system used in the GUI
  - Investigate serializing to/from TOML instead of JSON

### ValidatedField Validation
- validator class? interface? that does... what
  - Validate
  - Accept inputs for Range, Validation checking, etc.
    - for example. ValidatedInt, have Min, Max, callback {val -> ValidationResult<val>}
  - widgetBuild method, provides a functional widget that it can validate against
    - for example, ValidatedBool -> ToggleButtonWidget
  - Have an input chain of Widget action -> `validateAndSet()`
    - Don't want: constant resetting of values while player is typing etc. 
    - Use PopupWindow for widgets that need time
      - Make Entries
      - Player can check over them, maybe a `validate` only method here for helping
      - Submit from the Popup
      - `validateAndSet()`
      - Ex: `ValidatedEnum` would create a Popup with buttons for the valid selections.
    - Utilize CommandSuggestions...? 
      - Config of disabledEnchants = suggest applicable enchants for disabling
- Need:
  - validateAndSet()
  - validateAndCorrectInputs()
  - validate()
  - EntryValidator
  - createWidget()
  - markDirty()
- For:
  - Call-Response of
    - Client closes/accepts/applies their config screen
    - Packet of dirty ValidatedFields sent to Server for config updating
    - Server records fields changed
    - Replicates updates to all clients
    - Live updating of ConfigScreen??
      - Need a check for change of currently-being-edited field

### Screen Construction
Basic component needs of the GUI side of things
- Generic Config Screen Layer that can be utilized at any layer
  - Needs to know its scope
  - Needs to be able to instance its ListWidget
  - Needs to be able to instance its Scope buttons at the top
- Generic Entry
- Changes button
- Apply Button
- Done Button
- Change Log Button (TextlessButton)
- PopupWidget implementation as ParentElement
  - PopupWidgets for List-type fields (List, Map)
  - PopupWidget for Color
  - PopupWidget for forwarding a setting
- ClickableWidgets for all fields