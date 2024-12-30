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

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget.Companion.DEFAULT_TEXTURES
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.MathHelper
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
 * @since 0.2.0, implements TextureSet for backgrounds 0.6.0
 */
//client
open class ActiveButtonWidget@JvmOverloads constructor(
    private val titleSupplier: Supplier<Text>,
    width: Int,
    height: Int,
    private val activeSupplier: Supplier<Boolean>,
    private val pressAction: Consumer<ActiveButtonWidget>,
    override val textures: TextureSet = DEFAULT_TEXTURES)
    :
    CustomPressableWidget(0, 0, width, height, titleSupplier.get()) {

    //TODO
    @JvmOverloads
    constructor(
        title: Text,
        width: Int,
        height: Int,
        activeProvider: Supplier<Boolean>,
        pressAction: Consumer<ActiveButtonWidget>,
        background: TextureSet? = null)
            :
            this(Supplier { title }, width, height, activeProvider, pressAction, background ?: DEFAULT_TEXTURES)

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