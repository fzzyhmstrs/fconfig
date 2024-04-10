# FzzyConfig

Fzzy Config is a powerful configuration engine for Minecraft Mods that meets you where you want to be. Quick and dirty or detailed and featureful, Fzzy Config will work with you. It provides a wide array of features not common in other config libraries:
* Automatic GUI generation - Generates a GUI for your configs automatically based on the layout and content of your config files. Screens are fully navigable and narratable out of the box.
* Server-Client sync - Configs are automatically synchronized between server and client, and server Operators can push updates to the server configs from their GUI.
  * Setting Forwarding - Players can forward client-sided settings to other players who want the same setup.
* Validation and Correction - Everything in Fzzy Config is backed by a suite of custom validation, correction, and de/serialization tools.
  * Restrict user inputs - Control what values a player can choose from
  * Automatic suggestions - For settings like tags and identifiers, suggestions are automatically generated from the permissible options, just like command completions.
* Versioned Updates - Implement methods to automatically correct or update inputs from outdated file versions.
* ModMenu and Catalogue - Automatic integration with ModMenu and Catalogue is provided.
* Scrape previous configs - Updating from another config lib? Tell Fzzy Config about the old config file and it will do it's best to scrape it and update the new config file with the old info.
