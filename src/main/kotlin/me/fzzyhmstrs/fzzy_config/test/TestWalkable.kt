package me.fzzyhmstrs.fzzy_config.test

import me.fzzyhmstrs.fzzy_config.annotations.NonSync
import me.fzzyhmstrs.fzzy_config.impl.Walkable

/*
Wanted result:
test.walkable.testProp
test.walkable.testString
test.walkable.testChildDave
test.walkable.testChildDave.testChildBool
test.walkable.testChildDave.testChildList
test.walkable.testChildPolly << IF ignoreNonSync
test.walkable.testChildPolly.testChildBool << IF ignoreNonSync
test.walkable.testChildPolly.testChildList << IF ignoreNonSync

ignore nonSync: 100%
X [test.walkable.testChildDave,
X test.walkable.testChildDave.testChildBool,
X test.walkable.testChildDave.testChildList,
X test.walkable.testChildPolly,
X test.walkable.testChildPolly.testChildBool,
X test.walkable.testChildPolly.testChildList,
X test.walkable.testProp,
X test.walkable.testString]

follow nonSync: 100%
X [test.walkable.testChildDave,
X test.walkable.testChildDave.testChildBool,
X test.walkable.testChildDave.testChildList,
X test.walkable.testProp,
X test.walkable.testString]

 */

class TestWalkable: Walkable {

    var testProp = 20

    var testString = "Hello Werld"

    var testChildDave = TestWalkableChild()

    @NonSync
    var testChildPolly = TestWalkableChild()

    class TestWalkableChild: Walkable{

        var testChildBool = true

        var testChildList = listOf(1,3,5,7,9)

        @Transient
        var testChildIgnoreMePls = "pls"

    }

}