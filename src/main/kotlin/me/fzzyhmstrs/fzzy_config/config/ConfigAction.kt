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

import com.google.common.collect.Sets
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.entry.EntryCreator
import me.fzzyhmstrs.fzzy_config.entry.EntryFlag
import me.fzzyhmstrs.fzzy_config.entry.EntryKeyed
import me.fzzyhmstrs.fzzy_config.entry.EntryWidget
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.decoration.SpriteDecoration
import me.fzzyhmstrs.fzzy_config.screen.entry.EntryCreators
import me.fzzyhmstrs.fzzy_config.screen.widget.ActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.SharedConstants
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ConfirmLinkScreen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.ClickEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import org.jetbrains.annotations.ApiStatus.Internal
import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.util.function.Supplier
import kotlin.experimental.and

/**
 * Builds a button that will appear in a Config GUI, to perform some arbitrary, possible non-config-related action
 *
 * This could be used to link to a wiki, open another non-fzzy-config config, open a non-config screen, open a patchouli guide, run a command, and so on.
 * @param titleSupplier Supplier&lt;Text&gt; - supplies a name for the button widget. Will be checked every frame, so the button name will dynamically update as needed.
 * @param activeSupplier Supplier&lt;Boolean&gt; - supplies an active state; whether the button is insactive ("greyed out" and unclickable) or active (functioning normally)
 * @param pressAction [Runnable] - the action to execute on clicking the button
 * @param background [Identifier], nullable - if non-null, will provide a custom background for the widget rendering.
 * @param decoration [Decorated], nullable - if non-null, will render a "decoration" next to the widget. These are the typically white/wireframe icons shown next to certain settings like lists.
 * @author fzzyhmstrs
 * @since 0.5.0, Decorated incorporated 0.6.0
 */
class ConfigAction @JvmOverloads constructor(
    private val titleSupplier: Supplier<Text>,
    private val activeSupplier: Supplier<Boolean>,
    private val pressAction: Runnable,
    private val decoration: Decorated?,
    private val description: Text? = null,
    private val background: ActiveButtonWidget.Background? = null)
:
    EntryWidget<Any>,
    EntryFlag,
    EntryCreator,
    EntryKeyed,
    Translatable
{

    constructor(
    titleSupplier: Supplier<Text>,
    activeSupplier: Supplier<Boolean>,
    pressAction: Runnable,
    decoration: Identifier?,
    description: Text? = null,
    background: Identifier? = null): this(titleSupplier, activeSupplier, pressAction, decoration?.let{ SpriteDecoration(it) }, description, background)

    private var flags: Byte = 0
    private var actionKey = "fc.config.generic.action"

    @Internal
    override fun widgetEntry(choicePredicate: ChoiceValidator<Any>): ClickableWidget {
        return ActiveButtonWidget(titleSupplier, 110, 20, activeSupplier, { _ -> pressAction.run() }, background)
    }

    @Internal
    override fun createEntry(context: EntryCreator.CreatorContext): List<EntryCreator.Creator> {
        return EntryCreators.createActionEntry(context, decoration, this.widgetEntry())
    }

    internal fun setFlag(flag: Byte) {
        if (hasFlag(flag)) return
        this.flags = (this.flags + flag).toByte()
    }

    private fun hasFlag(flag: Byte): Boolean {
        return (this.flags and flag) == flag
    }

    override fun hasFlag(flag: EntryFlag.Flag): Boolean {
        return this.hasFlag(flag.flag)
    }

    /**
     * Builds a [ConfigAction]
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    class Builder {
        private var titleSupplier: Supplier<Text> = Supplier { FcText.EMPTY }
        private var activeSupplier: Supplier<Boolean> = Supplier { true }
        private var desc: Text? = null
        private var background: ActiveButtonWidget.Background? = null
        private var decoration: Identifier? = null
        private var flags: Byte = 0

        /**
         * Sets the title of the widget. This will be a static title, unchanging based on state.
         * @param title [Text] the button widget title
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.5.0
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
         * @since 0.5.0
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
         * @since 0.5.0
         */
        fun active(activeSupplier: Supplier<Boolean>): Builder {
            this.activeSupplier = activeSupplier
            return this
        }

        /**
         * Sets a custom background for the button widget, which will appear when the button is selected and active
         * @param background [ActiveButtonWidget.Background] the background sprite id and padding
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.5.0
         */
        fun background(background: ActiveButtonWidget.Background): Builder {
            this.background = background
            return this
        }

        /**
         * Defines a decoration texture id. This will be drawn to the left of the button widget in the config screen. Decorations are typically 20x20 at the most
         * @param id [Identifier] decoration sprite id
         * @see me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.5.0
         */
        fun decoration(id: Identifier): Builder {
            this.decoration = id
            return this
        }

        /**
         * Defines the tooltip description for this button. Default is no tooltip.
         * @param desc [Text] the tooltip to display. Will be split be newlines automatically
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.5.6
         */
        fun desc(desc: Text): Builder {
            this.desc = desc
            return this
        }

        /**
         * Adds a flag to this Action.
         * @param flag [EntryFlag.Flag] flag to add to this action
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.5.6
         */
        fun flag(flag: EntryFlag.Flag): Builder {
            this.flags = (this.flags + flag.flag).toByte()
            return this
        }

        /**
         * Builds the [ConfigAction] with the provided runnable as the on-click event
         * @param action [Runnable] - event to run when the button is clicked
         * @return [ConfigAction]
         * @author fzzyhmstrs
         * @since 0.5.0
         */
        fun build(action: Runnable): ConfigAction {
            val q = ConfigAction(titleSupplier, activeSupplier, action, decoration ?: TextureIds.DECO_BUTTON_CLICK, desc, background)
            q.flags = flags
            return q
        }

        /**
         * Builds the [ConfigAction] with the supplied ClickEvent as the on-click event
         * @param clickEvent [ClickEvent] - event to run when the button is clicked
         * @return [ConfigAction]
         * @author fzzyhmstrs
         * @since 0.5.0
         */
        fun build(clickEvent: ClickEvent): ConfigAction {
            val runnable = Runnable {
                val client = MinecraftClient.getInstance()
                if (clickEvent.action == ClickEvent.Action.OPEN_URL) {
                    if (!client.options.chatLinks.value) {
                        return@Runnable
                    }

                    try {
                        val uRI = URI(clickEvent.value)
                        val string = uRI.scheme ?: throw URISyntaxException(clickEvent.value, "Missing protocol")
                        if (!Sets.newHashSet("http", "https").contains(string.lowercase())) {
                            throw URISyntaxException(clickEvent.value, "Unsupported protocol: " + string.lowercase())
                        }
                        if (client.options.chatLinksPrompt.value) {
                            val screen = client.currentScreen
                            client.setScreen(ConfirmLinkScreen({ confirmed: Boolean ->
                                if (confirmed) {
                                    Util.getOperatingSystem().open(uRI)
                                }
                                client.setScreen(screen)
                            }, clickEvent.value, false))
                        } else {
                            Util.getOperatingSystem().open(uRI)
                        }
                    } catch (var4: URISyntaxException) {
                        FC.LOGGER.error("Can't open url for {}", clickEvent, var4)
                    }
                } else if (clickEvent.action == ClickEvent.Action.OPEN_FILE) {
                    Util.getOperatingSystem().open(File(clickEvent.value))
                } else if (clickEvent.action == ClickEvent.Action.SUGGEST_COMMAND) {
                    FC.LOGGER.error("Can't suggest a command from a config action")
                } else if (clickEvent.action == ClickEvent.Action.RUN_COMMAND) {
                    val string = SharedConstants.stripInvalidChars(clickEvent.value)
                    if (string.startsWith("/")) {
                        if (client.player?.networkHandler?.sendCommand(string.substring(1)) != true) {
                            FC.LOGGER.error("Not allowed to run command with signed argument from click event: '{}'", string)
                        }
                    } else {
                        FC.LOGGER.error("Failed to run command without '/' prefix from click event: '{}'", string)
                    }
                } else if (clickEvent.action == ClickEvent.Action.COPY_TO_CLIPBOARD) {
                    client.keyboard.clipboard = clickEvent.value
                } else {
                    FC.LOGGER.error("Don't know how to handle {}", clickEvent)
                }
            }
            val action = clickEvent.action
            val value = clickEvent.value
            if (desc == null && action != null) {
                desc = when(action) {
                    ClickEvent.Action.OPEN_URL -> "fc.button.click.open_url".translate(value)
                    ClickEvent.Action.OPEN_FILE -> "fc.button.click.open_file".translate(value)
                    ClickEvent.Action.RUN_COMMAND -> "fc.button.click.run_command".translate(value)
                    ClickEvent.Action.SUGGEST_COMMAND -> null
                    ClickEvent.Action.CHANGE_PAGE -> null
                    ClickEvent.Action.COPY_TO_CLIPBOARD -> "fc.button.click.copy_to_clipboard".translate(value)

                }
            }
            if (decoration == null  && action != null) {
                decoration = when(action) {
                    ClickEvent.Action.OPEN_URL -> TextureIds.DECO_LINK
                    ClickEvent.Action.OPEN_FILE -> TextureIds.DECO_FOLDER
                    ClickEvent.Action.RUN_COMMAND -> TextureIds.DECO_COMMAND
                    ClickEvent.Action.SUGGEST_COMMAND -> TextureIds.DECO_BUTTON_CLICK
                    ClickEvent.Action.CHANGE_PAGE -> TextureIds.DECO_BUTTON_CLICK
                    ClickEvent.Action.COPY_TO_CLIPBOARD -> TextureIds.DECO_BUTTON_CLICK
                }
            }
            when(action) {
                ClickEvent.Action.RUN_COMMAND -> this.flag(EntryFlag.Flag.REQUIRES_WORLD)
                ClickEvent.Action.SUGGEST_COMMAND -> this.flag(EntryFlag.Flag.REQUIRES_WORLD)
                else -> {}
            }
            val q = ConfigAction(titleSupplier, activeSupplier, runnable, decoration ?: TextureIds.DECO_BUTTON_CLICK, desc, background)
            q.flags = flags
            return q
        }
    }

    @Internal
    override fun getEntryKey(): String {
        return actionKey
    }

    @Internal
    override fun setEntryKey(key: String) {
        actionKey = key
    }

    override fun translationKey(): String {
        return getEntryKey()
    }

    override fun descriptionKey(): String {
        return getEntryKey() + ".desc"
    }

    override fun prefixKey(): String {
        return getEntryKey() + ".prefix"
    }

    override fun hasDescription(): Boolean {
        return description != null || super.hasDescription()
    }

    override fun description(fallback: String?): MutableText {
        return description?.copy() ?: super.description(fallback)
    }
}