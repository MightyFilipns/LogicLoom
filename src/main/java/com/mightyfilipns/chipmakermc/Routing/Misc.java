package com.mightyfilipns.chipmakermc.Routing;

import com.mightyfilipns.chipmakermc.JsonLoader.PortDirection;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

    public static HashSet<Pair<Integer, Integer>> MakeObstMapFromPort(BlockPos p1, PortDirection dir)
    {
        var pts1 = GetSurroundingPoints(AsPair(p1));
        HashSet<Pair<Integer, Integer>> ret = new HashSet<>(pts1);
        List<Pair<Integer, Integer>> pts2;
        ret.add(AsPair(p1));
        if (dir == PortDirection.Input)
        {
            pts2 = GetSurroundingPoints(AsPair(p1.add(1, 0, 0)));
            ret.add(AsPair(p1.add(1, 0, 0)));
        }
        else // Output
        {
            pts2 = new ArrayList<>();
        }
        ret.addAll(pts2);
        return ret;
    }
    public static HashSet<Pair<Integer, Integer>> MakeObstMapFromPortRemove(BlockPos p1, PortDirection dir)
    {
        HashSet<Pair<Integer, Integer>> ret = MakeObstMapFromPort(p1, dir);
        if(dir == PortDirection.Input)
        {
            ret.remove(Pair.of(p1.getX() + 1, p1.getZ()));
        }
        return ret;
    }

    static boolean AxisDiffer(BlockPos p1, BlockPos p2)
    {
        return p1.getX() != p2.getX() && p1.getZ() != p2.getZ();
    }

    public static void SetupFlute(InputStream in1, InputStream in2)
    {
        if (Router.flu != null)
            return;
        Router.flu = new Flute();
        try {
            Router.flu.readLUT(in1, in2);
        } catch (IOException e) {
            System.out.println("Failed to load FLUTE library lookup tables");
        }
    }
}
