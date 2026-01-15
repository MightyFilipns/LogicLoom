package com.mightyfilipns.chipmakermc.Routing;

import com.mightyfilipns.chipmakermc.CellInfo;
import com.mightyfilipns.chipmakermc.CellType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Map;

public class RoutingPrep
{
    public static void SetupCellPorts(CommandContext<ServerCommandSource> context, Map<CellInfo, BlockPos> cellmap)
    {
        var w = context.getSource().getWorld();
        for (Map.Entry<CellInfo, BlockPos> entry : cellmap.entrySet())
        {
            var output = entry.getValue().add(1, 0, 0);
            BuildOutput(output, w);
            var in1 = entry.getValue().add(0, 0, 5);
            var in2 = entry.getValue().add(2, 0, 5);
            if (entry.getKey().type != CellType.NOT)
            {
                BuildInput(in1, w);
                BuildInput(in2, w);
            }
            else
            {
                BuildInput(in1, w);
            }
        }
    }

    public static void BuildOutput(BlockPos p, World w)
    {
        w.setBlockState(p, Blocks.STICKY_PISTON.getDefaultState().with(FacingBlock.FACING, Direction.UP));
        w.setBlockState(p.add(0,1,0), Blocks.SLIME_BLOCK.getDefaultState());
        w.setBlockState(p.add(0,2,0), Blocks.SLIME_BLOCK.getDefaultState());
        //w.setBlockState(p.add(0,2,1), Blocks.SLIME_BLOCK.getDefaultState());
        w.setBlockState(p.add(0,3,0), Blocks.REDSTONE_BLOCK.getDefaultState());
        w.setBlockState(p.add(1,3,0), Blocks.OBSIDIAN.getDefaultState());
        w.setBlockState(p.add(-1,3,0), Blocks.OBSIDIAN.getDefaultState());
        w.setBlockState(p.add(1,4,0), Blocks.REDSTONE_WIRE.getDefaultState());
        w.setBlockState(p.add(-1,4,0), Blocks.REDSTONE_WIRE.getDefaultState());
    }

    public static void BuildInput(BlockPos p, World w)
    {
        w.setBlockState(p.add(0,1,0), Blocks.REDSTONE_BLOCK.getDefaultState());
        w.setBlockState(p.add(0,2,0), Blocks.STICKY_PISTON.getDefaultState().with(FacingBlock.FACING, Direction.DOWN));
        w.setBlockState(p.add(0,3,0), Blocks.LIME_WOOL.getDefaultState());
        w.setBlockState(p.add(0,4,0), Blocks.REDSTONE_WIRE.getDefaultState());
    }
}
