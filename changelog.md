## Version 0.6.0 implements several breaking changes, please update implementations as needed, and feel free to reach out to discuss issues that come up.
* `ValidatedEntityAttribute` is removed
* `Custom[Widgets]` are moved from the internal widget package to the custom package
* Several widgets and other classes have been deleted
* `PopupWidget` has many deprecations, and probably at least one breaking change despite my best efforts
* Possibly more, I didn't take great notes

## Registrar is still marked experimental with anticipated stability by 0.7.0

## As of 0.6.0, 1.20.4 and 1.20.6 will no longer be receiving active updates.

-------------------------------------

### Additions
* Created a ground-up list widget system with `CustomListWidget` and the built-in implementation `DynamicListWidget`. This new list is more powerful than the vanilla lists, allowing for varying heights for each element, hiding/unhiding individual entries, built in searching of entries, and more.
* Created `Searcher` system for building simple search implementations from a collection of elements.
* `ValidatedPair` and corresponding method `ValidatedPair.pairWith`. This new validation joins two settings into one, and displays their widgets side-by-side. This is convenient for number ranges and so on.
* Validated numbers now have a third widget type, `TEXTBOX_WITH_BUTTONS`. The standard entrybox also has a small up and down button with this layout.
* Added custom ScreenNarrator implementation for config screens with several improvements and bug fixes over the vanilla narrator.

#### Translatable Updates
* `Translatable` now includes a `prefix` element. Add a lang key for a setting, section, or config to add inline "prefix text" that appears above the setting or at the top of the setting list, respectively. This pairs well with the next addition
* For implementations of `Translatable`, be sure to override `hasPrefix`, `prefix`, and `prefixKey` as needed.

#### Config Groups
* Added new Config Group system. Groups are inline, collapsible collections of settings. Groups are an excellent place to add a prefix, to provide a general description of what the settings inside the group do.
* Groups can be nested, though this is probably not needed in most circumstances

#### Screen Anchors
* Configs, Sections, and Groups are now Anchors, much like anchors in a web page.
* A new go-to menu in the bottom left (accessible by pressing Ctrl + E also) allows you to quickly navigate between all the anchors of the current config namespace.

#### Context Action System
* Created new keybind-like `ContextAction` system that allows for powerful handling of context actions and key presses.
* Automatically builds context menus as applicable to the right-clicked element.
* Handles complex keybinds like ctrl-shift-C, etc.
* Handles inputs in a layered manner, allowing each layer to only capture the inputs it cares about, passing the input
* Added several new keybinds
  * F1: Opens info screen
  * Ctrl + E: Opens goto menu
  * Backspace: Return to the previous config screen
  * Home: Scrolls to the top of the config list
  * End: Scrolls to the bottom of the config list 
  * (Context menu): You can now fully clear collection settings with the Clear command.

#### EntryCreator
* New system for creating list entries in an orderly manner
* `ValidatedField` now has several helper methods for easily creating custom entries without needing to fully re-implement the base design.

#### LayoutWidget
* `LayoutWidget` added, a generalization and improvement of the system in `PopupWidget`. This widget can be used to create automatically laid out collections of widgets using a DOM-like layout model.
* Entry creators and several other systems in FC now use these layouts.

### Changes
* The Result Provider API is promoted to stable
* The `Pos` system now implements `Supplier`
* Overhauled the `Decoration` system, now `Decorated`, a simple Drawable-like interface that allows passing of different types of icons to various FC systems, dynamically rendered, animated, etc.
* Most custom widget implementations now use a `TextureSet` system for defining textures to use in various hovered and focused states

### Fixes
* Fix apparent concurrent modification problem with screen scope registration.
* Moved catalogue compat back to somewhere that Catalogue can actually find it.
* Fix crash on suggestion windows trying to substring empty suggestions.
* Fix config objects that don't implement `equals` not mapping properly between de/serializations, which improperly caused resets of data.
* Fixed bug with update de/serialization not working properly on nested config sections