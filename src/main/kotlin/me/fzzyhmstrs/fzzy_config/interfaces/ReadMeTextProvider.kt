package me.fzzyhmstrs.fzzy_config.interfaces

/**
 * A class marked as a ReadMeTextProvider can be scraped by a ReadMeBuilder for building a ReadMeText
 */
interface ReadMeTextProvider{
    /**
     * Called by ReadMeBuilder to get a raw line of readme text. It will apply a decorator to this line, so adding formatting may confuse the overall readme appearance
     */
    fun readmeText(): String
}