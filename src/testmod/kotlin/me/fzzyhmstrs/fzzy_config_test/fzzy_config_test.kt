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
import com.mojang.brigadier.CommandDispatcher
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.updates.BaseUpdateManager
import me.fzzyhmstrs.fzzy_config.util.Expression
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber
import me.fzzyhmstrs.fzzy_config_test.test.TestConfig
import me.fzzyhmstrs.fzzy_config_test.test.TestConfigClient
import me.fzzyhmstrs.fzzy_config_test.test.TestPopupScreen
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.util.Identifier
import net.fabricmc.fabric.api.util.TriState
import net.minecraft.util.math.MathHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.random.Random

object FC: ModInitializer {
    const val MOD_ID = "fzzy_config_test"
    val LOGGER: Logger = LoggerFactory.getLogger("fzzy_config_test")
    val fcRandom = Random(System.currentTimeMillis())
    const val TEST_PERMISSION_GOOD = "test.perm.good"
    const val TEST_PERMISSION_BAD = "test.perm.bad"

    override fun onInitialize() {
        PermissionCheckEvent.EVENT.register { _, permission ->
            if (permission == TEST_PERMISSION_GOOD)
                TriState.TRUE
            else if (permission == TEST_PERMISSION_BAD)
                TriState.FALSE
            else
                TriState.DEFAULT
        }

        TestConfig.init()
        val expressionTestResults = AssertionResults()
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
        assertConstExpression("log(4,4)", kotlin.math.log(4.0,4.0), expressionTestResults)
        assertConstExpression("log10(5)", kotlin.math.log10(5.0), expressionTestResults)
        assertConstExpression("log2(5)", kotlin.math.log2(5.0), expressionTestResults)
        assertConstExpression("abs(-4.5)", 4.5,expressionTestResults)
        assertConstExpression("sin(4.5)", MathHelper.sin(4.5.toFloat()).toDouble(), expressionTestResults)
        assertConstExpression("cos(4.5)", MathHelper.cos(4.5.toFloat()).toDouble(), expressionTestResults)
        assertConstExpression("incr(4.268,0.1)", 4.2,expressionTestResults)
        println(expressionTestResults)
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

    //val testEnum = ValidatedEnum(Selectable.SelectionType.FOCUSED)
    //val testEnum2 = ValidatedEnum(Selectable.SelectionType.FOCUSED,ValidatedEnum.WidgetType.CYCLING)
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
                client.setScreen(TestPopupScreen())
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
    return Identifier(FC.MOD_ID, this)
}