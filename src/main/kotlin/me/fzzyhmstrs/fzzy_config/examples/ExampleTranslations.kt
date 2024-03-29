package me.fzzyhmstrs.fzzy_config.examples

object ExampleTranslations {

    //fields and sections have lang keys based on their "location" in the Config class graph.
    //Lange key composition is as follows
    //1. the namespace of the config id: (my_mod)
    //2. the path of the config id: (my_mod.my_config)
    //3. any parent ConfigSection field names as declared in-code: (my_mod.my_config.subSection)
    //4. the setting field name as declared in-code: (my_mod.my_config.subSection.fieldName)
    val lang = """
    {
        "_comment1": "the lang for an example 'fieldName' setting in a config inside section 'subSection'",
        "my_mod.my_config.subSection.fieldName": "Very Important Setting",
        "my_mod.my_config.subSection.fieldName.desc": "This very important setting is used in this very important way.",
        
        "_comment2": "this is the lang for the corresponding subSection",
        "my_mod.my_config.subSection": "Important Settings",
        "my_mod.my_config.subSection.desc": "Important settings related to important things are in this section."
        
        "_comment2": "the lang for the base config itself",
        "my_mod.my_config": "My Mod's Config",
        "my_mod.my_config.desc": "Lots of really cool config settings live in this config."
    }
    """

    //fields and sections have lang keys based on their "location" in the Config class graph.
    //Lange key composition is as follows
    //1. the namespace of the config id: (my_mod)
    //2. the path of the config id: (my_mod.my_config)
    //3. any parent ConfigSection field names as declared in-code: (my_mod.my_config.subSection)
    //4. the setting field name as declared in-code: (my_mod.my_config.subSection.fieldName)
    val fieldLang = """
    {
        "_comment1": "the lang for an example 'fieldName' setting in a config inside section 'subSection'",
        "my_mod.my_config.subSection.fieldName": "Very Important Setting",
        "my_mod.my_config.subSection.fieldName.desc": "This very important setting is used in this very important way."
    }
    """

}