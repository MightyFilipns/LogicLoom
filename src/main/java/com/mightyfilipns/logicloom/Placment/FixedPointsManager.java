package com.mightyfilipns.logicloom.Placment;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class FixedPointsManager
{
    public record FixedPoint(int x, int z, double strength)
    {
        @Override
        public @NonNull String toString()
        {
            if(Placer.start_pos == null)
            {
                return String.format("Fixed point at abs: (/, /) relative (%d, %d) strength: %f", x, z, strength);
            }
            else
            {
                return String.format("Fixed point at abs (%d, %d) relative (%d, %d) strength: %f", Placer.start_pos.getX() + x, Placer.start_pos.getZ() + z, x, z, strength);
            }
        }
    }

    private static final List<FixedPoint> points = new ArrayList<>();

    public static int AddPointAbs(CommandContext<ServerCommandSource> context)
    {
        if(Placer.start_pos == null)
        {
            context.getSource().sendError(Text.literal("You must set the start_pos using /logicloom set_start_pos before using this command"));
            return 0;
        }
        BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");
        double st = DoubleArgumentType.getDouble(context, "strength");
        FixedPoint pt = new FixedPoint(pos.getX() - Placer.start_pos.getX(), pos.getZ() - Placer.start_pos.getZ(), st);
        points.add(pt);
        context.getSource().sendMessage(Text.literal("Added " + pt));

        return 1;
    }

    public static int AddPointRel(CommandContext<ServerCommandSource> context)
    {
        int x = IntegerArgumentType.getInteger(context, "x");
        int z = IntegerArgumentType.getInteger(context, "z");
        double st = DoubleArgumentType.getDouble(context, "strength");
        FixedPoint pt = new FixedPoint(x, z, st);
        points.add(pt);
        context.getSource().sendMessage(Text.literal("Added " + pt));

        return 1;
    }

    public static int ListPoints(CommandContext<ServerCommandSource> context)
    {
        if(points.isEmpty())
        {
            context.getSource().sendMessage(Text.literal("No fixed points"));
            return 1;
        }

        context.getSource().sendMessage(Text.literal("Fixed points: "));

        for (int i = 0; i < points.size(); i++)
        {
            var p = points.get(i);
            context.getSource().sendMessage(Text.literal(String.format("[%d] - %s ", i, p)));
        }

        return 1;
    }

    public static int RemovePoint(CommandContext<ServerCommandSource> context)
    {
        int index = IntegerArgumentType.getInteger(context, "index");

        if (index < 0 || index >= points.size())
        {
            context.getSource().sendError(Text.literal(String.format("Index %d is out of bounds", index)));
            return 0;
        }

        var rp = points.remove(index);

        context.getSource().sendMessage(Text.literal("Removed " + rp));

        return 1;
    }

    public static List<FixedPoint> GetFixedPointsList()
    {
        return points;
    }

    public static int ClearAllPoints(CommandContext<ServerCommandSource> context)
    {
        points.clear();
        context.getSource().sendMessage(Text.literal("Removed all fixed points"));

        return 1;
    }
}
