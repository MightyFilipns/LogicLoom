package com.mightyfilipns.chipmakermc.Routing;

import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class VerticalBuilder
{
    public static void BuildUpwards(ServerWorld w, BlockPos down, BlockPos upper, List<HashSet<Pair<Integer, Integer>>> occupied_map, BlockPos start)
    {
        List<Integer> piston_pos = new ArrayList<>();
        int y = start.getY() + 3;

        int sanity_check = 0;
        for (int i = down.getY(); i <= upper.getY(); i += 14)
        {
            var v = ValidPosForBlock(i - 2, y, down, occupied_map);
            while(!v)
            {
                i -= 2;
                v = ValidPosForBlock(i - 2, y, down, occupied_map);
                sanity_check++;
                if(i + 2 < down.getY() || sanity_check > 2000)
                {
                    throw new RuntimeException("Failed to build upwards connector at: " + down);
                }
            }
            piston_pos.add(i);
            System.out.println("valid: " + v + " i: " + i);
        }
        if(upper.getY() - piston_pos.getLast() > 12)
        {
            piston_pos.add(upper.getY() - 2);
        }

        for (Integer pistonPo : piston_pos)
        {
            w.setBlockState(down.withY(pistonPo), Blocks.STICKY_PISTON.getDefaultState().with(FacingBlock.FACING, Direction.UP));
        }

        for (int i = 1; i < piston_pos.size(); i++)
        {
            var y1 = piston_pos.get(i - 1) + 1;
            var y2 = piston_pos.get(i) - 3;
            for (BlockPos blockPos : BlockPos.iterate(down.withY(y1), down.withY(y2)))
            {
                w.setBlockState(blockPos, Blocks.SLIME_BLOCK.getDefaultState());
            }
            w.setBlockState(down.withY(piston_pos.get(i) - 2), Blocks.REDSTONE_BLOCK.getDefaultState());
        }

        var p1 = down.withY(piston_pos.getLast() + 1);
        var p2 = upper.add(0, -1,0);
        for (BlockPos blockPos : BlockPos.iterate(p1, p2))
        {
            w.setBlockState(blockPos, Blocks.SLIME_BLOCK.getDefaultState());
        }
        w.setBlockState(upper, Blocks.REDSTONE_BLOCK.getDefaultState());
        for (int i = y; i <= upper.getY(); i += 2)
        {
            var ps = LeeRouter.GetSurroundingPoints(Pair.of(down.getX(), down.getZ()));

            int dy = i - y;
            if (dy < 0)
                continue;
            var ind = dy/2;
            var map = occupied_map.get(ind);
            for (Pair<Integer, Integer> a : ps.stream().filter(map::contains).toList())
            {
                var p = new BlockPos(a.getLeft(), i, a.getRight());
                if (!w.getBlockState(p).isAir())
                {
                    w.setBlockState(p, Blocks.OBSIDIAN.getDefaultState());
                }
            }
        }
    }

    public static void BuildDownwards(ServerWorld w, BlockPos down, BlockPos upper, List<HashSet<Pair<Integer, Integer>>> occupied_map, BlockPos start)
    {
        List<Integer> piston_pos = new ArrayList<>();
        int y = start.getY() + 3;

        piston_pos.add(upper.getY());
        int sanity_check = 0;
        for (int i = upper.getY() - 13; i >= down.getY() + 3; i -= 13)
        {
            var v = ValidPosForBlock(i + 2, y, down, occupied_map);
            v &= ValidPosForBlock(i + 1, y, down, occupied_map);
            while(!v)
            {
                i += 2;
                v = ValidPosForBlock(i + 2, y, down, occupied_map);
                v &= ValidPosForBlock(i + 1, y, down, occupied_map);
                sanity_check++;
                if(i > upper.getY() || sanity_check > 2000)
                {
                    throw new RuntimeException("Failed to build downwards connector at: " + down);
                }
            }
            piston_pos.add(i);
            System.out.println("valid: " + v + " i: " + i);
        }

        if(piston_pos.getLast() - down.getY() > 12)
        {
            piston_pos.add(down.getY() + 3);
        }

        for (Integer pistonPo : piston_pos)
        {
            w.setBlockState(down.withY(pistonPo), Blocks.STICKY_PISTON.getDefaultState().with(FacingBlock.FACING, Direction.DOWN));
        }


        for (int i = 1; i < piston_pos.size(); i++)
        {
            var y1 = piston_pos.get(i - 1) - 1;
            var y2 = piston_pos.get(i) + 4;
            for (BlockPos blockPos : BlockPos.iterate(down.withY(y1), down.withY(y2)))
            {
                w.setBlockState(blockPos, Blocks.SLIME_BLOCK.getDefaultState(), 2 | 816);
            }
            w.setBlockState(down.withY(piston_pos.get(i) + 1), Blocks.REDSTONE_WIRE.getDefaultState());
            w.setBlockState(down.withY(piston_pos.get(i) + 3), Blocks.REDSTONE_BLOCK.getDefaultState());
        }

        var p1 = down.withY(piston_pos.getLast() - 1);
        var p2 = down.add(0, 2,0);
        for (BlockPos blockPos : BlockPos.iterate(p1, p2))
        {
            w.setBlockState(blockPos, Blocks.SLIME_BLOCK.getDefaultState(), 2 | 816);
        }
        w.setBlockState(down.add(0, 1, 0), Blocks.REDSTONE_BLOCK.getDefaultState());

        for (int i = y; i <= upper.getY(); i += 2)
        {
            var ps = LeeRouter.GetSurroundingPoints(Pair.of(down.getX(), down.getZ()));

            int dy = i - y;
            if (dy < 0)
                continue;
            var ind = dy/2;
            var map = occupied_map.get(ind);
            for (Pair<Integer, Integer> a : ps.stream().filter(map::contains).toList())
            {
                var p = new BlockPos(a.getLeft(), i, a.getRight());
                if (!w.getBlockState(p).isAir())
                {
                    w.setBlockState(p, Blocks.OBSIDIAN.getDefaultState());
                }
            }
        }
    }

    static boolean ValidPosForBlock(int ypos, int wire_start, BlockPos pos, List<HashSet<Pair<Integer, Integer>>> occupied_map)
    {
        int dy = ypos - wire_start;
        if (dy < 0)
            return true;
        var ind = dy/2;
        var map = occupied_map.get(ind);
        var ps = LeeRouter.GetSurroundingPoints(Pair.of(pos.getX(), pos.getZ()));
        return ps.stream().noneMatch(map::contains);
    }
}
