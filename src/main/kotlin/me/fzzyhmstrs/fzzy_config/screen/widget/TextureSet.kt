package me.fzzyhmstrs.fzzy_config.screen.widget

import net.minecraft.util.Identifier

/**
 * A set of textures used for enabled, disabled, and focused contexts of an element. All three textures can be the same, in which case the texture rendered is of course the same no matter the state of the underlying object.
 *
 * [ClickableWidget][net.minecraft.client.gui.widget.ClickableWidget] is an example of an object that tracks active and highlighted state that this set would apply to. In fact, [PressableWidget][net.minecraft.client.gui.widget.PressableWidget] uses a similar concept as TextureSet, [ButtonTextures][net.minecraft.client.gui.screen.ButtonTextures]. Texture set doesn't provide different textures for focused/unfocused while disabled.
 * @param tex [Identifier] the "normal" texture, rendered when the object is active but not focused
 * @param disabled [Identifier] rendered when the object is disabled. This has higher priority than [highlighted], so will render focused or not.
 * @param highlighted [Identifier] rendered then the object is active and focused.
 * @author fzzyhmstrs
 * @since 0.6.0, implements `TextureProvider` 0.6.4
 */
data class TextureSet(private val tex: Identifier, private val disabled: Identifier, private val highlighted: Identifier): TextureProvider {

    /**
     * A set of textures where all three stored textures are the exact same. The sprite rendered will not change based on object state.
     * @param id [Identifier] the "normal" texture, rendered under any circumstance
     * @author fzzyhmstrs
     * @since 0.6.0, deprecated 0.6.4
     */
    @Deprecated("Use TextureSet.Single instead")
    constructor(id: Identifier): this(id, id, id)

    /**
     * Retrieves the appropriate texture based on provided state
     * @param enabled The active state of the underlying object, such as [ClickableWidget.active][net.minecraft.client.gui.widget.ClickableWidget.active]
     * @param focused The highlighted state of the underlying object. This is usually a combination of "focused" in the Minecraft sense as well as hovered. [ClickableWidget.isSelected][net.minecraft.client.gui.widget.ClickableWidget.isSelected] checks for both, for example.
     * @return [Identifier] from the texture set matching the current object state.
     * @author fzzyhmstrs
     * @since 0.6.0, overrides from `TextureProvider` 0.6.4
     */
    override fun get(enabled: Boolean, focused: Boolean): Identifier {
        return if (enabled) {
            if (focused) highlighted else tex
        } else {
            disabled
        }
    }

    data class Single(private val tex: Identifier): TextureProvider {

        override fun get(enabled: Boolean, focused: Boolean): Identifier {
            return tex
        }
    }

    data class Quad(private val tex: Identifier, private val disabled: Identifier, private val highlighted: Identifier, private val disabledHighlighted: Identifier): TextureProvider {

        override fun get(enabled: Boolean, focused: Boolean): Identifier {
            return if (enabled) {
                if (focused) highlighted else tex
            } else {
                if (focused) disabledHighlighted else disabled
            }
        }
    }
}