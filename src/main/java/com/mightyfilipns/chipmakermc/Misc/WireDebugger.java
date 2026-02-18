package com.mightyfilipns.chipmakermc.Misc;

import com.mightyfilipns.chipmakermc.JsonLoader.PortDirection;
import com.mightyfilipns.chipmakermc.Placment.Placer;
import com.mightyfilipns.chipmakermc.Routing.HyperGraphNet;
import com.mightyfilipns.chipmakermc.Routing.Misc;
import com.mightyfilipns.chipmakermc.Routing.Router;
import com.mightyfilipns.chipmakermc.Routing.TwoPinNet;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class WireDebugger
{
    public enum ForceWireMode
    {
        ON,
        OFF,
        NORMAL
    }

    public static int ForcePowerWires(CommandContext<ServerCommandSource> context)
    {
        if(Router.cached_hy == null || Router.cached_tpn == null)
        {
            context.getSource().sendError(Text.literal("You must place wires using /chipmaker route before using this command"));
            return 0;
        }
        if (Misc.CheckStartPos(context))
        {
            return 0;
        }

        var starty = Placer.start_pos.getY() + Placer.Y_MAX_CELL_SIZE;
        var w = context.getSource().getWorld();
        var ty = ForceWireMode.valueOf(StringArgumentType.getString(context, "type"));
        var blk = switch (ty)
        {
            case ON -> Blocks.REDSTONE_BLOCK;
            case OFF -> Blocks.AIR;
            case NORMAL -> Blocks.REDSTONE_WIRE;
        };
        context.getSource().sendMessage(Text.of("Force setting all wires Mode: " + ty));
        for (var hy : Router.cached_hy)
        {
            w.setBlockState(hy.all_points.get(hy.allpoints_pos).withY(starty + hy.y_pos * 2 + 1), blk.getDefaultState());
        }
        for (var hy : Router.cached_tpn)
        {
            w.setBlockState((hy.p1dir == PortDirection.Output ? hy.p1 : hy.p2).withY(starty + hy.y_pos * 2 + 1), blk.getDefaultState());
        }
        return 1;
    }

    public static void CheckWires(CommandContext<ServerCommandSource> context)
    {
        var w = context.getSource().getWorld();
        int wire_start_y = Placer.start_pos.getY() + Placer.Y_MAX_CELL_SIZE;
        boolean found = false;
        for (HyperGraphNet hp : Router.cached_hy)
        {
            var wire_y = wire_start_y + hp.y_pos * 2 + 1;

            var outpos = hp.all_points.get(hp.allpoints_pos).withY(wire_y);
            var outpwr = CheckPower(w, outpos);
            for (BlockPos allPoint : hp.pin_port_pos)
            {
                BlockPos inpos = allPoint.withY(wire_y);
                var pointpwr = CheckPower(w, inpos);
                if (pointpwr != outpwr)
                {
                    found = true;
                    context.getSource().sendError(Text.literal(String.format("BW: PwrOut: %s %s PwrIn: %s %s", VCDHandler.XZ_tostring(outpos), outpwr, VCDHandler.XZ_tostring(inpos), pointpwr)));
                }
            }
        }
        for (TwoPinNet hp : Router.cached_tpn)
        {
            var wire_y = wire_start_y + hp.y_pos * 2 + 1;

            var outpos = hp.p1dir == PortDirection.Output ? hp.p1 : hp.p2;
            var inpos = hp.p1dir == PortDirection.Output ? hp.p2 : hp.p1;
            var pwr1 = CheckPower(w, outpos.withY(wire_y));
            var pwr2 = CheckPower(w, inpos.withY(wire_y));

            if (pwr1 != pwr2)
            {
                found = true;
                context.getSource().sendError(Text.literal(String.format("BW: PwrOut: %s %s PwrIn: %s %s", VCDHandler.XZ_tostring(outpos), pwr1, VCDHandler.XZ_tostring(inpos), pwr2)));
            }
        }
        if (!found)
        {
            context.getSource().sendError(Text.literal("No bad wires found"));
        }
    }

    private static boolean CheckPower(ServerWorld w, BlockPos outpos)
    {
        if(w.getBlockState(outpos).getBlock() == Blocks.REDSTONE_BLOCK)
            return true;
        if(w.getBlockState(outpos).getBlock() == Blocks.AIR)
            return false;
        return w.getBlockState(outpos).get(RedstoneWireBlock.POWER) != 0;
    }
}
