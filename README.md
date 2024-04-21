# FzzyConfig

## Depending on Fzzy Config
Add the following to your build script to depend on Fzzy Config:

#### Repositories section
``` Kotlin
// kotlin
maven {
    name = "Modrinth"
    url = uri("https://api.modrinth.com/maven")
    content {
        includeGroup("maven.modrinth")
    }
}
```
``` java
// java
maven {
    name = "Modrinth"
    url = "https://api.modrinth.com/maven"
    content {
        includeGroup "maven.modrinth"
    }
}
```

#### Dependencies section:
``` kotlin
// kotlin
val fzzyConfigVersion: String by project //define this in your gradle.properties file
modImplementation("maven.modrinth:fzzy-config:$fzzyConfigVersion")
```
``` java
// java
modImplementation "maven.modrinth:fzzy-config:${project.fzzyConfigVersion}"
```

**NOTE: `include` or similar jar-in-jar mechanisms is not allowed in the terms of the [TDL-M license](https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified) that Fzzy Config is released under**

Fzzy Config is a powerful configuration engine for Minecraft Mods that meets you where you want to be. Quick and dirty or detailed and featureful, Fzzy Config will work with you. It provides a wide array of features above and beyond what you can find in other libraries.

### All the basics
* Automatic serialization
* Minimal API with lightweight integration requirements

### GUIs
* Automatic GUI generation based on the layout and content of your config files.
* Screens are fully keyboard-navigable and narratable out of the box.
* No need for screen builders or registration.
* ModMenu and Catalogue automatically integrated.

### Synchronization 
* Automatic server-client sync
* Push live updates to a server from a client
* Forward client settings to other users who want your setup.

### Validation
* Validation and Correction built into everything, beyond just primitive types. 
* Tightly control what values a player can choose from and how they choose them.
* Automatic suggestions for settings like tags and identifiers.

### Updates
* Versioned Updates supported with one simple method implementation
* Scrape previous configs to easily update from another config library

### Flexibility
* Implement a config with as little or much effort as you want
* Plain java object, or Minecraft GameOptions-like carefully crafted settings
