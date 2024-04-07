package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.screen.entry.Decorated
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.function.Consumer
import java.util.function.Supplier

open class DecoratedActiveButtonWidget(
    titleSupplier: Supplier<Text>,
    width: Int,
    height: Int,
    private val decoration: Identifier,
    activeProvider: Supplier<Boolean>,
    pressAction: Consumer<ActiveButtonWidget>,
    background: Identifier? = null)
    :
    ActiveButtonWidget(titleSupplier,width,height,activeProvider,pressAction,background),
    Decorated{

    constructor(title: Text,
                width: Int,
                height: Int,
                decoration: Identifier,
                activeProvider: Supplier<Boolean>,
                pressAction: Consumer<ActiveButtonWidget>,
                background: Identifier? = null): this(Supplier{title},width, height,decoration, activeProvider, pressAction, background)

    override fun decorationId(): Identifier {
        return decoration
    }
}