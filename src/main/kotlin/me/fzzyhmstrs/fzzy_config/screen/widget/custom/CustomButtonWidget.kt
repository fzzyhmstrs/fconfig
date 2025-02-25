/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.widget.custom

import me.fzzyhmstrs.fzzy_config.screen.widget.TextureProvider
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureSet
import me.fzzyhmstrs.fzzy_config.screen.widget.TooltipChild
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget.ActiveNarrationSupplier
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.isNotEmpty
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

/**
 * A custom [ButtonWidget] implementation with builder and rendering improvements (and more features in general)
 *
 * This constructor is for subclasses of this widget only. Buttons should be constructed with the builder, like vanilla buttons.
 * @param x button X position
 * @param y button Y position
 * @param width button width in pixels
 * @param height button height in pixels
 * @param pressAction [Consumer]&lt;[CustomButtonWidget]&gt; action to invoke when the button is clicked or activated
 * @param narrationSupplier [ActiveNarrationSupplier] converts a provided input text and the current active state of this widget into a narration text.
 * @param narrationAppender [Consumer]&lt;[NarrationMessageBuilder]&gt; unlike the supplier, this is used to directly append additional narrations as needed to the message builder.
 * @param textures [TextureSet], default [CustomPressableWidget.DEFAULT_TEXTURES]. The textures for this button
 * @param child [TooltipChild], used to pass additional tooltip context. This button will pass and tooltip from this child out to its own parent (this button is also a [TooltipChild])
 * @param renderMessage If false, the label won't be rendered
 * @author fzzyhmstrs
 * @since 0.5.?, update to ActiveNarrationSupplier 0.6.3, improve memory footprint 0.6.5
 */
open class CustomButtonWidget protected constructor(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Text,
    private val pressAction: Consumer<CustomButtonWidget>,
    private val narrationSupplier: ActiveNarrationSupplier? = null,
    private val narrationAppender: Consumer<NarrationMessageBuilder>? = null,
    private val child: TooltipChild? = null,
    override val textures: TextureProvider = DEFAULT_TEXTURES,
    private val renderMessage: Boolean = true)
    :
    CustomPressableWidget(x, y, width, height, message)
{

    /**
     * A custom [ButtonWidget] implementation with builder and rendering improvements (and more features in general)
     *
     * This constructor is for compatibility purposes with subclasses. Use the builder to create buttons.
     * @param x button X position
     * @param y button Y position
     * @param width button width in pixels
     * @param height button height in pixels
     * @param pressAction [Consumer]&lt;[CustomButtonWidget]&gt; action to invoke when the button is clicked or activated
     * @param narrationSupplier [ButtonWidget.NarrationSupplier] same use as in vanilla; converts a provided input text into a narration text.
     * @param narrationAppender [Consumer]&lt;[NarrationMessageBuilder]&gt; unlike the supplier, this is used to directly append additional narrations as needed to the message builder.
     * @param child [TooltipChild], used to pass additional tooltip context. This button will pass and tooltip from this child out to its own parent (this button is also a [TooltipChild])
     * @param textures [TextureSet], default [CustomPressableWidget.DEFAULT_TEXTURES]. The textures for this button
     * @param renderMessage If false, the label won't be rendered
     * @author fzzyhmstrs
     * @since 0.6.4, scheduled for removal 0.7.0
     */
    @Deprecated("Custom button widgets now use TextureProvider. Use the primary constructor. Scheduled for removal 0.7.0")
    constructor(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        message: Text,
        pressAction: Consumer<CustomButtonWidget>,
        narrationSupplier: ActiveNarrationSupplier? = null,
        narrationAppender: Consumer<NarrationMessageBuilder>? = null,
        textures: TextureSet = DEFAULT_TEXTURES,
        child: TooltipChild? = null,
        renderMessage: Boolean = true): this(x, y, width, height, message, pressAction, narrationSupplier, narrationAppender, child, textures, renderMessage)

    /**
     * A custom [ButtonWidget] implementation with builder and rendering improvements (and more features in general)
     *
     * This constructor is for compatibility purposes with subclasses. Use the builder to create buttons.
     * @param x button X position
     * @param y button Y position
     * @param width button width in pixels
     * @param height button height in pixels
     * @param pressAction [Consumer]&lt;[CustomButtonWidget]&gt; action to invoke when the button is clicked or activated
     * @param narrationSupplier [ButtonWidget.NarrationSupplier] same use as in vanilla; converts a provided input text into a narration text.
     * @param narrationAppender [Consumer]&lt;[NarrationMessageBuilder]&gt; unlike the supplier, this is used to directly append additional narrations as needed to the message builder.
     * @param textures [TextureSet], default [CustomPressableWidget.DEFAULT_TEXTURES]. The textures for this button
     * @param child [TooltipChild], used to pass additional tooltip context. This button will pass and tooltip from this child out to its own parent (this button is also a [TooltipChild])
     * @param renderMessage If false, the label won't be rendered
     * @author fzzyhmstrs
     * @since 0.6.3, scheduled for removal 0.7.0
     */
    @Deprecated("Custom button widgets now use ActiveNarrationSupplier. Use the primary constructor. Scheduled for removal 0.7.0")
    protected constructor(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        message: Text,
        pressAction: Consumer<CustomButtonWidget>,
        narrationSupplier: ButtonWidget.NarrationSupplier = DEFAULT_NARRATION_SUPPLIER,
        narrationAppender: Consumer<NarrationMessageBuilder> = Consumer { _-> },
        textures: TextureSet = DEFAULT_TEXTURES,
        child: TooltipChild? = null,
        renderMessage: Boolean = true): this(x, y, width, height, message, pressAction, ActiveNarrationSupplier { _, supplier -> narrationSupplier.createNarrationMessage(supplier) }, narrationAppender, textures, child, renderMessage)

    protected var activeSupplier: Supplier<Boolean> = DEFAULT_ACTIVE_SUPPLIER
    protected var messageSupplier: Supplier<Text>? = null
    protected var tooltipSupplier: Function<Boolean, Text>? = null

    override fun onPress() {
        pressAction.accept(this)
    }

    override fun renderCustom(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
        if (renderMessage)
            super.renderCustom(context, x, y, width, height, mouseX, mouseY, delta)
    }

    override fun renderBackground(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
        this.active = activeSupplier.get()
        tooltipSupplier?.apply(active)?.let { if(it.isNotEmpty()) this.tooltip = Tooltip.of(it) else this.tooltip = null }
        super.renderBackground(context, x, y, width, height, mouseX, mouseY, delta)
    }

    override fun getMessage(): Text {
        return messageSupplier?.get() ?: super.getMessage()
    }

    override fun getNarrationMessage(): MutableText {
        return (narrationSupplier ?: DEFAULT_ACTIVE_NARRATION_SUPPLIER).createNarrationMessage(this.active) { super.getNarrationMessage() }
    }

    override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Text> {
        return child?.provideTooltipLines(mouseX, mouseY, parentSelected, keyboardFocused) ?: super.provideTooltipLines(mouseX, mouseY, parentSelected, keyboardFocused)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
        super.appendClickableNarrations(builder)
        if (builder != null)
            narrationAppender?.accept(builder)
    }

    @FunctionalInterface
    fun interface ActiveNarrationSupplier {
        fun createNarrationMessage(active: Boolean, textSupplier: Supplier<MutableText>): MutableText
    }

    /**
     * Builds a [CustomButtonWidget]
     * @param message [Text] the button label
     * @param onPress [Consumer]&lt;[CustomButtonWidget]&gt; action to invoke when the button is clicked or activated
     * @author fzzyhmstrs
     * @since 0.5.?
     */
    class Builder(private val message: Text, private val onPress: Consumer<CustomButtonWidget>) {

        constructor(onPress: Consumer<CustomButtonWidget>): this(FcText.EMPTY, onPress)

        private var tooltip: Tooltip? = null
        private var tooltipSupplier: Function<Boolean, Text>? = null
        private var x = 0
        private var y = 0
        private var w = 150
        private var h = 20
        private var narrationSupplier: ActiveNarrationSupplier = DEFAULT_ACTIVE_NARRATION_SUPPLIER
        private var narrationAppender: Consumer<NarrationMessageBuilder> = Consumer { _-> }
        private var activeSupplier: Supplier<Boolean> = Supplier { true }
        private var messageSupplier: Supplier<Text>? = null
        private var textures: TextureProvider = DEFAULT_TEXTURES
        private var child: TooltipChild? = null
        private var renderMessage: Boolean = true

        /**
         * Positions the widget in both x and y. Default is 0, 0
         * @param x horizontal position
         * @param y vertical position
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.5.?
         */
        fun position(x: Int, y: Int): Builder {
            this.x = x
            this.y = y
            return this
        }

        /**
         * Sets the width of this widget. Default value is 150
         * @param width Int width in pixels
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.5.?
         */
        fun width(width: Int): Builder {
            this.w = width
            return this
        }

        /**
         * Sets the height of this widget. Default value is 20
         * @param height Int height in pixels
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun height(height: Int): Builder {
            this.h = height
            return this
        }

        /**
         * Sets the width and height of this widget. Default is 150w x 20h
         * @param width Int width in pixels
         * @param height Int height in pixels
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun size(width: Int, height: Int): Builder {
            this.w = width
            this.h = height
            return this
        }

        /**
         * Sets the XY position and width/height of this widget. Defaults are position 0, 0 and 150w x 20h
         * @param x horizontal position
         * @param y vertical position
         * @param width Int width in pixels
         * @param height Int height in pixels
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun dimensions(x: Int, y: Int, width: Int, height: Int): Builder {
            return position(x, y).size(width, height)
        }

        /**
         * Applies a [Tooltip] to this widget. Default is null
         * @param tooltip [Tooltip], nullable
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.5.?
         */
        fun tooltip(tooltip: Tooltip?): Builder {
            this.tooltip = tooltip
            return this
        }

        /**
         * Applies the provided text as a [Tooltip] to this widget. Default is null
         * @param tooltip [Text], nullable. Converted to a [Tooltip] for this widget
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.5.?
         */
        fun tooltip(tooltip: Text?): Builder {
            this.tooltip = if (tooltip != null) Tooltip.of(tooltip) else null
            return this
        }

        /**
         * A tooltip supplier for this widget. Default is null
         * @param tooltipSupplier [Function]&lt;Boolean, [Text]&gt; converts the widgets current active state into a tooltip text
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.3
         */
        fun tooltipSupplier(tooltipSupplier: Function<Boolean, Text>): Builder {
            this.tooltipSupplier = tooltipSupplier
            return this
        }

        /**
         * Applies a narration supplier to this widget. Default is the standard supplier for buttons, which narrates the button as "Message button"
         * @param narrationSupplier [ButtonWidget.NarrationSupplier]
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.5.?
         */
        @Deprecated("Custom button widgets now use ActiveNarrationSupplier. Use the overload accepting that.")
        fun narrationSupplier(narrationSupplier: ButtonWidget.NarrationSupplier): Builder {
            this.narrationSupplier = ActiveNarrationSupplier { _, textSupplier -> narrationSupplier.createNarrationMessage(textSupplier) }
            return this
        }

        /**
         * Applies a narration supplier to this widget. Default is the standard supplier for buttons, which narrates the button as "Message button"
         * @param narrationSupplier [ActiveNarrationSupplier]
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.3
         */
        fun narrationSupplier(narrationSupplier: ActiveNarrationSupplier): Builder {
            this.narrationSupplier = narrationSupplier
            return this
        }

        /**
         * Applies a narration appender to this widget for adding narrations on top of the standard button narrations. Default is none.
         * @param narrationAppender [Consumer]&lt;[NarrationMessageBuilder]&gt; unlike the supplier, this is used to directly append additional narrations as needed to the message builder.
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun narrationAppender(narrationAppender: Consumer<NarrationMessageBuilder>): Builder {
            this.narrationAppender = narrationAppender
            return this
        }

        /**
         * Sets the active state of this button.
         * @param active true for active, false for inactive
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.5.?
         */
        fun active(active: Boolean): Builder {
            this.activeSupplier = Supplier { active }
            return this
        }

        /**
         * Sets the dynamic active state of this button.
         * @param activeSupplier supplier of active state. Should re-evaluate state on each call (or dynamically in some way as needed)
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.3
         */
        fun activeSupplier(activeSupplier: Supplier<Boolean>): Builder {
            this.activeSupplier = activeSupplier
            return this
        }

        /**
         * Applies a [TooltipChild] to this button for passing additional tooltip info
         * @param child [TooltipChild], used to pass additional tooltip context. This button will pass and tooltip from this child out to its own parent (this button is also a [TooltipChild])
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.5.9
         */
        fun child(child: TooltipChild): Builder {
            this.child = child
            return this
        }

        /**
         * Disables rendering of the label provided to the button
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun noMessage(): Builder {
            this.renderMessage = false
            return this
        }

        /**
         * Sets the dynamic label text this button.
         * @param messageSupplier a dynamic supplier of the button label. This should update its message as needed for the dynamic state of the button.
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.3
         */
        fun messageSupplier(messageSupplier: Supplier<Text>): Builder {
            this.messageSupplier = messageSupplier
            return this
        }

        /**
         * Defines the texture set used for rendering the button background
         * @param textures [TextureProvider] Predefined set of textures for this buttons various states
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.3, updated to use base provider interface 0.6.4
         */
        fun textures(textures: TextureProvider): Builder {
            this.textures = textures
            return this
        }

        /**
         * Defines the texture set used for rendering the button background
         * @param tex [Identifier] the "normal" texture, rendered when the button is active but not focused
         * @param disabled [Identifier] rendered when the button is disabled. This has higher priority than [highlighted], so will render focused or not.
         * @param highlighted [Identifier] rendered then the button is active and focused.
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun textures(tex: Identifier, disabled: Identifier, highlighted: Identifier): Builder {
            this.textures = TextureSet(tex, disabled, highlighted)
            return this
        }

        /**
         * Defines the texture set used for rendering the button background
         * @param tex [Identifier] the "normal" texture, rendered when the button is active but not focused
         * @param disabled [Identifier] rendered when the button is disabled. This has higher priority than [highlighted], so will render focused or not.
         * @param highlighted [Identifier] rendered then the button is active and focused.
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun textures(tex: Identifier, disabled: Identifier, highlighted: Identifier, highlightedDisabled: Identifier): Builder {
            this.textures = TextureSet.Quad(tex, disabled, highlighted, highlightedDisabled)
            return this
        }

        /**
         * Defines the texture for rendering the button background. The sprite rendered will not change based on object state.
         * @param tex [Identifier] the "normal" texture, rendered under any circumstance
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun texture(tex: Identifier): Builder {
            this.textures = TextureSet.Single(tex)
            return this
        }

        /**
         * Builds a [CustomButtonWidget] instance
         * @return [CustomButtonWidget]
         * @author fzzyhmstrs
         * @since 0.5.?
         */
        fun build(): CustomButtonWidget {
            val widget = CustomButtonWidget(x, y, w, h, message, onPress, narrationSupplier, narrationAppender, child, textures, renderMessage)
            widget.tooltip = tooltip
            widget.activeSupplier = activeSupplier
            widget.messageSupplier = messageSupplier
            widget.tooltipSupplier = tooltipSupplier
            return widget
        }

    }

    companion object {
        /**
         * Creates a [Builder] instance
         * @param message [Text] the button label. Rendering this can be disabled in the builder.
         * @param onPress [Consumer]&lt;[CustomButtonWidget]&gt; action to invoke when the button is clicked or activated
         * @author fzzyhmstrs
         * @since 0.5.?
         */
        @JvmStatic
        fun builder(message: Text, onPress: Consumer<CustomButtonWidget>): Builder {
            return Builder(message, onPress)
        }

        /**
         * Creates a [Builder] instance. Will have no message by default
         * @param onPress [Consumer]&lt;[CustomButtonWidget]&gt; action to invoke when the button is clicked or activated
         * @author fzzyhmstrs
         * @since 0.6.3
         */
        @JvmStatic
        fun builder(onPress: Consumer<CustomButtonWidget>): Builder {
            return Builder(onPress)
        }

        /**
         * A default instance of narration supplier. Simply returns the supplied test unchanged.
         * @author fzzyhmstrs
         * @since 0.5.?
         */
        @JvmStatic
        @Deprecated("Custom button widgets now use ActiveNarrationSupplier. See DEFAULT_ACTIVE_NARRATION_SUPPLIER")
        protected val DEFAULT_NARRATION_SUPPLIER: ButtonWidget.NarrationSupplier = ButtonWidget.NarrationSupplier { textSupplier: Supplier<MutableText?> -> textSupplier.get() }

        /**
         * A default instance of narration supplier. Simply returns the supplied test unchanged.
         * @author fzzyhmstrs
         * @since 0.6.3
         */
        @JvmStatic
        protected val DEFAULT_ACTIVE_NARRATION_SUPPLIER: ActiveNarrationSupplier = ActiveNarrationSupplier { _, textSupplier: Supplier<MutableText> -> textSupplier.get() }

        private val DEFAULT_ACTIVE_SUPPLIER = Supplier { true }
    }
}