package com.mightyfilipns.chipmakermc.Routing;

import com.mightyfilipns.chipmakermc.JsonLoader.PortDirection;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class ObstacleFixer
{

    static void FixObstaclesHyperGraph(HyperGraphNet hp, ObstacleMap port_map, ServerWorld w, int y)
    {
        port_map.TempExclude(hp.pin_port_pos.stream().map(a -> Misc.MakeObstMapFromPortRemove(a,
                a == hp.pin_port_pos.get(hp.out_port_pos) ? PortDirection.Output : PortDirection.Input)).toList());

        port_map.SetFullExclude(hp.pin_port_pos.stream().map(a -> Misc.MakeObstMapFromPort(a,
                a == hp.pin_port_pos.get(hp.out_port_pos) ? PortDirection.Output : PortDirection.Input)).toList());

        List<List<Integer>> adj_list = new ArrayList<>();
        List<BlockPos> all_points = new ArrayList<>();
        HashMap<BlockPos, Integer> block_to_index = new HashMap<>();
        for (int i = 0; i < hp.tree.branch.length; i++)
        {
            var brn = hp.tree.branch[i];
            if(!port_map.IsFree(Pair.of(brn.x, brn.y), y))
            {
                // Steiner point is over an obstacle
                do {
                    // Move the Steiner point
                    brn.x--;
                    // TODO: better algorithm for finding a valid point
                } while(!port_map.IsFree(Pair.of(brn.x, brn.y), y));
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

            if(!Misc.AxisDiffer(startp, endp) && !IntersectsObstacles(startp, endp, port_map, y))
            {
                int si;
                int ei;
                si = GetPointIndex(block_to_index, startp, all_points, adj_list);
                ei = GetPointIndex(block_to_index, endp, all_points, adj_list);

                ConnBranches(adj_list, si, ei);
                continue;
            }

            BlockPos fmid;

            boolean c1 = IntersectsObstacles(startp, mid1, port_map, y);
            boolean c2 = IntersectsObstacles(mid1, endp, port_map, y);

            if(c1 || c2)
            {
                c1 = IntersectsObstacles(startp, mid2, port_map, y);
                c2 = IntersectsObstacles(mid2, endp, port_map, y);
                if(c1 || c2)
                {
                    var rez = LeeRouter.DoLeeRouter(port_map, startp, endp, y);
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
    }

    public static void ConnBranches(List<List<Integer>> adj_list, int si, int mi)
    {
        if (si == mi)
            throw new RuntimeException("Can not connect node to itself");

        if (!adj_list.get(si).stream().filter(a -> a == mi).toList().isEmpty())
            return;

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

    static boolean IntersectsObstacles(BlockPos p1, BlockPos p2, ObstacleMap port_map, int y)
    {
        for (BlockPos blockPos : BlockPos.iterate(p1, p2))
        {
            if (!port_map.IsFree(Pair.of(blockPos.getX(), blockPos.getZ()), y))
                return true;
        }
        return false;
    }

    static void FixObstacleTwoPinNet(TwoPinNet tpn, ObstacleMap obm, int y)
    {
        var pr1 = Misc.MakeObstMapFromPortRemove(tpn.p1, tpn.p1dir);
        var pr2 = Misc.MakeObstMapFromPortRemove(tpn.p2, tpn.p1dir.Invert());
        var pr3 = new HashSet<>(pr1);
        pr3.addAll(pr2);
        obm.TempExclude(Collections.singletonList(pr3));

        pr1 = Misc.MakeObstMapFromPort(tpn.p1, tpn.p1dir);
        pr2 = Misc.MakeObstMapFromPort(tpn.p2, tpn.p1dir.Invert());
        obm.SetFullExclude(List.of(new HashSet[]{pr1, pr2}));

        var p1 = tpn.p1;
        var p2 = tpn.p2;
        var mid1 = new BlockPos(p1.getX(), 0, p2.getZ());
        var mid2 = new BlockPos(p2.getX(), 0, p1.getZ());

        boolean mid1_possible = !IntersectsObstacles(p1, mid1, obm, y) && !IntersectsObstacles(mid1, p2, obm, y);
        boolean mid2_possible = !IntersectsObstacles(p1, mid2, obm, y) && !IntersectsObstacles(mid2, p2, obm, y);

        if(!Misc.AxisDiffer(p1, p2) && !IntersectsObstacles(p1, p2, obm, y))
        {
            List<List<Integer>> adjl = new ArrayList<>();
            adjl.add(new ArrayList<>());
            adjl.add(new ArrayList<>());

            List<BlockPos> blks = new ArrayList<>();
            blks.add(p1);
            blks.add(p2);

            ConnBranches(adjl, 0 ,1);

            tpn.adj_list = adjl;
            tpn.point_list = tpn.p1dir == PortDirection.Output ? blks : blks.reversed();
        }
        else if(mid2_possible || mid1_possible)
        {
            var fmid = mid1_possible ? mid1 : mid2;

            List<List<Integer>> adjl = new ArrayList<>();
            adjl.add(new ArrayList<>());
            adjl.add(new ArrayList<>());
            adjl.add(new ArrayList<>());
            ConnBranches(adjl, 0 ,1);
            ConnBranches(adjl, 1 ,2);
            List<BlockPos> blks = new ArrayList<>();
            blks.add(p1);
            blks.add(fmid);
            blks.add(p2);
            tpn.adj_list = adjl;
            tpn.point_list = tpn.p1dir == PortDirection.Output ? blks : blks.reversed();
        }
        else
        {
            var leerez = LeeRouter.DoLeeRouter(obm, p1, p2, y);
            var blks = leerez.stream().map(a -> new BlockPos(a.getLeft(), 0, a.getRight())).toList();
            List<List<Integer>> adjl = new ArrayList<>();
            adjl.add(new ArrayList<>());
            for (int i = 1; i < leerez.size(); i++)
            {
                adjl.add(new ArrayList<>());
                ConnBranches(adjl, i - 1, i);
            }
            tpn.point_list = tpn.p1dir == PortDirection.Output ? blks.reversed() : blks;
            tpn.adj_list = adjl;
        }
    }
}
