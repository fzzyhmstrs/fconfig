### Additions
* Search field now has a tooltip and shows "Search" when the search query is empty
  * Search also now has several modes, described in the tooltip. Search setting descriptions, negate searches, and more.

### Changes
* `ValidatedEnum` popups now center the enum buttons on popups with very long enum names
* Many internal refactors to further move away from referencing version specific MC code outside util classes
* Further implement Custom widgets and remove more vanilla widget references

### Fixes
* Fixed number widgets sometimes freezing if their allowable range was low (<1.0)
* Fixed integer type slider left/right button incrementing on ranges <10 min to max
* Fixed search bar and done button moving strangely on resize
* Fixed setting entry tooltips appearing for entries "tucked" under the header or footer when hovered over