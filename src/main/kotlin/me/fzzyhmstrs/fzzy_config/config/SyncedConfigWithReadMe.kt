package me.fzzyhmstrs.fzzy_config.config

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.validated_field.*
import me.fzzyhmstrs.fzzy_config.interfaces.SyncedConfig
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import me.fzzyhmstrs.fzzy_config.validated_field.list.ValidatedList
import me.fzzyhmstrs.fzzy_config.validated_field.map.ValidatedStringKeyMap
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * Helper class for the creation of synchronized configurations
 *
 * It is *strongly* recommended you utilize this class for building a configuration, as it will handle a majority of the busy work that would otherwise require special attention
 *
 * Pair this with [ConfigClass] and [ConfigSection] to build a well organized and more easily updated configuration system. A typical approach would look something like:
 * 1. An Object inheriting [SyncedConfigWithReadMe]
 * 2. A series of [ConfigClass] instances used to store various [ValidatedField](me.fzzyhmstrs.fzzy_config.validated_field.ValidatedField)
 * 3. Where needed, [ConfigSection] instances used to mark sub-sections of larger section. A section regarding entity cosmetics in a larger class covering entity settings, for example.
 *
 * The embedded/below Sample Config can be used as a reference for possible implementation strategies
 *
 * @param configName String. The unique name for this config. Using identifier notation (namespace:path) may help ensure uniqueness
 * @param file String. The file name for the ReadMe. Need suffix, so "README.txt" would be a typical choice.
 * @param base String, optional. The subfolder for the readme. Ideally should match the base used for the config classes themselves.
 * @param headerText Header, optional. If left out, a default [Header](me.fzzyhmstrs.fzzy_config.config_util.ReadMeBuilder.Header) with a pre-defined translation key in the format "fc.config.configName" will be provided.
 * @param decorator LineDecorating, optional. If left out, [LineDecorator.DEFAULT](me.fzzyhmstrs.fzzy_config.config_util.ReadMeBuilder.LineDecorator.DEFAULT) will be used to decorate readme lines.
 *
 * @see ReadMeBuilder
 * @see SyncedConfig
 *
 * @sample FcSampleConfig
 */
abstract class SyncedConfigWithReadMe(
    private val configName: String,
    file: String,
    base: String = FC.MOD_ID,
    headerText: Header = Header.Builder().add("fc.config.$configName").space().build(),
    decorator: LineDecorating = LineDecorator.DEFAULT)
    :
    ReadMeBuilder(file,base,headerText, decorator),
    SyncedConfig
{
    /**
     * Call this method in a [ModInitializer](net.fabricmc.api.ModInitializer) to both initialize config settings stored within and to ensure the config is registered with the [SyncedConfigRegistry] in time for the JOIN event.
     */
    override fun initConfig() {
        SyncedConfigRegistry.registerConfig(configName,this)
    }

    //A sample configuration system that utilizes a broad range of features available in Fzzy Config
    private object FcSampleConfig:
        SyncedConfigWithReadMe(
            "fc_test_new_config",
            "test_README.txt",
            FC.MOD_ID,
            headerText = Header.Builder()
                .add("testing.translation2")
                .literal()
                .add("I'm trying to do lots of cool automagical stuff")
                .space()
                .add("So cool")
                .space().build()
        ) {

        //vals will be ignored by auto-serialization and ReadMe building, so can be used to store undonfigurable information.
        private val testConfigHeader = Header.Builder().space().box("TEST CONFIG").space().add("This is a config about testing the new fzz core config system").build()

        //a sample ConfigClass. Note the two line implementation of sections to enable proper auto-serialization
        class Test: ConfigClass(testConfigHeader){
            //all configurable entries stored as var. A subsection of the larger config using a ConfigSection
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
                var test_Id = ValidatedIdentifier(Identifier("redstone"), Registries.ITEM.ids, "ID needs to be in the item registry.")
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
                {id,_ -> val idChk = Identifier.tryParse(id); if(idChk == null){false} else {
                    Registries.ITEM.containsId(idChk)}  },
                {_,bl -> bl},
                "Map key needs to be a valid item identifier; map entry needs to be a boolean ('true' or 'false')"
            )

            var testColor = ValidatedColor(0,0,128, headerText = Header.Builder().add("testing.translation").build())

        }

        //Where the actual test class instance is generated. Note, still a var
        var test: Test = SyncedConfigHelperV1.readOrCreateAndValidate("new_test_v0.json") { Test() }

        //Enums can be validated! Don't mess around with weird string checking or combinations of boolean values.
        enum class Testy{
            OPTION_A,
            OPTION_B,
            OPTION_C,
            BASIC
        }
    }
}