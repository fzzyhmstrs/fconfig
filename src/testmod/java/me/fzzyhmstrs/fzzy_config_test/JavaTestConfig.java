/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config_test;

import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import net.minecraft.util.Identifier;

public class JavaTestConfig extends Config {
    public JavaTestConfig() {
        super(new Identifier("fzzy_config_test", "java_config"), "test");
    }

    public int anInt = 4;
    public boolean aBoolean = true;

    public ValidatedDouble validatedDouble = new ValidatedDouble(1.0, 1.0, 0.0);
}