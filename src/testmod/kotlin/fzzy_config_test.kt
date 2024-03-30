
import com.mojang.brigadier.CommandDispatcher
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.random.Random

import java.util.*

object FC: ModInitializer {
    const val MOD_ID = "fzzy_config_test"
    val LOGGER: Logger = LoggerFactory.getLogger("fzzy_config_test")
    val fcRandom = Random(System.currentTimeMillis())

    override fun onInitialize() {
    }
}

object FCC: ClientModInitializer {

    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register{ dispatcher, _ ->
            registerClientCommands(dispatcher)
        }
    }

    private fun registerClientCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>){

    }
}

fun String.fctId(): Identifier {
    return Identifier(FC.MOD_ID,this)
}