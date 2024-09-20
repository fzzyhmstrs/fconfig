/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.config

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.entry.EntryDeserializer
import me.fzzyhmstrs.fzzy_config.entry.EntryKeyed
import me.fzzyhmstrs.fzzy_config.entry.EntrySerializer
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.contextualize
import me.fzzyhmstrs.fzzy_config.util.Walkable
import net.peanuuutz.tomlkt.TomlElement
import org.jetbrains.annotations.ApiStatus.Internal
import java.lang.Runnable
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Builds a button that will appear in a Config GUI, to perform some arbitrary, possible non-config-related action
 * 
 * This could be used to link to a wiki, open another non-fzzy-config config, open a non-config screen, open a patchouli guide, run a command, and so on.
 * @param titleSupplier Supplier&lt;Text&gt; - supplies a name for the button widget. Will be checked every frame, so the button name will dynamically update as needed.
 * @param activeSupplier Supplier&lt;Boolean&gt; - supplies an active state; whether the button is insactive ("greyed out" and unclickable) or active (functioning normally)
 * @param pressAction [Runnable] - the action to execute on clicking the button
 * @param background [Identifier], nullable - if non-null, will provide a custom background for the widget rendering.
 * @param decoration [Identifier[, nullable - if non-null, will render a "decoration" next to the widget. These are the typically white/wireframe icons shown next to certain settings like lists.
 * @author fzzyhmstrs
 * @since 0.4.4
 */
class ConfigAction @JvmOverloads constructor(
    private val titleSupplier: Supplier<Text>, 
    private val activeSupplier: Supplier<Boolean>, 
    private val pressAction: Runnable, 
    private val background: Identifier? = null,
    private val decoration: Identifier? = null)
:
    EntryWidget<Any>
{
    @Internal
    override fun widgetEntry(choicePredicate: ChoiceValidator<Any>): ClickableWidget {
        return if (decoration == null) 
            ActiveButtonWidget(titleSupplier, 110, 20, activeSupplier, Consumer { _ -> pressAction.run() }, background)
        else
            DecoratedActiveButtonWidget(titleSupplier, 110, 20, decoration, activeSupplier, Consumer { _ -> pressAction.run() }, background)
    }

    /**
     * Builds a [ConfigAction]
     * @author fzzyhmstrs
     * @since 0.4.4
     */
    class Builder {
        private var titleSupplier: Supplier<Text> = Supplier { FcText.empty() }
        private var activeSupplier: Supplier<Boolean> = Supplier { true }
        private var pressAction: Runnable = Runnable { }
        private var background: Identifier? = null
        private var decoration: Identifier? = null

        /**
         * Sets the title of the widget. This will be a static title, unchanging based on state.
         * @param title [Text] the button widget title
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.4.4
         */
        fun title(title: Text): Builder {
            this.titleSupplier = Supplier { title }
            return this
        }

        /**
         * Sets the title of the widget. This can be dynamically provided as needed, the title is polled from the supplier every frame.
         * @param titleSupplier Supplier&lt;[Text]&gt; the button widget title supplier
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.4.4
         */
        fun title(titleSupplier: Supplier<Text>): Builder {
            this.titleSupplier = titleSupplier
            return this
        }

        /**
         * Determines whether the button should be active or not. Polled every frame.
         * @param activeSupplier Supplier&lt;Boolean&gt; provides the buttons current active state.
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.4.4
         */
        fun active(activeSupplier: Supplier<Boolean>): Builder {
            this.activeSupplier = activeSupplier
            return this
        }

        /**
         * Sets a custom background for the button widget, which will appear when the button is selected and active
         * @param id [Identifier] the background sprite id
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.4.4
         */
        fun background(id: Identifier): Builder {
            this.background = id
        }

        /**
         * Defines a decoration texture id. This will be drawn to the left of the button widget in the config screen. Decorations are typically 20x20 at the most
         * @param id [Identifier] decoration sprite id
         * @see me.fzzyhmstrs.screen.widget.TextureIds
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.4.4
         */
        fun decoration(id: Identifier): Builder {
            this.decoration = id
        }

        /**
         * Builds the [ConfigAction] with the provided runnable as the on-click event
         * @param action [Runnable] - event to run when the button is clicked
         * @return [ConfigAction]
         * @author fzzyhmstrs
         * @since 0.4.4
         */
        fun build(action: Runnable): ConfigAction {
            return ClickAction(titleSupplier, activeSupplier, action, background, decoration)
        }

        /**
         * Builds the [ConfigAction] with the supplied ClickEvent as the on-click event
         * @param event [ClickEvent] - event to run when the button is clicked
         * @return [ConfigAction]
         * @author fzzyhmstrs
         * @since 0.4.4
         */
        fun build(event: ClickEvent): ConfigAction{
            TODO()
        }
    }
}
