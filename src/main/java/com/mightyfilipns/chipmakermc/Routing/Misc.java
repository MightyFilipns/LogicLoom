package com.mightyfilipns.chipmakermc.Routing;

import com.mightyfilipns.chipmakermc.JsonDesign;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.List;

import static com.mightyfilipns.chipmakermc.Routing.LeeRouter.GetSurroundingPoints;

public class Misc
{
    public static Pair<Integer, Integer> AsPair(BlockPos p)
    {
        return Pair.of(p.getX(), p.getZ());
    }

    public static BlockPos AsBlockPos(Pair<Integer, Integer> p, int y)
    {
        return new BlockPos(p.getLeft(), y , p.getRight());
    }

    public static HashSet<Pair<Integer, Integer>> MakeObstMapFromPort(BlockPos p1, JsonDesign.PortDirection dir)
    {
        var pts1 = GetSurroundingPoints(AsPair(p1));
        HashSet<Pair<Integer, Integer>> ret = new HashSet<>(pts1);
        List<Pair<Integer, Integer>> pts2;
        ret.add(AsPair(p1));
        if (dir == JsonDesign.PortDirection.Input)
        {
            pts2 = GetSurroundingPoints(AsPair(p1.add(-1, 0, 0)));
            ret.add(AsPair(p1.add(-1, 0, 0)));
        }
        else // Output
        {
            pts2 = GetSurroundingPoints(AsPair(p1.add(0, 0, 1)));
            ret.add(AsPair(p1.add(0, 0, 1)));
        }
        ret.addAll(pts2);
        return ret;
    }
    public static HashSet<Pair<Integer, Integer>> MakeObstMapFromPortRemove(BlockPos p1, JsonDesign.PortDirection dir)
    {
        var pts1 = GetSurroundingPoints(AsPair(p1));
        HashSet<Pair<Integer, Integer>> ret = new HashSet<>(pts1);
        List<Pair<Integer, Integer>> pts2;
        ret.add(AsPair(p1));
        if (dir == JsonDesign.PortDirection.Input)
        {
            pts2 = GetSurroundingPoints(AsPair(p1.add(-1, 0, 0)));
            ret.add(AsPair(p1.add(-1, 0, 0)));
        }
        else // Output
        {
            pts2 = GetSurroundingPoints(AsPair(p1.add(0, 0, 1)));
            ret.add(AsPair(p1.add(0, 0, 1)));
        }
        if(dir == JsonDesign.PortDirection.Input)
        {
            ret.remove(Pair.of(p1.getX() - 1, p1.getZ()));
        }
        ret.addAll(pts2);
        return ret;
    }
}
