package me.fzzyhmstrs.fzzy_config_test;

import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import net.minecraft.util.Identifier;

public class JavaTestConfig extends Config {
    public JavaTestConfig() {
        super(new Identifier("fzzy_config_test","java_config"),"test");
    }

    public int anInt = 4;
    public boolean aBoolean = true;

    public ValidatedDouble validatedDouble = new ValidatedDouble(1.0,1.0,0.0);
}