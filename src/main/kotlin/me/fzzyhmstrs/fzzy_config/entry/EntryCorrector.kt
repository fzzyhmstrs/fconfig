/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.entry

import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import java.util.function.Predicate

/**
 * Deserializes individual entries in a complex [Entry]
 *
 * SAM: [correctEntry] takes a TomlElement, returns a deserialized instance of T
 * @param T the type of the Entry stored value
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@FunctionalInterface
fun interface EntryCorrector<T> {

    /**
     * Corrects an attempted input, as possible. When correction isn't possible or deterministic, simply validate the entry the same way you would for [EntryValidator]
     * @param input [T] the attempted input
     * @param type [EntryValidator.ValidationType] Whether this correction should weakly try to correct, or strongly. Weak correction is used during deserialization, as things like Registries may not be ready yet. Strong correction is used in-game.
     * @return [ValidationResult] with correct
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun correctEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T>

    /**
     * Builder subclass for this corrector. Builds a corrector from predicates for weak and strong correction circumstances.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    class Builder<T:Any>: AbstractBuilder<T, Builder<T>>() {
        override fun builder(): Builder<T> {
            return this
        }
    }

    abstract class AbstractBuilder<T: Any, E: AbstractBuilder<T, E>> {

        private var ifStrong: EntryCorrector<T> = EntryCorrector{ i, _ -> ValidationResult.success(i) }
        private var ifWeak: EntryCorrector<T> = EntryCorrector{ i, _ -> ValidationResult.success(i) }

        protected abstract fun builder(): E

        /**
         * Applies an [EntryCorrector] for correcting strong inputs
         * @param corrector [EntryCorrector]
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun strong(corrector: EntryCorrector<T>): E {
            ifStrong = corrector
            return builder()
        }

        /**
         * Builds a validator for strong inputs. This can't perform correction, so should be used where correction isn't feasible.
         * @param predicate [Predicate]&lt;[T]&gt; tests
         * @param errorMsg message presented by the [ValidationResult] if validation fails
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun strong(predicate: Predicate<T>, errorMsg: String = "Problem validating Entry!"): E {
            ifStrong = EntryCorrector { i, _ ->
                if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, ValidationResult.Errors.BASIC, errorMsg)
            }
            return builder()
        }

        /**
         * Applies an [EntryCorrector] for correcting weak inputs
         * @param corrector [EntryCorrector]
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun weak(corrector: EntryCorrector<T>): E {
            ifWeak = corrector
            return builder()
        }

        /**
         * Builds a validator for weak inputs. This can't perform correction, so should be used where correction isn't feasible.
         * @param predicate [Predicate]&lt;[T]&gt; tests
         * @param errorMsg message presented by the [ValidationResult] if validation fails
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun weak(predicate: Predicate<T>, errorMsg: String = "Problem validating Entry!"): E {
            ifWeak = EntryCorrector { i, _ ->
                if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, ValidationResult.Errors.BASIC, errorMsg)
            }
            return builder()
        }

        /**
         * Applies a [EntryCorrector] to both weak and strong inputs
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun both(corrector: EntryCorrector<T>): E {
            ifStrong = corrector
            ifWeak = corrector
            return builder()
        }

        /**
         * Builds a validator for weak and strong inputs. This can't perform correction, so should be used where correction isn't feasible.
         * @param predicate [Predicate]&lt;[T]&gt; tests
         * @param errorMsg message presented by the [ValidationResult] if validation fails
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        fun both(predicate: Predicate<T>, errorMsg: String = "Problem validating Entry!"): E {
            ifStrong = EntryCorrector { i, _ ->
                if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, ValidationResult.Errors.BASIC, errorMsg)
            }
            ifWeak = EntryCorrector { i, _ ->
                if (predicate.test(i)) ValidationResult.success(i) else ValidationResult.error(i, ValidationResult.Errors.BASIC, errorMsg)
            }
            return builder()
        }

        /**
         * Builds the [EntryCorrector] instance for use
         * @return [EntryCorrector]
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun buildCorrector(): EntryCorrector<T> {
            return EntryCorrector{ i, t -> if(t == EntryValidator.ValidationType.WEAK) ifWeak.correctEntry(i, t) else ifStrong.correctEntry(i, t) }
        }
    }
}