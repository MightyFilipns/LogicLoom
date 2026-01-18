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
    public static void BuildUpwards(ServerWorld w, BlockPos down, BlockPos upper, List<HashSet<Pair<Integer, Integer>>> occupied_map, BlockPos start, List<BlockPos> rep_map)
    {
        List<Integer> piston_pos = new ArrayList<>();
        int y = start.getY() + 3;

        int sanity_check = 0;
        for (int i = down.getY(); i <= upper.getY() - 2; i += 14)
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

    public static void BuildDownwards(ServerWorld w, BlockPos down, BlockPos upper, List<HashSet<Pair<Integer, Integer>>> occupied_map, BlockPos start, List<BlockPos> rep_map)
    {
        List<Integer> piston_pos = new ArrayList<>();
        int y = start.getY() + 3;

        piston_pos.add(upper.getY());
        int sanity_check = 0;

        for (int i = upper.getY() - 13; i >= down.getY() + 3; i -= 13)
        {
            var r = PistonPosFinder(i, y, down, upper, occupied_map, start, rep_map, sanity_check, piston_pos);
            i = r.getLeft();
            sanity_check = r.getRight();

            /*
            var v = CheckDownwardsRedstonePos(i + 2, y, down, occupied_map);
            v &= ValidPosForBlock(i + 1, y, down, occupied_map);
            v &= CheckDownwardsRedstonePos(i + 3, y, down, occupied_map);
            boolean do_rep_res = false;
            while(!v)
            {
                i += 1;
                var v1 = CheckDownwardsRedstonePos(i + 2, y, down, occupied_map);
                var v2 = ValidPosForBlock(i + 1, y, down, occupied_map);
                var v3 = CheckDownwardsRedstonePos(i + 3, y, down, occupied_map);

                v = v1 & v2 & v3;

                if(do_rep_res)
                {
                    List<BlockPos> tm = new ArrayList<>();
                    if (!v1)
                        v1 = ReserveRepeater(i + 2, y, down, occupied_map, tm);
                    if (!v2)
                        v2 = ReserveRepeater(i + 1, y, down, occupied_map, tm);
                    if (!v3)
                        v3 = ReserveRepeater(i + 3, y, down, occupied_map, tm);
                    v = v1 & v2 & v3;
                    if(v)
                    {
                        if(i >= piston_pos.getLast() - 4)
                        {
                            if(piston_pos.size() == 1)
                            {
                                throw new RuntimeException("Failed to build downwards connector at: " + down + " Could not find position for piston");
                            }
                            piston_pos.set(piston_pos.size() - 1, piston_pos.getLast() - 1);
                        }
                        else
                        {
                            rep_map.addAll(tm);
                            do_rep_res = false;
                        }
                    }
                }

                sanity_check++;

                if(i >= piston_pos.getLast() - 4)
                {
                    v = false;
                    i = piston_pos.getLast() - 10;
                    do_rep_res = true;
                }

                if(sanity_check > 2000)
                {
                    throw new RuntimeException("Failed to build downwards connector at: " + down + " Sanity check triggered");
                }
                System.out.println("2 valid: " + v + " i: " + i);
            }
            if(piston_pos.getLast() <= i)
            {
                throw new RuntimeException("Failed to build downwards connector at: " + down + " - Cant put piston before or at the same height as the previous one");
            }
            piston_pos.add(i);
            System.out.println("valid: " + v + " i: " + i);*/
        }

        if(piston_pos.getLast() - down.getY() > 12)
        {
            int i = down.getY() + 3;
            var r = PistonPosFinder(i, y, down, upper, occupied_map, start, rep_map, sanity_check, piston_pos);
        }

        for (Integer pistonPo : piston_pos)
        {
            w.setBlockState(down.withY(pistonPo), Blocks.STICKY_PISTON.getDefaultState().with(FacingBlock.FACING, Direction.DOWN));
        }

        var srrp = LeeRouter.GetSurroundingPoints(Pair.of(down.getX(), down.getZ()));
        for (Integer pistonPo : piston_pos)
        {
            var blkp = srrp.stream().map(a -> new BlockPos(a.getLeft(), pistonPo, a.getRight())).toList();
            for (BlockPos blockPos : blkp)
            {
                if(!w.getBlockState(blockPos.add(0, -1, 0)).isAir())
                {
                    w.setBlockState(blockPos.add(0,1,0), Blocks.OBSIDIAN.getDefaultState());
                }
            }
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

    public static Pair<Integer, Integer> PistonPosFinder(int i, int y, BlockPos down, BlockPos upper, List<HashSet<Pair<Integer, Integer>>> occupied_map, BlockPos start, List<BlockPos> rep_map, int sanity_check, List<Integer> piston_pos)
    {
        var v = CheckDownwardsRedstonePos(i + 2, y, down, occupied_map);
        v &= ValidPosForBlock(i + 1, y, down, occupied_map);
        v &= CheckDownwardsRedstonePos(i + 3, y, down, occupied_map);
        boolean do_rep_res = false;
        while(!v)
        {
            i += 1;
            var v1 = CheckDownwardsRedstonePos(i + 2, y, down, occupied_map);
            var v2 = ValidPosForBlock(i + 1, y, down, occupied_map);
            var v3 = CheckDownwardsRedstonePos(i + 3, y, down, occupied_map);

            v = v1 & v2 & v3;

            if(do_rep_res)
            {
                List<BlockPos> tm = new ArrayList<>();
                if (!v1)
                    v1 = ReserveRepeater(i + 2, y, down, occupied_map, tm);
                if (!v2)
                    v2 = ReserveRepeater(i + 1, y, down, occupied_map, tm);
                if (!v3)
                    v3 = ReserveRepeater(i + 3, y, down, occupied_map, tm);
                v = v1 & v2 & v3;
                if(v)
                {
                    if(i >= piston_pos.getLast() - 4)
                    {
                        if(piston_pos.size() == 1)
                        {
                            throw new RuntimeException("Failed to build downwards connector at: " + down + " Could not find position for piston");
                        }
                        /// Blindly move the previous piston on block up. Could cause interference with nearby wires
                        /// However its very unlikely that this code get triggered and that piston causes problems so we ignore this problem for now
                        // TODO: fix
                        piston_pos.set(piston_pos.size() - 1, piston_pos.getLast() - 1);
                    }
                    else
                    {
                        rep_map.addAll(tm);
                        do_rep_res = false;
                    }
                }
            }

            sanity_check++;

            if(i >= piston_pos.getLast() - 4)
            {
                v = false;
                i = piston_pos.getLast() - 10;
                do_rep_res = true;
            }

            if(sanity_check > 2000)
            {
                throw new RuntimeException("Failed to build downwards connector at: " + down + " Sanity check triggered");
            }
            System.out.println("2 valid: " + v + " i: " + i);
        }
        if(piston_pos.getLast() <= i)
        {
            throw new RuntimeException("Failed to build downwards connector at: " + down + " - Cant put piston before or at the same height as the previous one");
        }
        piston_pos.add(i);
        System.out.println("valid: " + v + " i: " + i);
        return Pair.of(i, sanity_check);
    }

    static boolean ReserveRepeater(int ypos, int wire_start, BlockPos pos, List<HashSet<Pair<Integer, Integer>>> occupied_map, List<BlockPos> repeater_map)
    {
        int dy = ypos - wire_start;
        if (dy < 0)
            return true;
        var ind = dy/2;
        var map = occupied_map.get(ind);
        var ps = LeeRouter.GetSurroundingPoints(Pair.of(pos.getX(), pos.getZ()));
        List<Pair<Integer, Integer>> to_check = ps.stream().filter(map::contains).toList();
        if(to_check.stream().allMatch(a -> CheckRepeater(a, map)))
        {
            repeater_map.addAll(to_check.stream().map(a -> new BlockPos(a.getLeft(), ypos, a.getRight())).toList());
            return true;
        }
        else
            return false;

    }

    private static boolean CheckRepeater(Pair<Integer, Integer> to_check, HashSet<Pair<Integer, Integer>> occupied_map)
    {
        var ps = LeeRouter.GetSurroundingPoints(to_check);
        List<Pair<Integer, Integer>> tc2 = ps.stream().filter(occupied_map::contains).toList();
        if (tc2.size() == 2)
        {
            var p1 = tc2.get(0);
            var p2 = tc2.get(1);
            var dx = p1.getLeft() - p2.getLeft();
            var dy = p1.getRight() - p2.getRight();
            return dx == 0 || dy == 0;
        }
        else
            return false;
    }

    static boolean CheckDownwardsRedstonePos(int ypos, int wire_start, BlockPos pos, List<HashSet<Pair<Integer, Integer>>> occupied_map)
    {
        int dy = ypos - wire_start;
        if (dy < 0)
            return true;
        var mod = dy % 2;
        if(mod == 0)
            return true;
        var ind = dy/2;
        var map = occupied_map.get(ind);
        var ps = LeeRouter.GetSurroundingPoints(Pair.of(pos.getX(), pos.getZ()));
        return ps.stream().noneMatch(map::contains);
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
