/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.declaration

import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.ValueBuilders
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.DeclarationKey
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.value.LengthValue
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.value.PaddingDeclarationKey
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.value.ScrollBarDeclarationKey
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.value.SimpleValueKey
import net.minecraft.util.Identifier

object DeclarationKeys {
    val SCROLL_BAR = ScrollBarDeclarationKey.register("scroll-bar", *ScrollBarDeclarationKey.ALIASES)
    val NARRATION_KEY = SimpleValueKey("narration-key", "fc.narrator.position.list", ValueBuilders.STRING).register("narration-key")
    val TRANSLATION_KEY = SimpleValueKey("translation-key", "default.translation.key", ValueBuilders.STRING).register("translation-key")
    val PADDING = PaddingDeclarationKey.register("padding", *PaddingDeclarationKey.ALIASES)
    val HEIGHT = SimpleValueKey(
        "height",
        LengthValue(100.0, LengthValue.Unit.PERCENTAGE),
        ValueBuilders.dimensionValueBuilder(::height)).register("height")
    val WIDTH = SimpleValueKey(
        "width",
        LengthValue(100.0, LengthValue.Unit.PERCENTAGE),
        ValueBuilders.dimensionValueBuilder(::width)).register("width")
    val FONT = FontDeclarationKey.register("font", *FontDeclarationKey.ALIASES)
      //Consider adding more font-specific things that are now available in newer versions if I plan to put 0.8.0 to new versions only
    //val BORDER = TODO()
    //val OUTLINE..? Replace border, or allow for either/both?
    //val BACKGROUND
    //val margin..? Do I do full box model..?
    //val place-content..? As part of the new layout model..?
    //val DECO_ICON_X -> props for defining the image used by the various icons
      // create an enum for getting the DECO from context, or just have the collection of keys


    // Layout discussion
      // position - for layout?
      // May want to consider my own LAYOUT property that lets you define the various layout enums etc I have defined
      // Might need a layout take 3, since in CSS the layout is dynamic based on selector context in the moment.
      // Different layouts on different screen resolutions etc.
        // Need to think about my potential overhaul to widget positioning very carefully. 
        // Everything is teetering towards ripping out my current Pos paradigm for a DOM-like separately computed/applied positioning and sizing
      // Might be something like: a DOM element submits itself as context in a tree. That context provides hooks for gettings things like 
        // the background image
        // the current border
        // text color
        // etc etc.
      // Might not be the elements responsibility to get things like their background, text rendering, etc.
        // they submit into the context: get my [blah] (e.g., get my background renderer)
        // This logically flows into the sentiment that what I'm actually doing is completely replacing the vanilla GUI layout system.
        // COMPLETELY. Like no such thing as a widget any more.
        // Visual elements are contexts and types of things.
        // Almost a javascript approach to building a webpage. Code-based building of a DOM
      // So, is that worth it....
        // If I do it, I give myself the freedom to start from scratch on every feature. How do elements transition between each other etc.
        // But I burden myself with re-implementing every feature the MC GUI has, navigation, narration, etc.
        // If I pull it off, I could split it out into a GUI library and dep into FC
        // And give the power to others.
          // Get back to the Wiki mod I was working on, 1.21.11+ in this case, based on MDX
      // If I reign back to just flavoring, my scope of work drops dramatically (does it?)
        // I still have to fight with positioning. Either I have a neutered approach to positioning (alignments, layouts)
        // or I focus on visuals for now.
        // I really think the ability to custom position elements would be huge
          // Imagine being able to put the bottom bars along the side, or "max out" the list to the whole screen width
      //No matter what, I should start planning out the DOM structure that will be the backbone for context storage/interaction
        // parent-child tree that is traversed by elements as they render
        // Might rework selector context into a general "DOM Element". Might be an interface so I can have things directly implement
          // or have wrappers for other things like clickable widgets
        // It does mean I should completely remove "ClickableWidget" as a thing from ValidatedField. It should become a CustomPressableWidget
          // or whatever other base class I decide that will use the DOM for getting style info

    private fun height(): DeclarationKey<LengthValue, *> {
        return HEIGHT
    }

    private fun width(): DeclarationKey<LengthValue, *> {
        return WIDTH
    }

    private fun fontFamily(): DeclarationKey<Identifier, *> {
        return FONT_FAMILY
    }

    fun <F: DeclarationKey<*, *>> F.register(declaration: String, vararg alias: String): F {
        DeclarationKey.register(declaration, this, *alias)
        return this
    }
}
