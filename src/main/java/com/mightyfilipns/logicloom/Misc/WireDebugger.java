package com.mightyfilipns.logicloom.Misc;

import com.mightyfilipns.logicloom.JsonLoader.PortDirection;
import com.mightyfilipns.logicloom.Placment.Placer;
import com.mightyfilipns.logicloom.Routing.HyperGraphNet;
import com.mightyfilipns.logicloom.Routing.Misc;
import com.mightyfilipns.logicloom.Routing.Router;
import com.mightyfilipns.logicloom.Routing.TwoPinNet;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

public class WireDebugger
{
    public enum ForceWireMode
    {
        ON,
        OFF,
        NORMAL
    }

    public static int ForcePowerWires(CommandContext<CommandSourceStack> context)
    {
        if(Router.cached_hy == null || Router.cached_tpn == null)
        {
            context.getSource().sendFailure(Component.literal("You must place wires using /logicloom route before using this command"));
            return 0;
        }
        if (Misc.CheckStartPos(context))
        {
            return 0;
        }

        var starty = Placer.start_pos.getY() + Placer.Y_MAX_CELL_SIZE;
        var w = context.getSource().getLevel();
        var ty = ForceWireMode.valueOf(StringArgumentType.getString(context, "type"));
        var blk = switch (ty)
        {
            case ON -> Blocks.REDSTONE_BLOCK;
            case OFF -> Blocks.AIR;
            case NORMAL -> Blocks.REDSTONE_WIRE;
        };
        context.getSource().sendSystemMessage(Component.nullToEmpty("Force setting all wires Mode: " + ty));
        for (var hy : Router.cached_hy)
        {
            w.setBlockAndUpdate(hy.all_points.get(hy.allpoints_pos).atY(starty + hy.y_pos * 2 + 1), blk.defaultBlockState());
        }
        for (var hy : Router.cached_tpn)
        {
            w.setBlockAndUpdate((hy.p1dir == PortDirection.Output ? hy.p1 : hy.p2).atY(starty + hy.y_pos * 2 + 1), blk.defaultBlockState());
        }
        return 1;
    }

    public static void CheckWires(CommandContext<CommandSourceStack> context)
    {
        var w = context.getSource().getLevel();
        int wire_start_y = Placer.start_pos.getY() + Placer.Y_MAX_CELL_SIZE;
        boolean found = false;
        for (HyperGraphNet hp : Router.cached_hy)
        {
            var wire_y = wire_start_y + hp.y_pos * 2 + 1;

            var outpos = hp.all_points.get(hp.allpoints_pos).atY(wire_y);
            var outpwr = CheckPower(w, outpos);
            for (BlockPos allPoint : hp.pin_port_pos)
            {
                BlockPos inpos = allPoint.atY(wire_y);
                var pointpwr = CheckPower(w, inpos);
                if (pointpwr != outpwr)
                {
                    found = true;
                    context.getSource().sendFailure(Component.literal(String.format("BW: PwrOut: %s %s PwrIn: %s %s", VCDHandler.XZ_tostring(outpos), outpwr, VCDHandler.XZ_tostring(inpos), pointpwr)));
                }
            }
        }
        for (TwoPinNet hp : Router.cached_tpn)
        {
            var wire_y = wire_start_y + hp.y_pos * 2 + 1;

            var outpos = hp.p1dir == PortDirection.Output ? hp.p1 : hp.p2;
            var inpos = hp.p1dir == PortDirection.Output ? hp.p2 : hp.p1;
            var pwr1 = CheckPower(w, outpos.atY(wire_y));
            var pwr2 = CheckPower(w, inpos.atY(wire_y));

            if (pwr1 != pwr2)
            {
                found = true;
                context.getSource().sendFailure(Component.literal(String.format("BW: PwrOut: %s %s PwrIn: %s %s", VCDHandler.XZ_tostring(outpos), pwr1, VCDHandler.XZ_tostring(inpos), pwr2)));
            }
        }
        if (!found)
        {
            context.getSource().sendFailure(Component.literal("No bad wires found"));
        }
    }

    private static boolean CheckPower(ServerLevel w, BlockPos outpos)
    {
        if(w.getBlockState(outpos).getBlock() == Blocks.REDSTONE_BLOCK)
            return true;
        if(w.getBlockState(outpos).getBlock() == Blocks.AIR)
            return false;
        return w.getBlockState(outpos).getValue(RedStoneWireBlock.POWER) != 0;
    }
}
