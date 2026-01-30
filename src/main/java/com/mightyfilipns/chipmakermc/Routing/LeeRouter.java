package com.mightyfilipns.chipmakermc.Routing;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.minecraft.block.Blocks;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class LeeRouter
{
    public static ServerWorld w = null;

    public static List<Pair<Integer, Integer>> DoLeeRouter(HashSet<Pair<Integer, Integer>> port_map, BlockPos start, BlockPos end)
    {
        HashMap<Pair<Integer, Integer>, Integer> value_grid = new HashMap<>();
        List<Pair<Integer, Integer>> last_marked_points = new ArrayList<>();
        var endp = Pair.of(end.getX(), end.getZ());
        var sp = Pair.of(start.getX(), start.getZ());
        value_grid.put(sp, 0);
        int lastv = 0;
        last_marked_points.add(sp);
        if(port_map.contains(endp))
            throw new RuntimeException("LeeRouter: End point is in the list of obstacles. Unroutable");
        if(port_map.contains(sp))
            throw new RuntimeException("LeeRouter: Start point is in the list of obstacles. Unroutable");

        // Bounding box
        var max = Pair.of(Math.max(start.getX(), end.getX()), Math.max(start.getZ(), end.getZ()));
        var min = Pair.of(Math.min(start.getX(), end.getX()), Math.min(start.getZ(), end.getZ()));

        int tol = 9;
        while(!value_grid.containsKey(endp))
        {
            lastv++;
            List<Pair<Integer, Integer>> new_mark = new ArrayList<>();
            boolean expanded = false;
            for (Pair<Integer, Integer> lastMarkedPoint : last_marked_points)
            {
                // var pts = GetSurroundingPoints(lastMarkedPoint);
                var pts = GetSurroundingPointsLimited(lastMarkedPoint, max, min, tol);
                for (Pair<Integer, Integer> pt : pts)
                {
                    if(!value_grid.containsKey(pt) && !port_map.contains(pt))
                    {
                        expanded = true;
                        value_grid.put(pt, lastv);
                        new_mark.add(pt);
                    }
                }

            }
            if(!new_mark.isEmpty())
            {
                last_marked_points = new_mark;
            }
            else
            {
                for (Pair<Integer, Integer> integerIntegerPair : port_map)
                {
                    w.setBlockState(Misc.AsBlockPos(integerIntegerPair, 0), Blocks.RED_WOOL.getDefaultState());
                }
                w.setBlockState(start.withY(0), Blocks.BLUE_WOOL.getDefaultState());
                w.setBlockState(end.withY(0), Blocks.BLUE_WOOL.getDefaultState());
                throw new RuntimeException("empty new mark");
            }

            if(lastv > 2000)
            {
                throw new RuntimeException("Lee Router: Exceeded 2000 iterations. End point is possibly unreachable or too far");
            }
        }

        Pair<Integer, Integer> last_pos = endp;
        LeeDir last_dir = LeeDir.None;
        List<Pair<Integer, Integer>> ret = new ArrayList<>();
        ret.add(endp);
        while(last_pos != sp)
        {
            // var res = GetSurroundingPoints(last_pos).stream().filter(value_grid::containsKey);
            var res = GetSurroundingPointsLimited(last_pos, max, min, tol).stream().filter(value_grid::containsKey);
            var r1 = res.toList();
            var possible_tiles_v = value_grid.entrySet().stream().filter(a -> r1.contains(a.getKey())).toList();
            var minv = possible_tiles_v.stream().min(Comparator.comparingInt(Map.Entry::getValue)).get().getValue();
            var rez = possible_tiles_v.stream().filter(a -> Objects.equals(a.getValue(), minv)).toList();
            if(rez.size() == 1)
            {
                var new_pos = rez.getFirst().getKey();
                var cdir = CalcDir(last_pos, new_pos);
                if(CalcDir(last_pos, new_pos) != last_dir)
                {
                    if(last_dir != LeeDir.None)
                    {
                        ret.add(last_pos);
                    }
                    last_dir = cdir;
                }
                last_pos = new_pos;
            }
            else
            {
                var new_pos1 = rez.getFirst().getKey();
                var new_pos2 = rez.get(1).getKey();
                var dir1 = CalcDir(last_pos, new_pos1);
                var dir2 = CalcDir(last_pos, new_pos2);
                if(dir1 == last_dir)
                {
                    last_pos = new_pos1;
                }
                else if (dir2 == last_dir)
                {
                    last_pos = new_pos2;
                }
                else
                {
                    if(last_dir != LeeDir.None)
                    {
                        ret.add(last_pos);
                    }
                    last_dir = dir1;
                    last_pos = new_pos1; // pick either does not matter
                }
            }
        }
        ret.add(sp);

        return ret;
    }

    public static List<Pair<Integer, Integer>> GetSurroundingPointsLimited(Pair<Integer, Integer> p, Pair<Integer, Integer> max, Pair<Integer, Integer> min, int tolerance)
    {
        List<Pair<Integer, Integer>> ret = new ArrayList<>();
        if(p.getLeft() <= max.getLeft() + tolerance)
        {
            ret.add(Pair.of(p.getLeft() + 1, p.getRight()));
        }
        if(p.getLeft() >= min.getLeft() - tolerance)
        {
            ret.add(Pair.of(p.getLeft() - 1, p.getRight()));
        }
        if(p.getRight() <= max.getRight() + tolerance)
        {
            ret.add(Pair.of(p.getLeft(), p.getRight() + 1));
        }
        if(p.getRight() >= min.getRight() - tolerance)
        {
            ret.add(Pair.of(p.getLeft(), p.getRight() - 1));
        }
        return ret;
    }

    public static List<Pair<Integer, Integer>> GetSurroundingPoints(Pair<Integer, Integer> p)
    {
        List<Pair<Integer, Integer>> ret = new ArrayList<>();
        ret.add(Pair.of(p.getLeft() + 1, p.getRight()));
        ret.add(Pair.of(p.getLeft() - 1, p.getRight()));
        ret.add(Pair.of(p.getLeft(), p.getRight() + 1));
        ret.add(Pair.of(p.getLeft(), p.getRight() - 1));
        return ret;
    }

    private static LeeDir CalcDir(Pair<Integer, Integer> p1, Pair<Integer, Integer> p2)
    {
        Pair<Integer, Integer> rez = Pair.of(p2.getLeft() - p1.getLeft(), p2.getRight() - p1.getRight());
        if (rez.getLeft() == 1)
            return LeeDir.PosX;
        else if (rez.getLeft() == -1)
            return LeeDir.NegX;
        else if (rez.getRight() == 1)
            return LeeDir.PosY;
        else if (rez.getRight() == -1)
            return LeeDir.NegY;
        throw new RuntimeException("Failed to determine direction");
    }

    enum LeeDir
    {
        PosX,
        NegX,
        PosY,
        NegY,
        None
    }
}
