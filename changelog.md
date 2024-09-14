### Additions
* None

### Changes
* None

### Fixes
* Revert `@JvmStatic` addition in ConfigApi that accidentally breaks code using the previous way, thanks to Kotlins assertion of an "additional" static simply being false (it replaces with a static)