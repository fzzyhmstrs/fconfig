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

import me.fzzyhmstrs.fzzy_config.annotations.TomlHeaderComment;
import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config_test.test.TestConfigImpl3;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

@TomlHeaderComment(text = "goes here")
@Translation(prefix = "java.prefix")
public class JavaTestConfig extends Config {

    public JavaTestConfig(Identifier id) {super(id, "test");}

    public JavaTestConfig() {
        super(new Identifier("fzzy_config_test", "java_config"), "test");
    }

    public int anInt = 4;
    public boolean aBoolean = true;

    public ValidatedDouble validatedDouble = new ValidatedDouble(1.0, 1.0, 0.0);

    public TestEnum testEnum = TestEnum.C;

    public enum TestEnum implements EnumTranslatable {
        A,
        B,
        C;

        @NotNull
        @Override
        public String prefix() {
            return "my_mod.test_enum";
        }
    }
}