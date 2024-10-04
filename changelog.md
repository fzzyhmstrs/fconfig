### Additions
* None

### Changes
* `ValidatedEnum` popups now center the enum buttons on popups with very long enum names
* Many internal refactors to further move away from referencing version specific MC code outside util classes
* Further implement Custom widgets and remove more vanilla widget references

### Fixes
* Fixed number widgets sometimes freezing if their allowable range was low (<1.0)
* Fixed integer type slider left/right button incrementing on ranges <10 min to max