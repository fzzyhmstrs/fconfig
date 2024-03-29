
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
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
    }
}

fun String.fctId(): Identifier {
    return Identifier(FC.MOD_ID,this)
}