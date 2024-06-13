### Additions
* Port to 1.21
* Added new (undocumented) `cast` and `nullCast` extension functions for functional programming style of type casting. Ex: val thing: MyType2 = `myType1.cast<MyType2>().withThing().doAnotherThing()`

### Changes
* Optimized `Expression` with pre-compiled shortcuts for many situations; optimized constant usage.
* Expressions now have `hashcode` and `equals` methods for direct comparison.

### Fixes
* None.