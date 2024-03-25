package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.validation.misc.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.number.*

internal object ValidatedNumberExamples {

    //Example ValidatedInt. Defaults to 20, allowable range 40 to 5. Uses a Slider widget
    val validatedInt = ValidatedInt(20, 40, 5)

    //You can define the widget type of ValidatedInt, either a Slider or a "textbox".
    val textBoxInt = ValidatedInt(20, 40, 5, ValidatedNumber.WidgetType.TEXTBOX)

    //Example unbounded ValidatedInt. Can be any valid integer value. Widget forced to "textbox" style
    val unboundedInt = ValidatedInt(20)

    //Shorthand unbounded int. The int is the default value
    val shorthandInt = 12.validated()


    //Example ValidatedByte. Defaults to 4, allowable range 8 to 0. Uses a Slider widget
    val validatedByte = ValidatedByte(4, 8, 0)

    //You can define the widget type of ValidatedByte, either a Slider or a "textbox"
    val textBoxByte = ValidatedByte(4, 8, 0, ValidatedNumber.WidgetType.TEXTBOX)

    //Example unbounded ValidatedByte. Can be any valid integer value. Widget forced to "textbox" style
    val unboundedByte = ValidatedByte(4)

    //Shorthand unbounded byte. The byte is the default value
    val shorthandByte = 12.toByte().validated()


    //Example ValidatedShort. Defaults to 4, allowable range 8 to 0. Uses a Slider widget
    val validatedShort = ValidatedShort(128, 512, -512)

    //You can define the widget type of ValidatedShort, either a Slider or a "textbox"
    val textBoxShort = ValidatedShort(128, 512, -512, ValidatedNumber.WidgetType.TEXTBOX)

    //Example unbounded ValidatedShort. Can be any valid integer value. Widget forced to "textbox" style
    val unboundedShort = ValidatedShort(128)

    //Shorthand unbounded byte. The byte is the default value
    val shorthandShort = 12.toShort().validated()


    //Example ValidatedLong. Defaults to 1000000L, allowable range 10000000000L to 0L. Uses a Slider widget
    val validatedLong = ValidatedLong(1000000L, 10000000000L, 0L)

    //You can define the widget type of ValidatedLong, either a Slider or a "textbox"
    val textBoxLong = ValidatedLong(1000000L, 10000000000L, 0L, ValidatedNumber.WidgetType.TEXTBOX)

    //Example unbounded ValidatedLong. Can be any valid integer value. Widget forced to "textbox" style
    val unboundedLong = ValidatedLong(1000000L)

    //Shorthand unbounded long. The long is the default value
    val shorthandLong = 100L.validated()


    //Example ValidatedDouble. Defaults to 2.0, allowable range 3.0 to 1.0. Uses a Slider widget
    val validatedDouble = ValidatedDouble(2.0, 3.0, 1.0)

    //You can define the widget type of ValidatedDouble, either a Slider or a "textbox"
    val textBoxDouble = ValidatedDouble(2.0, 3.0, 1.0, ValidatedNumber.WidgetType.TEXTBOX)

    //Example unbounded ValidatedDouble. Can be any valid integer value. Widget forced to "textbox" style
    val unboundedDouble = ValidatedDouble(2.0)

    //Shorthand unbounded double. The double is the default value
    val shorthandDouble = 4.0.validated()


    //Example ValidatedFloat. Defaults to 2f, allowable range 3f to 1f. Uses a Slider widget
    val validatedFloat = ValidatedFloat(2f, 3f, 1f)

    //You can define the widget type of ValidatedFloat, either a Slider or a "textbox"
    val textBoxFloat = ValidatedFloat(2f, 3f, 1f, ValidatedNumber.WidgetType.TEXTBOX)

    //Example unbounded ValidatedFloat. Can be any valid integer value. Widget forced to "textbox" style
    val unboundedFloat = ValidatedFloat(2f)

    //Shorthand unbounded float. The float is the default value
    val shorthandFloat = 4f.validated()

}