package me.fzzyhmstrs.fzzy_config_test
import com.mojang.brigadier.CommandDispatcher
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

    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register{ dispatcher, _ ->
            registerClientCommands(dispatcher)
        }
        ClientTickEvents.START_CLIENT_TICK.register{client ->
            if (openDamnScreen != "") {
                client.setScreen(TestPopupScreen())
                openDamnScreen = ""
            }
        }
    }

    private fun registerClientCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>){
        dispatcher.register(
            ClientCommandManager.literal("test_screen_1")
                .executes{ context ->
                    openDamnScreen = "please"
                    1
                }
        )
    }
}

fun String.fctId(): Identifier {
    return Identifier(FC.MOD_ID,this)
}