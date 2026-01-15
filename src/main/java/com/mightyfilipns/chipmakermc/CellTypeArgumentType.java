package com.mightyfilipns.chipmakermc;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.StringIdentifiable;

public class CellTypeArgumentType extends net.minecraft.command.argument.EnumArgumentType<CellType>
{
    public static final Codec<CellType> CODEC = StringIdentifiable.createCodec(CellType::values);
    private CellTypeArgumentType() {
        super(CODEC, CellType::values);
    }

    public static EnumArgumentType<CellType> CellType() {
        return new CellTypeArgumentType();
    }

    public static CellType getCellType(CommandContext<ServerCommandSource> context, String id) {
        return context.getArgument(id, CellType.class);
    }
}
