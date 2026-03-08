package com.mightyfilipns.logicloom.Routing;

import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import static com.mightyfilipns.logicloom.Routing.LeeRouter.GetSurroundingPoints;

public class DFSWireFinder
{

    static boolean DFS_Mark(HyperGraphNet hp, int current, ObstacleMap obm, int depth, boolean[] visited_map, int y)
    {
        if(depth > 500)
        {
            throw new RuntimeException("DFS_Mark: recursion depth exceeded 500");
        }

        var p1 = hp.all_points.get(current);

        for (Integer integer : hp.adj_list.get(current))
        {
            if (visited_map[integer])
                continue;
            visited_map[integer] = true;
            var p2 = hp.all_points.get(integer);

            for (BlockPos bp : BlockPos.iterate(p1, p2))
            {
                var np = Pair.of(bp.getX(), bp.getZ());
                if(!obm.IsFreeIncludeWire(np, y))
                    return false;
                obm.TempAddWire(np);
                GetSurroundingPoints(np).forEach(obm::TempAddWire);
            }

           if(!DFS_Mark(hp, integer, obm, depth + 1, visited_map, y))
               return false;
        }

        return true;
    }

    static boolean DFS_Mark_tpn(TwoPinNet tpn, ObstacleMap obm, int y)
    {
        for (int i = 1; i < tpn.point_list.size(); i++)
        {
            var p1 = tpn.point_list.get(i);
            var p2 = tpn.point_list.get(i - 1);
            for (BlockPos bp : BlockPos.iterate(p1, p2))
            {
                var np = Pair.of(bp.getX(), bp.getZ());
                if(!obm.IsFreeIncludeWire(np, y))
                    return false;
                obm.TempAddWire(np);
                GetSurroundingPoints(np).forEach(obm::TempAddWire);
            }
        }

        return true;
    }
}
