package me.fzzyhmstrs.fzzy_config.interfaces

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.config_util.SyncedConfigHelperV1
import java.io.File
import java.io.FileWriter

/**
 * Provides a helper method to an inheriting class that writes a list of strings to file
 */
interface ReadMeWriter{
    /**
     * Writes a file using the list of lines provided by [readmeText]
     *
     * @param file String. The filename to write. Needs a suffix like "myFile.txt"
     * @param base String, optional.
     */
    fun writeReadMe(file: String, base: String = FC.MOD_ID){
        val textLines: List<String> = readmeText()
        val dirPair = SyncedConfigHelperV1.makeDir("", base)
        if (!dirPair.second){
            println("Couldn't make directory for storing the readme")
        }
        val f = File(dirPair.first,file)
        val fw = FileWriter(f)
        textLines.forEach {
                value -> fw.write(value)
            fw.write(System.getProperty("line.separator"))
        }
        fw.close()
    }

    /**
     * implementations of this method should pass back the list of text they need written to file. Ordering should be maintained via a LinkedList or similar
     */
    fun readmeText(): List<String>
}