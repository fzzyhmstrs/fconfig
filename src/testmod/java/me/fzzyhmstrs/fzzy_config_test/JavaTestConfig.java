package me.fzzyhmstrs.fzzy_config_test;

import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import org.jetbrains.annotations.NotNull;

public class JavaTestConfig extends Config {

    public JavaTestConfig() {
        super("java_config", "fzzy_config_test", "test");
    }

    public int anInt = 4;
    public boolean aBoolean = true;

    public ValidatedDouble validatedDouble = new ValidatedDouble(1.0,1.0,0.0);
}