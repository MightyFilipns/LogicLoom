package com.mightyfilipns.chipmakermc.Routing;

import com.mightyfilipns.chipmakermc.JsonDesign;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ObstacleFixer
{

    static void FixObstaclesHyperGraph(HyperGraphNet hp, HashSet<Pair<Integer, Integer>> port_map, ServerWorld w)
    {
        HashSet<Pair<Integer, Integer>> obst_points = new HashSet<>();
        hp.pin_port_pos.stream().map(a -> Misc.MakeObstMapFromPortRemove(a,
                a == hp.pin_port_pos.get(hp.out_port_pos) ? JsonDesign.PortDirection.Output : JsonDesign.PortDirection.Input)).forEach(obst_points::addAll);
        port_map.removeAll(obst_points);
        // hp.pin_port_pos.stream().map(a -> Pair.of(a.getX(), a.getZ())).toList().forEach(port_map::remove);
        /*var d = hp.pin_port_pos.stream()
                .<HashSet<Pair<Integer, Integer>>>mapMulti((a, c)
                        -> c.accept(Misc.MakeObstMapFromPort(a,
                        a == hp.pin_port_pos.get(hp.out_port_pos) ? JsonDesign.PortDirection.Output : JsonDesign.PortDirection.Input)
                )).toList();*/

        List<List<Integer>> adj_list = new ArrayList<>();
        List<BlockPos> all_points = new ArrayList<>();
        HashMap<BlockPos, Integer> block_to_index = new HashMap<>();
        for (int i = 0; i < hp.tree.branch.length; i++)
        {
            var brn = hp.tree.branch[i];
            if(port_map.contains(Pair.of(brn.x, brn.y)))
            {
                // Steiner point is over a port
                do {
                    // Move the Steiner point
                    brn.x--;
                    // TODO: better algorithm for finding a valid point
                } while(port_map.contains(Pair.of(brn.x, brn.y)));
            }
        }

        for (Flute.Branch branch : hp.tree.branch)
        {
            // todo check if both branches intersect a point and find a possible solution
            var startp = new BlockPos(branch.x, 0, branch.y);
            var endp = new BlockPos(hp.tree.branch[branch.n].x, 0, hp.tree.branch[branch.n].y);

            if(startp.equals(endp))
                continue;

            var mid1 = new BlockPos(startp.getX(), 0, endp.getZ());
            var mid2 = new BlockPos(endp.getX(), 0, startp.getZ());

            if(!AxisDiffer(startp, endp) && !IntersectsObstacles(startp, endp, port_map))
            {
                int si;
                int ei;
                si = GetPointIndex(block_to_index, startp, all_points, adj_list);
                ei = GetPointIndex(block_to_index, endp, all_points, adj_list);

                ConnBranches(adj_list, si, ei);
                continue;
            }

            BlockPos fmid;

            boolean c1 = IntersectsObstacles(startp, mid1, port_map);
            boolean c2 = IntersectsObstacles(mid1, endp, port_map);

            if(c1 || c2)
            {
                c1 = IntersectsObstacles(startp, mid2, port_map);
                c2 = IntersectsObstacles(mid2, endp, port_map);
                if(c1 || c2)
                {
                    var rez = LeeRouter.DoLeeRouter(port_map, startp, endp);
                    List<Integer> indicies = new ArrayList<>();
                    for (Pair<Integer, Integer> integerIntegerPair : rez)
                    {
                        indicies.add(GetPointIndex(block_to_index, new BlockPos(integerIntegerPair.getLeft(), 0, integerIntegerPair.getRight()), all_points, adj_list));
                    }
                    for (int i = 1; i < indicies.size(); i++)
                    {
                        int i1 = indicies.get(i - 1);
                        int i2 = indicies.get(i);
                        ConnBranches(adj_list, i1, i2);
                    }
                    continue;
                }
                else
                    fmid = mid2;
            }
            else
                fmid = mid1;

            int si;
            int ei;
            int mi;
            si = GetPointIndex(block_to_index, startp, all_points, adj_list);
            ei = GetPointIndex(block_to_index, endp, all_points, adj_list);
            mi = GetPointIndex(block_to_index, fmid, all_points, adj_list);


            ConnBranches(adj_list, si, mi);
            ConnBranches(adj_list, mi, ei);
        }
        hp.allpoints_pos = all_points.indexOf(hp.pin_port_pos.get(hp.out_port_pos).withY(0));

        hp.SetAdjList(adj_list, all_points);
        port_map.addAll(obst_points);
        //port_map.addAll(hp.pin_port_pos.stream().map(a -> Pair.of(a.getX(), a.getZ())).toList());
    }

    public static void ConnBranches(List<List<Integer>> adj_list, int si, int mi)
    {
        if (si == mi)
            throw new RuntimeException("Can not connect node to itself");

        if (!adj_list.get(si).stream().filter(a -> a == mi).toList().isEmpty())
            return;
            //throw new RuntimeException("Can not add double branch");

        adj_list.get(si).add(mi);
        adj_list.get(mi).add(si);
    }

    private static int GetPointIndex(HashMap<BlockPos, Integer> block_to_index, BlockPos startp, List<BlockPos> all_points, List<List<Integer>> adj_list)
    {
        int si;
        if (block_to_index.containsKey(startp))
        {
            si = block_to_index.get(startp);
        }
        else
        {
            si = all_points.size();
            block_to_index.put(startp, si);
            all_points.add(startp);
            adj_list.add(new ArrayList<>());
        }
        return si;
    }

    static boolean IntersectsObstacles(BlockPos p1, BlockPos p2, HashSet<Pair<Integer, Integer>> port_map)
    {
        for (BlockPos blockPos : BlockPos.iterate(p1, p2))
        {
            if (port_map.contains(Pair.of(blockPos.getX(), blockPos.getZ())))
                return true;
        }
        return false;
    }

    static boolean AxisDiffer(BlockPos p1, BlockPos p2)
    {
        return p1.getX() != p2.getX() && p1.getZ() != p2.getZ();
    }
}
