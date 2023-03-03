package me.fzzyhmstrs.fzzy_config.config

import me.fzzyhmstrs.fzzy_config.config_util.*
import me.fzzyhmstrs.fzzy_config.config_util.validated_field.*
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object FcTestConfig:
    SyncedConfigWithReadMe(
        "fc_test_config",
        "test_README.txt",
        headerText = Header.Builder()
            .add("testing.translation2")
            .literal()
            .add("I'm trying to do lots of cool automagical stuff whee")
            .space()
            .add("So cool")
            .space().build()
        ) {

    private val testConfigHeader = Header.Builder().space().box("TEST CONFIG").space().add("This is a config about testing the new fzz core config system").build()

    fun printTest(){
        println("test_section_1")
        println(test.testSection_1.test_Int_1.get())
        println(test.testSection_1.test_Int_2.get())
        println("inner section")
        println(test.testSection_1.innerSection_1.test_Float_1.get())
        println(test.testSection_1.innerSection_1.test_Double.get())
        println("")
        println("test_section_2")
        println(test.testSection_2.test_Enum.get())
        println(test.testSection_2.test_Bool.get())
        println("")
        println("test_section_3")
        println(test.testSection_3.test_Id.get())
        println(test.testSection_3.test_List.get())
        println("")
        println("test map")
        println(test.testMap.get())
        println("")
        println("test color")
        println(test.testColor.getAsInt())
        println(test.testColor.getAsColor())

    }

    class Test: ConfigClass(testConfigHeader){
         var testSection_1 = TestSection1()
         class TestSection1: ConfigSection(Header.default("Test Section 1 Header")){
            @ReadMeText("Testing creating a custom readme entry for a field")
            var test_Int_1  = ValidatedInt(0,5,-5)
            var test_Int_2 = ValidatedInt(1000,10000000)
            @ReadMeText(header = [" >> Testing a custom header-only annotation"])
            var innerSection_1 = InnerSection()
            class InnerSection: ConfigSection(){
                var test_Float_1 = ValidatedFloat(1f,6f)
                var test_Double = ValidatedDouble(0.0,1.0)
            }
        }

        @ReadMeText("","Testing overriding a sections readme", ["","Testing an annotated header"])
        var testSection_2 = TestSection2()
        class TestSection2: ConfigSection(Header.default("Test Section 2 Header")){
            var test_Enum = ValidatedEnum(Testy.BASIC,Testy::class.java)
            var test_Bool = ValidatedBoolean(true)
        }

        private val section3Header = Header.Builder().literal().space().overscore("Test Section 3 Header").space().build()

        var testSection_3 = TestSection3(section3Header)
        class TestSection3(section3Header: Header): ConfigSection(section3Header){
            var test_Id = ValidatedIdentifier(Identifier("redstone"), {id -> Registries.ITEM.containsId(id)}, "ID needs to be in the item registry.")
            var test_List = ValidatedList(
                listOf(1, 3, 5, 7),
                Int::class.java,
                {i -> i > 0},
                "Values need to be greater than 0"
            )
        }

        @ReadMeText("Some more Readme testing for this map of strings and booleans")
        var testMap = ValidatedStringKeyMap(
            mapOf(
                "minecraft:diamond" to true,
                "minecraft:stick" to true,
                "minecraft:redstone" to false
            ),
            Boolean::class.java,
            {id,_ -> val idChk = Identifier.tryParse(id); if(idChk == null){false} else {Registries.ITEM.containsId(idChk)}  },
            "Map key needs to be a valid item identifier; map entry needs to be a boolean ('true' or 'false')"
        )

        var testColor = ValidatedColor(0,0,128, headerText = Header.Builder().add("testing.translation").build())

    }

    var test: Test = SyncedConfigHelperV1.readOrCreateAndValidate("new_test_v0.json") { Test() }

    enum class Testy{
        OPTION_A,
        OPTION_B,
        OPTION_C,
        BASIC
    }
}