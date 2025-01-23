/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget.Companion.DEFAULT_TEXTURES
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A Button Widget that can supply its message and active state, and render a custom background
 * @param titleSupplier [Supplier]&lt;[Text]&gt; - supplies the message/label for this button
 * @param width Int - width of the widget
 * @param height Int - height of the widget
 * @param activeSupplier [Supplier]&lt;Boolean&gt; - Supplies whether this button is active or not
 * @param pressAction [Consumer]&lt;ActiveButtonWidget&gt; - action to take when the button is pressed
 * @param textures [TextureSet], optional - a custom background texture set; defaults to [DEFAULT_TEXTURES]
 * @author fzzyhmstrs
 * @since 0.2.0, implements TextureSet for backgrounds 0.6.0, deprecated 0.6.3 for removal by 0.7.0
 */
//client
@Deprecated("Unused internally. Switch to CustomButtonWidget#builder. Scheduled for removal 0.7.0")
open class ActiveButtonWidget@JvmOverloads constructor(
    private val titleSupplier: Supplier<Text>,
    width: Int,
    height: Int,
    private val activeSupplier: Supplier<Boolean>,
    private val pressAction: Consumer<ActiveButtonWidget>,
    override val textures: TextureSet = DEFAULT_TEXTURES)
    :
    CustomPressableWidget(0, 0, width, height, titleSupplier.get()) {

    /**
     * A Button Widget that can supply its message and active state, and render a custom background
     * @param title [Text] - The message/label for this button
     * @param width Int - width of the widget
     * @param height Int - height of the widget
     * @param activeSupplier [Supplier]&lt;Boolean&gt; - Supplies whether this button is active or not
     * @param pressAction [Consumer]&lt;ActiveButtonWidget&gt; - action to take when the button is pressed
     * @param background [TextureSet], Nullable - a custom background texture set; defaults to [DEFAULT_TEXTURES] if null is passed (default)
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    constructor(
        title: Text,
        width: Int,
        height: Int,
        background: TextureSet?,
        activeSupplier: Supplier<Boolean>,
        pressAction: Consumer<ActiveButtonWidget>)
            :
            this(Supplier { title }, width, height, activeSupplier, pressAction, background ?: DEFAULT_TEXTURES)

    /**
     * A Button Widget that can supply its message and active state, and render a custom background
     * @param title [Text] - The message/label for this button
     * @param width Int - width of the widget
     * @param height Int - height of the widget
     * @param activeSupplier [Supplier]&lt;Boolean&gt; - Supplies whether this button is active or not
     * @param pressAction [Consumer]&lt;ActiveButtonWidget&gt; - action to take when the button is pressed
     * @param background [Identifier], Nullable - a custom background texture which will be used for all rendering circumstances; defaults to the "normal" texture for [DEFAULT_TEXTURES] if null is passed (default)
     * @author fzzyhmstrs
     * @since 0.2.0, demoted to tertiary constructor 0.6.0 for [TextureSet] constructors
     */
    @JvmOverloads
    constructor(
        title: Text,
        width: Int,
        height: Int,
        activeSupplier: Supplier<Boolean>,
        pressAction: Consumer<ActiveButtonWidget>,
        background: Identifier? = null)
            :
            this(title, width, height, background?.let { TextureSet(it) }, activeSupplier, pressAction)

    init {
        FC.LOGGER.error("This class is marked for removal. Please reimplement as a CustomButtonWidget or use CustomButtonWidget#builder")
    }

    /**
     * @suppress
     */
    override fun getMessage(): Text {
        return titleSupplier.get()
    }

    override fun renderCustom(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
        this.active = activeSupplier.get()
        super.renderCustom(context, x, y, width, height, mouseX, mouseY, delta)
    }

    /**
     * @suppress
     */
    override fun onPress() {
        pressAction.accept(this)
    }
}