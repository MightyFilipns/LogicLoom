package com.mightyfilipns.logicloom.Routing;

import com.mightyfilipns.logicloom.Placment.Placer;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class VerticalBuilder
{
    public static final int NO_UPDATE = 2 | 816;

    public static void BuildUpwards(ServerWorld w, BlockPos down, BlockPos upper)
    {
        var starty = down.getY() + Placer.Y_MAX_CELL_SIZE;
        var ylevel = (upper.getY() - starty) / 2;
        w.setBlockState(down, Blocks.LIME_WOOL.getDefaultState(), NO_UPDATE);
        if(ylevel % 2 == 1)
        {
            var d2 = down.add(0, 0, 1);
            w.setBlockState(d2, Blocks.REDSTONE_WALL_TORCH.getDefaultState().with(WallRedstoneTorchBlock.FACING, Direction.SOUTH));
            w.setBlockState(d2.add(0, 1, 0), Blocks.LIME_WOOL.getDefaultState(), NO_UPDATE);
            w.setBlockState(down.add(0, 1, 0), Blocks.REDSTONE_WALL_TORCH.getDefaultState().with(WallRedstoneTorchBlock.FACING, Direction.NORTH));
        }
        else
        {
            w.setBlockState(down.add(0, 1, 0), Blocks.REDSTONE_TORCH.getDefaultState());
        }

        int ry = down.getY() + 2;
        for (int i = 0; i <= ylevel + 2; i++)
        {
            w.setBlockState(down.withY(ry + i * 2), Blocks.LIME_WOOL.getDefaultState(), NO_UPDATE);
            w.setBlockState(down.withY(ry + i * 2 + 1), Blocks.REDSTONE_TORCH.getDefaultState());
        }
    }
    public static void BuildDownwards(ServerWorld w, BlockPos down, BlockPos upper)
    {
        boolean side = true;
        for (int i = upper.getY(); i > down.getY(); i -= 2)
        {
            var tpos = side ? down.add(1,0,0) : down;
            var wpos = !side ? down.add(1,0,0) : down;

            w.setBlockState(wpos.withY(i), Blocks.LIME_WOOL.getDefaultState());
            w.setBlockState(tpos.withY(i), Blocks.REDSTONE_WALL_TORCH.getDefaultState().with(WallRedstoneTorchBlock.FACING, side ? Direction.EAST : Direction.WEST));
            w.setBlockState(tpos.withY(i - 1), Blocks.REDSTONE_WIRE.getDefaultState(), NO_UPDATE);
            side = !side;
        }
        var starty = down.getY() + Placer.Y_MAX_CELL_SIZE;
        var ylevel = (upper.getY() - starty) / 2;
        if (ylevel % 2 == 0)
        {
            w.setBlockState(down, Blocks.LIME_WOOL.getDefaultState());
            w.setBlockState(down.add(1,0,0), Blocks.REDSTONE_WIRE.getDefaultState());
        }
        if(ylevel % 2 == 1)
        {
            w.setBlockState(down.add(1,0,0), Blocks.LIME_WOOL.getDefaultState());
            w.setBlockState(down, Blocks.REDSTONE_WIRE.getDefaultState());

            w.setBlockState(down.add(1,2,0), Blocks.AIR.getDefaultState());
            w.setBlockState(down.add(0,1,-1), Blocks.LIME_WOOL.getDefaultState());
            w.setBlockState(down.add(1,1,-1), Blocks.LIME_WOOL.getDefaultState());
            w.setBlockState(down.add(0,2,-1), Blocks.REDSTONE_WIRE.getDefaultState());
            w.setBlockState(down.add(1,2,-1), Blocks.REDSTONE_WIRE.getDefaultState());
        }
    }
}
