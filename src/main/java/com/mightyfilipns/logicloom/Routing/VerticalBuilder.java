package com.mightyfilipns.logicloom.Routing;

import com.mightyfilipns.logicloom.Placment.Placer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;

public class VerticalBuilder
{
    public static final int NO_UPDATE = 2 | 816;

    public static void BuildUpwards(ServerLevel w, BlockPos down, BlockPos upper)
    {
        var starty = down.getY() + Placer.Y_MAX_CELL_SIZE;
        var ylevel = (upper.getY() - starty) / 2;
        w.setBlock(down, Blocks.WOOL.lime().defaultBlockState(), NO_UPDATE);
        if(ylevel % 2 == 1)
        {
            var d2 = down.offset(0, 0, 1);
            w.setBlockAndUpdate(d2, Blocks.REDSTONE_WALL_TORCH.defaultBlockState().setValue(RedstoneWallTorchBlock.FACING, Direction.SOUTH));
            w.setBlock(d2.offset(0, 1, 0), Blocks.WOOL.lime().defaultBlockState(), NO_UPDATE);
            w.setBlockAndUpdate(down.offset(0, 1, 0), Blocks.REDSTONE_WALL_TORCH.defaultBlockState().setValue(RedstoneWallTorchBlock.FACING, Direction.NORTH));
        }
        else
        {
            w.setBlockAndUpdate(down.offset(0, 1, 0), Blocks.REDSTONE_TORCH.defaultBlockState());
        }

        int ry = down.getY() + 2;
        for (int i = 0; i <= ylevel + 2; i++)
        {
            w.setBlock(down.atY(ry + i * 2), Blocks.WOOL.lime().defaultBlockState(), NO_UPDATE);
            w.setBlockAndUpdate(down.atY(ry + i * 2 + 1), Blocks.REDSTONE_TORCH.defaultBlockState());
        }
    }
    public static void BuildDownwards(ServerLevel w, BlockPos down, BlockPos upper)
    {
        boolean side = true;
        for (int i = upper.getY(); i > down.getY(); i -= 2)
        {
            var tpos = side ? down.offset(1,0,0) : down;
            var wpos = !side ? down.offset(1,0,0) : down;

            w.setBlockAndUpdate(wpos.atY(i), Blocks.WOOL.lime().defaultBlockState());
            w.setBlockAndUpdate(tpos.atY(i), Blocks.REDSTONE_WALL_TORCH.defaultBlockState().setValue(RedstoneWallTorchBlock.FACING, side ? Direction.EAST : Direction.WEST));
            w.setBlock(tpos.atY(i - 1), Blocks.REDSTONE_WIRE.defaultBlockState(), NO_UPDATE);
            side = !side;
        }
        var starty = down.getY() + Placer.Y_MAX_CELL_SIZE;
        var ylevel = (upper.getY() - starty) / 2;
        if (ylevel % 2 == 0)
        {
            w.setBlockAndUpdate(down, Blocks.WOOL.lime().defaultBlockState());
            w.setBlockAndUpdate(down.offset(1,0,0), Blocks.REDSTONE_WIRE.defaultBlockState());
        }
        if(ylevel % 2 == 1)
        {
            w.setBlockAndUpdate(down.offset(1,0,0), Blocks.WOOL.lime().defaultBlockState());
            w.setBlockAndUpdate(down, Blocks.REDSTONE_WIRE.defaultBlockState());

            w.setBlockAndUpdate(down.offset(1,2,0), Blocks.AIR.defaultBlockState());
            w.setBlockAndUpdate(down.offset(0,1,-1), Blocks.WOOL.lime().defaultBlockState());
            w.setBlockAndUpdate(down.offset(1,1,-1), Blocks.WOOL.lime().defaultBlockState());
            w.setBlockAndUpdate(down.offset(0,2,-1), Blocks.REDSTONE_WIRE.defaultBlockState());
            w.setBlockAndUpdate(down.offset(1,2,-1), Blocks.REDSTONE_WIRE.defaultBlockState());
        }
    }
}
