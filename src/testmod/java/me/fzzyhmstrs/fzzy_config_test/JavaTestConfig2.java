/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config_test;

import me.fzzyhmstrs.fzzy_config.api.SaveType;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedRegistryType;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class JavaTestConfig2 extends JavaTestConfig {

	public JavaTestConfig2() {super(Identifier.of("fzzy_config_test","java_config_2"));}

	@NotNull
	@Override
	public SaveType saveType() {
		return SaveType.SEPARATE;
	}

	public int intFrom2 = 2;

	public ValidatedList<Block> blocks = new ValidatedList<>(getList(), ValidatedRegistryType.of(Registries.BLOCK));

	private List<Block> getList() {
		List<Block> list = new ArrayList<>();
		list.add(Blocks.ANDESITE);
		list.add(Blocks.COBBLESTONE);
		list.add(Blocks.CANDLE);
		return list;
	}

	public ValidatedFloat aFloat = new ValidatedFloat(1, 1, 0.3F);

	@ValidatedFloat.Restrict(min = 0.3F, max = 1)
	public float aFloat2 = 1;

	public ValidatedFloat aFloat3 = new ValidatedFloat(1, 1, 0.3F);

	public ValidatedDouble aDouble = new ValidatedDouble(1, 1, 0.3F);


}