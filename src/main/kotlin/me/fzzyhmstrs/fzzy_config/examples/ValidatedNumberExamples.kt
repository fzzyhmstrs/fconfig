package me.fzzyhmstrs.fzzy_config.examples

import me.fzzyhmstrs.fzzy_config.validation.number.*

internal object ValidatedNumberExamples {

    //Example ValidatedInt. Defaults to 20, allowable range 40 to 5. Uses a Slider widget
    val validatedInt = ValidatedInt(20, 40, 5)

    //You can define the widget type of ValidatedInt, either a Slider or a "textbox".
    val textBoxInt = ValidatedInt(20, 40, 5, ValidatedNumber.WidgetType.TEXTBOX)

    //Example unbounded ValidatedInt. Can be any valid integer value. Widget forced to "textbox" style
    val rangedInt = ValidatedInt(20, 0..40)

    //Example unbounded ValidatedInt. Can be any valid integer value. Widget forced to "textbox" style
    val unboundedInt = ValidatedInt(20)

    //Example validation-only Int (unless your default happens to be 0)
    val emptyInt = ValidatedInt()


    //Example ValidatedByte. Defaults to 4, allowable range 8 to 0. Uses a Slider widget
    val validatedByte = ValidatedByte(4, 8, 0)

    //You can define the widget type of ValidatedByte, either a Slider or a "textbox"
    val textBoxByte = ValidatedByte(4, 8, 0, ValidatedNumber.WidgetType.TEXTBOX)

    //Example unbounded ValidatedByte. Can be any valid integer value. Widget forced to "textbox" style
    val unboundedByte = ValidatedByte(4)

    //Example validation-only Byte (unless your default happens to be 0)
    val emptyByte = ValidatedByte()


    //Example ValidatedShort. Defaults to 4, allowable range 8 to 0. Uses a Slider widget
    val validatedShort = ValidatedShort(128, 512, -512)

    //You can define the widget type of ValidatedShort, either a Slider or a "textbox"
    val textBoxShort = ValidatedShort(128, 512, -512, ValidatedNumber.WidgetType.TEXTBOX)

    //Example unbounded ValidatedShort. Can be any valid integer value. Widget forced to "textbox" style
    val unboundedShort = ValidatedShort(128)

    //Example validation-only Short (unless your default happens to be 0)
    val emptyShort = ValidatedShort()


    //Example ValidatedLong. Defaults to 1000000L, allowable range 10000000000L to 0L. Uses a Slider widget
    val validatedLong = ValidatedLong(1000000L, 10000000000L, 0L)

    //You can define the widget type of ValidatedLong, either a Slider or a "textbox"
    val textBoxLong = ValidatedLong(1000000L, 10000000000L, 0L, ValidatedNumber.WidgetType.TEXTBOX)

    //Example unbounded ValidatedLong. Can be any valid integer value. Widget forced to "textbox" style
    val rangedLong = ValidatedLong(1000000L)

    //Example unbounded ValidatedLong. Can be any valid integer value. Widget forced to "textbox" style
    val unboundedLong = ValidatedLong(1000000L)

    //Example validation-only Long (unless your default happens to be 0L)
    val emptyLong = ValidatedLong()

    //Example ValidatedDouble. Defaults to 2.0, allowable range 3.0 to 1.0. Uses a Slider widget
    val validatedDouble = ValidatedDouble(2.0, 3.0, 1.0)

    //You can define the widget type of ValidatedDouble, either a Slider or a "textbox"
    val textBoxDouble = ValidatedDouble(2.0, 3.0, 1.0, ValidatedNumber.WidgetType.TEXTBOX)

    //Example unbounded ValidatedDouble. Can be any valid integer value. Widget forced to "textbox" style
    val unboundedDouble = ValidatedDouble(2.0)

    //Example validation-only Double (unless your default happens to be 0.0)
    val emptyDouble = ValidatedDouble()


    //Example ValidatedFloat. Defaults to 2f, allowable range 3f to 1f. Uses a Slider widget
    val validatedFloat = ValidatedFloat(2f, 3f, 1f)

    //You can define the widget type of ValidatedFloat, either a Slider or a "textbox"
    val textBoxFloat = ValidatedFloat(2f, 3f, 1f, ValidatedNumber.WidgetType.TEXTBOX)

    //Example unbounded ValidatedFloat. Can be any valid integer value. Widget forced to "textbox" style
    val unboundedFloat = ValidatedFloat(2f)

    //Example validation-only Float (unless your default happens to be 0f)
    val emptyFloat = ValidatedFloat()
}