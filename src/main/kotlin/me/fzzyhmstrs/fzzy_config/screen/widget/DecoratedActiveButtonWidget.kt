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

import me.fzzyhmstrs.fzzy_config.screen.decoration.SpriteDecorated
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * An [ActiveButtonWidget] that allows for parents to render its decoration
 *
 * This does NOT render its own decoration by default, something like a custom Screen or ParentElement that is checking for Decorated will typically render it
 * @param titleSupplier [Supplier]&lt;[Text]&gt; - supplies the message/label for this button
 * @param width Int - width of the widget
 * @param height Int - height of the widget
 * @param activeSupplier [Supplier]&lt;Boolean&gt; - Supplies whether this button is active or not
 * @param pressAction [Consumer]&lt;ActiveButtonWidget&gt; - action to take when the button is pressed
 * @param background [Identifier], optional - a custom background identifier. needs to be a nine-patch sprite
 * @author fzzyhmstrs
 * @since 0.2.0
 */
//client
open class DecoratedActiveButtonWidget(
    titleSupplier: Supplier<Text>,
    width: Int,
    height: Int,
    private val decoration: Identifier,
    activeProvider: Supplier<Boolean>,
    pressAction: Consumer<ActiveButtonWidget>,
    background: Identifier? = null)
    :
    ActiveButtonWidget(titleSupplier, width, height, activeProvider, pressAction, background),
    SpriteDecorated {

    constructor(title: Text,
                width: Int,
                height: Int,
                decoration: Identifier,
                activeProvider: Supplier<Boolean>,
                pressAction: Consumer<ActiveButtonWidget>,
                background: Identifier? = null): this(Supplier{title}, width, height, decoration, activeProvider, pressAction, background)

    override fun decorationId(): Identifier {
        return decoration
    }
}