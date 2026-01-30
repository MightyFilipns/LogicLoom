package com.mightyfilipns.chipmakermc.Routing;

import com.mightyfilipns.chipmakermc.Placer;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.mightyfilipns.chipmakermc.Routing.Router.g_pistonlist;
import static com.mightyfilipns.chipmakermc.Routing.Router.g_update_pos;

public class VerticalBuilder
{
    public static final int NO_UPDATE = 2 | 816;

    public static void BuildUpwards(ServerWorld w, BlockPos down, BlockPos upper, List<HashSet<Pair<Integer, Integer>>> occupied_map, BlockPos start, List<BlockPos> rep_map)
    {
        var starty = down.getY() + Placer.Y_CELL_SIZE;
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
        for (int i = 0; i <= ylevel; i++)
        {
            w.setBlockState(down.withY(ry + i * 2), Blocks.LIME_WOOL.getDefaultState(), NO_UPDATE);
            w.setBlockState(down.withY(ry + i * 2 + 1), Blocks.REDSTONE_TORCH.getDefaultState());
        }

        /*
        int ey = ry + ylevel * 2;
        if ()
        {
            w.setBlockState(down.withY(ey), Blocks.LIME_WOOL.getDefaultState(), NO_UPDATE);
            w.setBlockState(d2.withY(ey), Blocks.REDSTONE_WALL_TORCH.getDefaultState().with(WallRedstoneTorchBlock.FACING, Direction.SOUTH), NO_UPDATE);
            w.setBlockState(d2.withY(ey + 1),Blocks.LIME_WOOL.getDefaultState(), NO_UPDATE);
            w.setBlockState(down.withY(ey + 1), Blocks.REDSTONE_WALL_TORCH.getDefaultState().with(WallRedstoneTorchBlock.FACING, Direction.NORTH));
        }*/
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
        var starty = down.getY() + Placer.Y_CELL_SIZE;
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

    public static void BuildUpwardsOld(ServerWorld w, BlockPos down, BlockPos upper, List<HashSet<Pair<Integer, Integer>>> occupied_map, BlockPos start, List<BlockPos> rep_map)
    {
        // w.setBlockState(down, Blocks.REDSTONE_WIRE.getDefaultState());
        g_pistonlist.add(upper.add(0, 1, 0));
        boolean powered = false;
        try
        {
            powered = w.getBlockState(down).get(RedstoneWireBlock.POWER) != 0;
        } catch (Exception ignored)
        {
            w.setBlockState(down, Blocks.REDSTONE_WIRE.getDefaultState(), 2 | 816);
        }

        for (int i = down.getY() + 1; i < upper.getY() - 2; i++)
        {
            w.setBlockState(down.withY(i), Blocks.OBSERVER.getDefaultState().with(FacingBlock.FACING, Direction.DOWN), 2 | 816);
        }
        w.setBlockState(upper.add(0,-2,0), Blocks.NOTE_BLOCK.getDefaultState(), 2 | 816);
        w.setBlockState(upper.add(0,-1,0), Blocks.STICKY_PISTON.getDefaultState().with(FacingBlock.FACING, Direction.UP), 2 | 816);

        var pos = powered ? upper : upper.add(0, 1, 0);
        if (!powered)
        {
            w.setBlockState(upper, Blocks.AIR.getDefaultState(), 2 | 816);
        }
        w.setBlockState(pos, Blocks.SLIME_BLOCK.getDefaultState(), 2 | 816);
        w.setBlockState(pos.add(0,1,0), Blocks.REDSTONE_BLOCK.getDefaultState(), 2 | 816);
        if(powered)
        {
            g_update_pos.add(pos.add(0,1,0));
        }

        var ps = LeeRouter.GetSurroundingPoints(Pair.of(down.getX(), down.getZ()));

        int dy = upper.getY() - (start.getY() + Placer.Y_CELL_SIZE);
        if (dy < 0)
            return;
        var ind = dy/2;
        if(ind >= occupied_map.size())
        {
            throw new RuntimeException("err");
        }
        var map = occupied_map.get(ind);
        for (Pair<Integer, Integer> a : ps.stream().filter(map::contains).toList())
        {
            var p = new BlockPos(a.getLeft(), upper.getY(), a.getRight());
            if (!w.getBlockState(p).isAir())
            {
                w.setBlockState(p, Blocks.OBSIDIAN.getDefaultState(), 2 | 816);
            }
        }


    }

    public static void BuildUpwardsOldOld(ServerWorld w, BlockPos down, BlockPos upper, List<HashSet<Pair<Integer, Integer>>> occupied_map, BlockPos start, List<BlockPos> rep_map)
    {
        List<Integer> piston_pos = new ArrayList<>();
        int y = start.getY() + 3;

        int sanity_check = 0;
        for (int i = down.getY(); i <= upper.getY() - 2; i += 14)
        {
            var p = PistonPosFinderUpwards(i, y, down, occupied_map, rep_map, sanity_check, piston_pos);
            i = p.getLeft();
            sanity_check = p.getRight();
        }

        if(upper.getY() - piston_pos.getLast() > 12)
        {
            int i = upper.getY() - 2;
            var p = PistonPosFinderUpwards(i, y, down, occupied_map, rep_map, sanity_check, piston_pos);
        }

        for (Integer pistonPo : piston_pos)
        {
            w.setBlockState(down.withY(pistonPo), Blocks.STICKY_PISTON.getDefaultState().with(FacingBlock.FACING, Direction.UP));
            Router.g_pistonlist.add(down.withY(pistonPo));
        }

        for (int i = 1; i < piston_pos.size(); i++)
        {
            var y1 = piston_pos.get(i - 1) + 1;
            var y2 = piston_pos.get(i) - 3;
            for (BlockPos blockPos : BlockPos.iterate(down.withY(y1), down.withY(y2)))
            {
               w.setBlockState(blockPos, Blocks.SLIME_BLOCK.getDefaultState(), 2 | 816);
            }
            w.setBlockState(down.withY(piston_pos.get(i) - 2), Blocks.REDSTONE_BLOCK.getDefaultState(), 2 | 816);
        }

        var p1 = down.withY(piston_pos.getLast() + 1);
        var p2 = upper.add(0, -1,0);
        for (BlockPos blockPos : BlockPos.iterate(p1, p2))
        {
            w.setBlockState(blockPos, Blocks.SLIME_BLOCK.getDefaultState(), 2 | 816);
        }
        w.setBlockState(upper, Blocks.REDSTONE_BLOCK.getDefaultState(), 2 | 816);
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
                    w.setBlockState(p, Blocks.OBSIDIAN.getDefaultState(), 2 | 816);
                }
            }
        }
    }

    public static Pair<Integer, Integer> PistonPosFinderUpwards(int i, int y, BlockPos down, List<HashSet<Pair<Integer, Integer>>> occupied_map, List<BlockPos> rep_map, int sanity_check, List<Integer> piston_pos)
    {
        var v = CheckDownwardsRedstonePos(i - 2, y, down, occupied_map);
        v &= CheckDownwardsRedstonePos(i + 1, y, down, occupied_map);
        v &= CheckDownwardsRedstonePos(i + 2, y, down, occupied_map);
        v &= CheckDownwardsRedstonePos(i - 1, y, down, occupied_map);
        boolean do_rep_res = false;
        while(!v)
        {
            i -= 1;
            var v1 = CheckDownwardsRedstonePos(i - 2, y, down, occupied_map);
            var v2 = CheckDownwardsRedstonePos(i + 1, y, down, occupied_map);
            var v3 = CheckDownwardsRedstonePos(i + 2, y, down, occupied_map);
            var v4 = CheckDownwardsRedstonePos(i - 1, y, down, occupied_map);
            v = v1 & v2 & v3 & v4;
            sanity_check++;

            if(do_rep_res)
            {
                List<BlockPos> tm = new ArrayList<>();
                if (!v1)
                    v1 = ReserveRepeater(i - 2, y, down, occupied_map, tm);
                if (!v2)
                    v2 = ReserveRepeater(i + 1, y, down, occupied_map, tm);
                if (!v3)
                    v3 = ReserveRepeater(i + 2, y, down, occupied_map, tm);
                if (!v4)
                    v4 = ReserveRepeater(i - 1, y, down, occupied_map, tm);

                v = v1 & v2 & v3 & v4;
                if(v)
                {
                    if(i - 3 <= piston_pos.getLast())
                    {
                        if(piston_pos.size() == 1)
                        {
                            throw new RuntimeException("Failed to build downwards connector at: " + down + " Could not find position for piston");
                        }
                        /// Blindly move the previous piston on block up. Could cause interference with nearby wires
                        /// However its very unlikely that this code get triggered and that piston causes problems so we ignore this problem for now
                        // TODO: fix
                        piston_pos.set(piston_pos.size() - 1, piston_pos.getLast() + 1);
                    }
                    else
                    {
                        rep_map.addAll(tm);
                        do_rep_res = false;
                    }
                }
            }

            if(i - 3 <= piston_pos.getLast())
            {
                do_rep_res = true;
                v = false;
                i = piston_pos.getLast() + 10;
            }

            if(sanity_check > 2000)
            {
                throw new RuntimeException("Failed to build upwards connector at: " + down);
            }
        }
        piston_pos.add(i);
        System.out.println("valid: " + v + " i: " + i);
        return Pair.of(i, sanity_check);
    }

    public static void BuildDownwardsOld(ServerWorld w, BlockPos down, BlockPos upper, List<HashSet<Pair<Integer, Integer>>> occupied_map, BlockPos start, List<BlockPos> rep_map)
    {
        w.setBlockState(down.add(0, 1, 0), Blocks.REDSTONE_BLOCK.getDefaultState(), 2 | 816);
        // w.setBlockState(down.add(0, 2, 0), Blocks.SLIME_BLOCK.getDefaultState(), 2 | 816);
        w.setBlockState(down.add(0, 2, 0), Blocks.STICKY_PISTON.getDefaultState().with(FacingBlock.FACING, Direction.DOWN), 2 | 816);
        w.setBlockState(down.add(0, 3, 0), Blocks.NOTE_BLOCK.getDefaultState(), 2 | 816);
        for (int i = down.getY() + 4; i <= upper.getY(); i++)
        {
            w.setBlockState(down.withY(i), Blocks.OBSERVER.getDefaultState().with(FacingBlock.FACING, Direction.UP), 2 | 816);
        }
    }

    public static void BuildDownwardsOldOld(ServerWorld w, BlockPos down, BlockPos upper, List<HashSet<Pair<Integer, Integer>>> occupied_map, BlockPos start, List<BlockPos> rep_map)
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
            w.setBlockState(down.withY(pistonPo), Blocks.STICKY_PISTON.getDefaultState().with(FacingBlock.FACING, Direction.DOWN), 2 | 816);
            Router.g_pistonlist.add(down.withY(pistonPo));
        }

        var srrp = LeeRouter.GetSurroundingPoints(Pair.of(down.getX(), down.getZ()));
        for (Integer pistonPo : piston_pos)
        {
            var blkp = srrp.stream().map(a -> new BlockPos(a.getLeft(), pistonPo, a.getRight())).toList();
            for (BlockPos blockPos : blkp)
            {
                if(!w.getBlockState(blockPos.add(0, -1, 0)).isAir())
                {
                    w.setBlockState(blockPos.add(0,1,0), Blocks.OBSIDIAN.getDefaultState(), 2 | 816);
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
            w.setBlockState(down.withY(piston_pos.get(i) + 1), Blocks.REDSTONE_WIRE.getDefaultState(), 2 | 816);
            w.setBlockState(down.withY(piston_pos.get(i) + 3), Blocks.REDSTONE_BLOCK.getDefaultState(), 2 | 816);
        }

        var p1 = down.withY(piston_pos.getLast() - 1);
        var p2 = down.add(0, 2,0);
        for (BlockPos blockPos : BlockPos.iterate(p1, p2))
        {
            w.setBlockState(blockPos, Blocks.SLIME_BLOCK.getDefaultState(), 2 | 816);
        }
        w.setBlockState(down.add(0, 1, 0), Blocks.REDSTONE_BLOCK.getDefaultState(), 2 | 816);

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
                    w.setBlockState(p, Blocks.OBSIDIAN.getDefaultState(), 2 | 816);
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
                        /// Blindly move the previous piston on block down. Could cause interference with nearby wires
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
