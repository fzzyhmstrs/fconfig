/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config_test
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import kotlinx.io.bytestring.encodeToByteString
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.FileType
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.updates.BaseUpdateManager
import me.fzzyhmstrs.fzzy_config.util.Expression
import me.fzzyhmstrs.fzzy_config.util.TomlOps
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber
import me.fzzyhmstrs.fzzy_config_test.loot.ConfigLootCondition
import me.fzzyhmstrs.fzzy_config_test.loot.ConfigLootNumberProvider
import me.fzzyhmstrs.fzzy_config_test.test.TestConfig
import me.fzzyhmstrs.fzzy_config_test.test.TestConfig.gson
import me.fzzyhmstrs.fzzy_config_test.test.TestConfigClient
import me.fzzyhmstrs.fzzy_config_test.test.TestConfigImplAny
import me.fzzyhmstrs.fzzy_config_test.test.TestLateConfigImpl
import me.fzzyhmstrs.fzzy_config_test.test.screen.TestPopupScreen
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.util.Identifier
import net.fabricmc.fabric.api.util.TriState
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtEnd
import net.minecraft.nbt.NbtOps
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.server.command.CommandManager
import net.peanuuutz.tomlkt.TomlElement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object FC: ModInitializer {
    const val MOD_ID = "fzzy_config_test"
    val LOGGER: Logger = LoggerFactory.getLogger("fzzy_config_test")
    const val TEST_PERMISSION_GOOD = "test.perm.good"
    const val TEST_PERMISSION_BAD = "test.perm.bad"

    private val TEST_REGISTRAR = ConfigApi.platform().createRegistrar(MOD_ID, Registries.STATUS_EFFECT)

    init {
        TEST_REGISTRAR.init()
    }

    @Translatable.Name("Test Status 1")
    val TEST_STATUS_1 = TEST_REGISTRAR.register("test_1") { object: StatusEffect(StatusEffectCategory.NEUTRAL, 0xFFFFFF){} }
    @Translatable.Name("Test Status 2")
    val TEST_STATUS_2 = TEST_REGISTRAR.register("test_2") { object: StatusEffect(StatusEffectCategory.NEUTRAL, 0xFFFFFF){} }
    @Translatable.Name("Test Direct Status")
    val TEST_DIRECT_STATUS_ID = Identifier.of("fzzy_config_test", "direct")
    val TEST_DIRECT_STATUS = Registry.register(Registries.STATUS_EFFECT, TEST_DIRECT_STATUS_ID, object: StatusEffect(StatusEffectCategory.NEUTRAL, 0xFFFFFF){})

    internal fun encodeNbt(toml: TomlElement): ValidationResult<NbtElement> {
        return try {
            val jsonElement = TomlOps.INSTANCE.convertTo(NbtOps.INSTANCE, toml)
            ValidationResult.success(jsonElement)
        } catch (e: Throwable) {
            ValidationResult.error(NbtEnd.INSTANCE, ValidationResult.ErrorEntry.Type("NBT Encoding Problem"), "Exception encountered while encoding NBT", e)
        }
    }

    override fun onInitialize() {

        buildRegTranslation("en_us", "effect")

        PermissionCheckEvent.EVENT.register { _, permission ->
            if (permission == TEST_PERMISSION_GOOD)
                TriState.TRUE
            else if (permission == TEST_PERMISSION_BAD)
                TriState.FALSE
            else
                TriState.DEFAULT
        }

        TestConfig.init()
        ConfigLootCondition.init()
        ConfigLootNumberProvider.init()

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                CommandManager.literal("check_server_config")
                    .executes { _ ->
                        FC.LOGGER.info(TestConfig.serverConfig.toString())
                        1
                    }
            )
        }

        val result = ConfigApi.serializeToToml(TestConfig.testConfig2)
        val test1 = FileType.TOML.encode(result.get())
        val test2 = encodeNbt(result.get())

        LOGGER.info("Test Config 2 Written to:")
        LOGGER.info("  Toml String: ${test1.get().encodeToByteArray().size} bytes")
        LOGGER.info("  NBT Element: ${test2.get().sizeInBytes} bytes")

        /*val expressionTestResults = AssertionResults()
        assertConstExpression("3 + 5", 8.0, expressionTestResults)
        assertConstExpression("3 - 5", -2.0, expressionTestResults)
        assertConstExpression("3 * 5", 15.0, expressionTestResults)
        assertConstExpression("3 / 5", 3.0/5.0, expressionTestResults)
        assertConstExpression("8 % 2", 0.0, expressionTestResults)
        assertConstExpression("3 + 5 + 3 + 5 + 3 + 5", 24.0, expressionTestResults)
        assertVarExpression("3 + x", mapOf('x' to 5.0), 8.0, expressionTestResults)
        assertVarExpression("3 - x", mapOf('x' to 5.0), -2.0, expressionTestResults)
        assertVarExpression("3 * x", mapOf('x' to 5.0), 15.0, expressionTestResults)
        assertVarExpression("3 / x", mapOf('x' to 5.0), 3.0/5.0, expressionTestResults)
        assertVarExpression("8 % x", mapOf('x' to 3.0), 8.0 % 3.0, expressionTestResults)
        assertConstExpression("sqrt(4)", 2.0, expressionTestResults)
        assertConstExpression("ceil(4.5)", 5.0, expressionTestResults)
        assertConstExpression("floor(4.5)", 4.0, expressionTestResults)
        assertConstExpression("round(4.25)", 4.0, expressionTestResults)
        assertConstExpression("ln(4.5)", kotlin.math.ln(4.5), expressionTestResults)
        assertConstExpression("log(4, 4)", kotlin.math.log(4.0, 4.0), expressionTestResults)
        assertConstExpression("log10(5)", kotlin.math.log10(5.0), expressionTestResults)
        assertConstExpression("log2(5)", kotlin.math.log2(5.0), expressionTestResults)
        assertConstExpression("abs(-4.5)", 4.5, expressionTestResults)
        assertConstExpression("sin(4.5)", MathHelper.sin(4.5.toFloat()).toDouble(), expressionTestResults)
        assertConstExpression("cos(4.5)", MathHelper.cos(4.5.toFloat()).toDouble(), expressionTestResults)
        assertConstExpression("incr(4.268, 0.1)", 4.2, expressionTestResults)
        assertConstExpression("max(4.55, 0.1)", 4.55, expressionTestResults)
        assertConstExpression("min(4.55, 0.1)", 0.1, expressionTestResults)
        println(expressionTestResults)*/
    }

    internal fun buildTranslation(lang: String) {
        val testObj = JsonObject()

        fun add(key: String, value: String) {
            testObj.add(key, JsonPrimitive(value))
        }

        ConfigApi.buildTranslations(TestConfigImplAny::class, Identifier.of("fzzy_config_test","test_config_any"), lang, true, ::add)

        LOGGER.info("Test translation for $lang")
        LOGGER.info(gson.toJson(testObj))
    }

    internal fun buildRegTranslation(lang: String, prefix: String) {
        val testObj = JsonObject()

        fun add(key: String, value: String) {
            testObj.add(key, JsonPrimitive(value))
        }

        ConfigApi.platform().buildRegistryTranslations(this, prefix, lang, true, ::add)

        LOGGER.info("Test $prefix registry translation for $lang")
        LOGGER.info(gson.toJson(testObj))
    }

    private fun assertConstExpression(exp: String, result: Double, results: AssertionResults) {
        println("Expression Input: $exp")
        val e = Expression.parse(exp)
        println("Expression Parsed: $e")
        this.assert(e.eval(mapOf()), result, results)
    }

    private fun assertVarExpression(exp: String, vars: Map<Char, Double>, result: Double, results: AssertionResults) {
        println("Expression Input: $exp")
        val e = Expression.parse(exp)
        println("Expression Parsed: $e")
        this.assert(e.eval(vars), result, results)
    }

    private fun assert(testVal: Any, assertionVal: Any, assertions: AssertionResults) {
        println("Assertion [$testVal] == [$assertionVal]: ${(testVal == assertionVal).also { assertions.inc(it) }}")
    }

    private class AssertionResults(var fails: Int = 0, var tests: Int = 0) {
        override fun toString(): String {
            return "ASSERTIONS PASSED: ${tests - fails} out of $tests"
        }
        fun inc(result: Boolean) {
            tests++
            if(!result)
                fails++
        }
        fun addTo(other: AssertionResults) {
            other.tests += tests
            other.fails += fails
        }
    }
}

object FCC: ClientModInitializer {

    var openDamnScreen = ""
    var entries = 5

    //val testEnum = ValidatedEnum(Selectable.SelectionType.FOCUSED)
    //val testEnum2 = ValidatedEnum(Selectable.SelectionType.FOCUSED, ValidatedEnum.WidgetType.CYCLING)
    val testInt = ValidatedInt(8, 16, 0)
    val testInt2 = ValidatedInt(8, Int.MAX_VALUE, 0, ValidatedNumber.WidgetType.TEXTBOX)
    val testString = ValidatedString.Builder("chickenfrog")
        .both { s, _ -> ValidationResult.predicated(s, s.contains("chicken"), "String must contain the lowercase word 'chicken'.") }
        .withCorrector()
        .both { s, _ ->
            if(s.contains("chicken")) {
                ValidationResult.success(s)
            } else {
                if(s.contains("chicken", true)) {
                    val s2 = s.replace(Regex("(?i)chicken"), "chicken")
                    ValidationResult.error(s2, "'chicken' needs to be lowercase in the string")
                } else {
                    ValidationResult.error(s, "String must contain the lowercase word 'chicken'")
                }
            }
        }
        .build()
    val testBoolean = ValidatedBoolean()

    val manager = BaseUpdateManager()

    override fun onInitializeClient() {
        //testEnum.setEntryKey("fc.test.enum.name")
        //testEnum.setUpdateManager(manager)
        //testEnum2.setEntryKey("fc.test.enum2.name")
        //testEnum2.setUpdateManager(manager)
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
                client.setScreen(TestPopupScreen(entries))
                openDamnScreen = ""
            } else if (openDamnScreen == "the_big_one") {
                ConfigApi.openScreen("fzzy_config_test")
                openDamnScreen = ""
            }
        }
        TestConfigClient.init()
    }

    private fun registerClientCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommandManager.literal("test_screen_1")
                .executes{ context ->
                    openDamnScreen = "please"
                    1
                }
                .then(
                    ClientCommandManager.argument("count", IntegerArgumentType.integer(1))
                        .executes { context ->
                            val entries = IntegerArgumentType.getInteger(context, "count")
                            this.openDamnScreen = "please"
                            this.entries = entries
                            1
                        }

                )

        )
        dispatcher.register(
            ClientCommandManager.literal("load_late_config")
                .executes { context ->
                    ConfigApi.registerAndLoadConfig({ TestLateConfigImpl() }, RegisterType.CLIENT)
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
    return Identifier.of(FC.MOD_ID, this)
}