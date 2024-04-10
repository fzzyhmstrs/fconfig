# FzzyConfig

Fzzy Config is a powerful configuration engine for Minecraft Mods that meets you where you want to be. Quick and dirty or detailed and featureful, Fzzy Config will work with you. It provides a wide array of features not common in other config libraries:
* All the basics - Everything you expect out of a config lib: Automagical file serialization from plain objects, simple registration and minimal API usage, comments and descriptions. But Fzzy Config goes above and beyond the basics.
* Automatic GUI generation - Generates a GUI for your configs automatically based on the layout and content of your config files. Screens are fully navigable and narratable out of the box. No need for complicated screen builders, no need to even register screens. Everything works automatically and screens are opened for you with a simple API call or command.
  * ModMenu and Catalogue - Automatic integration with ModMenu and Catalogue is provided. 
* Server-Client sync - Configs are automatically synchronized between server and client, and server Operators can push updates to the server configs from their GUI.
  * Setting Forwarding - Players can forward client-sided settings to other players who want the same setup.
* Validation and Correction - Everything in Fzzy Config is backed by a suite of custom validation, correction, and de/serialization tools.
  * Beyond primitive types - Every pre-packaged or custom-built config type has validation and correction as a core concept. Enums, Numbers, Identifiers, Tags, Maps, Lists, etc. 
  * Restrict user inputs - Tightly control what values a player can choose from.
  * Automatic suggestions - For settings like tags and identifiers, suggestions are automatically generated from permissible options, just like command completions.
* Versioned Updates - Implement methods to automatically correct or update inputs from outdated file versions.
* Scrape previous configs - Updating from another config lib? Tell Fzzy Config about the old config file and it will do it's best to scrape it and update the new config file with the old info.
