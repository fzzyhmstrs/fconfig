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

import me.fzzyhmstrs.fzzy_config.screen.widget.TextureSet
import me.fzzyhmstrs.fzzy_config.screen.widget.TooltipChild
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.function.Consumer
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
 * @param narrationSupplier [ButtonWidget.NarrationSupplier] same use as in vanilla; converts a provided input text into a narration text.
 * @param narrationAppender [Consumer]&lt;[NarrationMessageBuilder]&gt; unlike the supplier, this is used to directly append additional narrations as needed to the message builder.
 * @param textures [TextureSet], default [CustomPressableWidget.DEFAULT_TEXTURES]. The textures for this button
 * @param child [TooltipChild], used to pass additional tooltip context. This button will pass and tooltip from this child out to its own parent (this button is also a [TooltipChild])
 * @param renderMessage If false, the label won't be rendered
 * @author fzzyhmstrs
 * @since 0.5.?
 */
open class CustomButtonWidget protected constructor(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Text,
    private val pressAction: Consumer<CustomButtonWidget>,
    private val narrationSupplier: ButtonWidget.NarrationSupplier,
    private val narrationAppender: Consumer<NarrationMessageBuilder> = Consumer { _-> },
    override val textures: TextureSet = DEFAULT_TEXTURES,
    private val child: TooltipChild? = null,
    private val renderMessage: Boolean = true)
    :
    CustomPressableWidget(x, y, width, height, message)
{

    override fun onPress() {
        pressAction.accept(this)
    }

    override fun renderCustom(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
        if (renderMessage)
            super.renderCustom(context, x, y, width, height, mouseX, mouseY, delta)
    }

    override fun getNarrationMessage(): MutableText {
        return narrationSupplier.createNarrationMessage { super.getNarrationMessage() }
    }

    override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Text> {
        return child?.provideTooltipLines(mouseX, mouseY, parentSelected, keyboardFocused) ?: super.provideTooltipLines(mouseX, mouseY, parentSelected, keyboardFocused)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
        super.appendClickableNarrations(builder)
        if (builder != null)
            narrationAppender.accept(builder)
    }

    /**
     * Builds a [CustomButtonWidget]
     * @param message [Text] the button label
     * @param onPress [Consumer]&lt;[CustomButtonWidget]&gt; action to invoke when the button is clicked or activated
     * @author fzzyhmstrs
     * @since 0.5.?
     */
    class Builder(private val message: Text, private val onPress: Consumer<CustomButtonWidget>) {

        private var tooltip: Tooltip? = null
        private var x = 0
        private var y = 0
        private var w = 150
        private var h = 20
        private var narrationSupplier: ButtonWidget.NarrationSupplier = DEFAULT_NARRATION_SUPPLIER
        private var narrationAppender: Consumer<NarrationMessageBuilder> = Consumer { _-> }
        private var active = true
        private var textures: TextureSet = DEFAULT_TEXTURES
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
         * Applies a narration supplier to this widget. Default is the standard supplier for buttons, which narrates the button as "Message button"
         * @param narrationSupplier [ButtonWidget.NarrationSupplier]
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.5.?
         */
        fun narrationSupplier(narrationSupplier: ButtonWidget.NarrationSupplier): Builder {
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
            this.active = active
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
         * Defines the texture set used for rendering the button background
         * @param
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun textures(tex: Identifier, disabled: Identifier, highlighted: Identifier): Builder {
            this.textures = TextureSet(tex, disabled, highlighted)
            return this
        }

        //TODO
        fun texture(tex: Identifier): Builder {
            this.textures = TextureSet(tex)
            return this
        }

        //TODO
        fun build(): CustomButtonWidget {
            val widget = CustomButtonWidget(x, y, w, h, message, onPress, narrationSupplier, narrationAppender, textures, child, renderMessage)
            widget.tooltip = tooltip
            widget.active = active
            return widget
        }

    }

    companion object {
        //TODO
        @JvmStatic
        fun builder(message: Text, onPress: Consumer<CustomButtonWidget>): Builder {
            return Builder(message, onPress)
        }

        //TODO
        @JvmStatic
        protected val DEFAULT_NARRATION_SUPPLIER: ButtonWidget.NarrationSupplier = ButtonWidget.NarrationSupplier { textSupplier: Supplier<MutableText?> -> textSupplier.get() }
    }
}