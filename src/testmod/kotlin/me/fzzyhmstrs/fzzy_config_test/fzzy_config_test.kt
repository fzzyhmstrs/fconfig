package me.fzzyhmstrs.fzzy_config_test
import com.mojang.brigadier.CommandDispatcher
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.updates.BaseUpdateManager
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber
import me.fzzyhmstrs.fzzy_config_test.test.TestConfig
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import me.fzzyhmstrs.fzzy_config_test.test.TestPopupScreen
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.gui.Selectable
import kotlin.random.Random

object FC: ModInitializer {
    const val MOD_ID = "fzzy_config_test"
    val LOGGER: Logger = LoggerFactory.getLogger("fzzy_config_test")
    val fcRandom = Random(System.currentTimeMillis())

    override fun onInitialize() {
    }
}

object FCC: ClientModInitializer {

    var openDamnScreen = ""

    val testEnum = ValidatedEnum(Selectable.SelectionType.FOCUSED)
    val testEnum2 = ValidatedEnum(Selectable.SelectionType.FOCUSED,ValidatedEnum.WidgetType.CYCLING)
    val testInt = ValidatedInt(8,16,0)
    val testInt2 = ValidatedInt(8,Int.MAX_VALUE,0,ValidatedNumber.WidgetType.TEXTBOX)
    val testString = ValidatedString.Builder("chickenfrog")
        .both { s,_ -> ValidationResult.predicated(s, s.contains("chicken"), "String must contain the lowercase word 'chicken'.") }
        .withCorrector()
        .both { s,_ ->
            if(s.contains("chicken")){
                ValidationResult.success(s)
            } else {
                if(s.contains("chicken", true)){
                    val s2 = s.replace(Regex("(?i)chicken"),"chicken")
                    ValidationResult.error(s2,"'chicken' needs to be lowercase in the string")
                } else {
                    ValidationResult.error(s,"String must contain the lowercase word 'chicken'")
                }
            }
        }
        .build()
    val testBoolean = ValidatedBoolean()

    val manager = BaseUpdateManager()

    override fun onInitializeClient() {
        testEnum.setEntryKey("fc.test.enum.name")
        testEnum.setUpdateManager(manager)
        testEnum2.setEntryKey("fc.test.enum2.name")
        testEnum2.setUpdateManager(manager)
        testInt.setEntryKey("fc.test.int.name")
        testInt.setUpdateManager(manager)
        testInt2.setEntryKey("fc.test.int2.name")
        testInt2.setUpdateManager(manager)
        testString.setEntryKey("fc.test.string.name")
        testString.setUpdateManager(manager)
        testBoolean.setEntryKey("fc.test.boolean.name")
        testBoolean.setUpdateManager(manager)
        ClientCommandRegistrationCallback.EVENT.register{ dispatcher, _ ->
            registerClientCommands(dispatcher)
        }
        ClientTickEvents.START_CLIENT_TICK.register{client ->
            if (openDamnScreen == "please") {
                client.setScreen(TestPopupScreen())
                openDamnScreen = ""
            } else if (openDamnScreen == "the_big_one"){
                ConfigApi.openScreen("fzzy_config")
                openDamnScreen = ""
            }
        }
        TestConfig.init()
        println(TestConfig.toString())
    }

    private fun registerClientCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>){
        dispatcher.register(
            ClientCommandManager.literal("test_screen_1")
                .executes{ context ->
                    openDamnScreen = "please"
                    1
                }

        )
        dispatcher.register(
            ClientCommandManager.literal("test_screen_2")
                .executes{ context ->
                    openDamnScreen = "the_big_one"
                    1
                }

        )
    }
}

fun String.fctId(): Identifier {
    return Identifier(FC.MOD_ID,this)
}