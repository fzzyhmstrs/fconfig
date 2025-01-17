# FzzyConfig

## Depending on Fzzy Config
Add the following to your build script to depend on Fzzy Config:

#### Repositories section
```kotlin
// build.gradle.kts

//version 0.4.3 or higher:
maven {
    name = "FzzyMaven"
    url = uri("https://maven.fzzyhmstrs.me/")
}

//version 0.4.2 or lower:
maven {
    name = "Modrinth"
    url = uri("https://api.modrinth.com/maven")
    content {
        includeGroup("maven.modrinth")
    }
}

//for (neo)forge
maven {
    url = URI("https://thedarkcolour.github.io/KotlinForForge/")
}
```
```groovy
// build.gradle

//version 0.4.3 or higher
maven {
    name = "FzzyMaven"
    url = "https://maven.fzzyhmstrs.me/"
}

//version 0.4.2 or lower
maven {
    name = "Modrinth"
    url = "https://api.modrinth.com/maven"
    content {
        includeGroup "maven.modrinth"
    }
}

//for (neo)forge
maven {
    url = "https://thedarkcolour.github.io/KotlinForForge/"
}
```

#### Dependencies section:
Version names will be in the form `x.x.x+[mc_version]`, e.g. `0.4.3+1.21`. For (Neo)Forge builds, add `+neoforge` for 1.20.4+ and `+forge` for 1.20.1.

Either CurseForge, Modrinth, or the [Maven itself](https://maven.fzzyhmstrs.me/me/fzzyhmstrs/fzzy_config/) can be used to see the current version listings.

```kotlin
// build.gradle.kts

//version 0.4.3 or higher
val fzzyConfigVersion: String by project //define this in your gradle.properties file
modImplementation("me.fzzyhmstrs:fzzy_config:$fzzyConfigVersion") //NOTE: underscore, not hyphen!

//version 0.4.2 or lower
val fzzyConfigVersion: String by project //define this in your gradle.properties file
modImplementation("maven.modrinth:fzzy-config:$fzzyConfigVersion")
```
```groovy
// build.gradle

//version 0.4.3 or higher
modImplementation "me.fzzyhmstrs:fzzy_config:${project.fzzyConfigVersion}" //NOTE: underscore, not hyphen!

//version 0.4.2 or lower
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
* Add _Action Buttons_ to open a wiki site, run a command, and so on.

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
