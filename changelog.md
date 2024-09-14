### Additions
* Added new helper extension functions to `FcText`.
  * `MutableText#command` cleanly defines a Run Command click action
  * `MutableText#tooltip` adds a hovered tooltip to text
  * `MutableText#underline` underlines the text
  * `MutableText#bold` bolds the text
  * `MutableText#italic` italicizes the text
  * `MutableText#strikethrough` strikes through the text
  * `MutableText#colored` colors the text with any RGB color
  * `Any#transSupplied` translates the receiver, with a supplied fallback literal if necessary. Useful if the fallback is computationally intensive
  * `Any#descSupplied` describes the receiver, with a supplied fallback literal if necessary. Useful if the fallback is computationally intensive
* Added new `Translation` annotation. Yet another way to translate your in-game config settings!
  * If applied to a config, section, or object, all the settings within will use the prefix provided in their translation keys instead of the normal class-path based one.
  * If applied to a setting, that setting will use the specified prefix. It can also be used to turn off translation for a setting if you want the normal translation scheme to apply for all settings except the negation-annotated ones.
  * With a prefix, your settings will have a lang key of `prefix.[fieldName]` and `prefix.[fieldName].desc`. This applies to all instances of the field, so repeating units of the same setting will share a lang key!
* Added new "Not in Game" uneditable setting to better show that you need to connect to a world to access it.
* Added new client commands that technically can be used in-game or for other purposes. Mostly they are "internal" for use with the changed action messages.
  * `/fzzy_config_restart` shuts down the client
  * `/fzzy_config_leave_game` disconnects you from the current game and returns you to the proper title screen.
  * `/fzzy_config_reload_resources` reloads resources (F3+T)

### Changes
* Action chat messages now include a click event with commands corresponding to the needed action (`RELOG` will have a leave game click action, and so on)

### Fixes
* Config screens properly understand when you are not in a game, or when the config is client-only. Configs accessed outside a game should more accurately display the settings you have access to
* Documentation mod links point to the mod pages now instead of the Modrinth/CF homepage.
* Fixed missing docs and `@JvmStatic` for `ConfigApi#network`