package com.mightyfilipns.chipmakermc.Routing;

import com.mightyfilipns.chipmakermc.CellInfo;
import com.mightyfilipns.chipmakermc.JsonDesign;
import com.mightyfilipns.chipmakermc.JsonLoader.AbstractCell;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.mightyfilipns.chipmakermc.Routing.LeeRouter.GetSurroundingPoints;
import static com.mightyfilipns.chipmakermc.Routing.ObstacleFixer.ConnBranches;
import static com.mightyfilipns.chipmakermc.Routing.ObstacleFixer.IntersectsObstacles;

public class Router
{
    static Flute flu = null;

    static List<HyperGraphNet> cached_hy = null;
    static List<TwoPinNet> cached_tpn = null;
    static List<HashSet<Pair<Integer, Integer>>> ocm = null;

    public static void DoRouting(CommandContext<ServerCommandSource> context, JsonDesign.DesignModule mod, Map<CellInfo, BlockPos> cellmap, Map<Integer, BlockPos> port_rel_pos)
    {
        Map<Integer, List<AbstractCell>> dd = new HashMap<>();

        for (CellInfo cellInfo : mod.cells.values())
        {
            cellInfo.connections.forEach((a,b) ->
            {
                for (Integer i : b)
                {
                    dd.computeIfAbsent(i, k -> new ArrayList<>());
                    dd.get(i).add(cellInfo);
                }
            });
        }

        for (var cellInfo : mod.ports.values())
        {
            cellInfo.bits.forEach(i ->
            {
                dd.computeIfAbsent(i, k -> new ArrayList<>());
                dd.get(i).add(cellInfo);
            });
        }

        BlockPos pos = BlockPosArgumentType.getBlockPos(context, "start_pos");

        List<Map.Entry<Integer, List<AbstractCell>>> hypergraph = dd.entrySet().stream().filter(a -> a.getValue().size() > 2).toList();
        List<Map.Entry<Integer, List<AbstractCell>>> twopin = dd.entrySet().stream().filter(a -> a.getValue().size() <= 2).toList();
        var hy = HandleHyperGraphs(hypergraph, cellmap, port_rel_pos);
        var tpn = HandleTwoPinNets(twopin, cellmap, port_rel_pos);

        var w = context.getSource().getWorld();

        // HashSet<Triple<Integer, Integer, Integer>> route_map = new HashSet<>();
        HashSet<Pair<Integer, Integer>> port_map = new HashSet<>();

        for (TwoPinNet tp : tpn)
        {
            port_map.add(Pair.of(tp.p1.getX(), tp.p1.getZ()));
            port_map.add(Pair.of(tp.p2.getX(), tp.p2.getZ()));
        }

        for (HyperGraphNet hyperGraphNet : hy)
        {
            for (BlockPos pinPortPo : hyperGraphNet.pin_port_pos)
            {
                port_map.add(Pair.of(pinPortPo.getX(), pinPortPo.getZ()));
            }
        }

        int starty = pos.getY() + 3;

        if(true)
        {
            int i =0;
            List<HashSet<Pair<Integer, Integer>>> occupied_map = new ArrayList<>();
            for (HyperGraphNet hp : hy)
            {
                System.out.println("Processing hypergraph " + (i + 1) + " of " + hy.size());
                ObstacleFixer.FixObstaclesHyperGraph(hp, port_map, w);
                System.out.println("Routing hypergraph " + (i + 1) + " of " + hy.size());
                RouteHyperGraph(hp, starty, occupied_map);
                System.out.println("Building hypergraph " + (i + 1) + " of " + hy.size());
                BuildHyperGraph(hp, w, hp.y_pos);
                // ypos++;
                i++;
            }
            i = 0;
            for (TwoPinNet twoPinNet : tpn)
            {
                System.out.println("Processing two pin net " + (i + 1) + " of " + tpn.size());
                FixObstacleTwoPinNet(twoPinNet, port_map);
                System.out.println("Routing two pin net " + (i + 1) + " of " + tpn.size());
                RouteTwoPinNet(twoPinNet, starty, occupied_map);
                System.out.println("Building two pin net " + (i + 1) + " of " + tpn.size());
                BuildTwoPinNet(twoPinNet, w, twoPinNet.y_pos);
                i++;
            }
            ocm = occupied_map;
/*
            BlockState placebl = Blocks.BROWN_WOOL.getDefaultState();
            for (Map.Entry<Integer, List<AbstractCell>> integerListEntry : twopin)
            {
                if (ypos > w.getHeight())
                    break;
                var startpos = GetRelPos(cellmap, integerListEntry.getValue().get(0), integerListEntry.getKey(), port_rel_pos).withY(ypos);
                var endpos = GetRelPos(cellmap, integerListEntry.getValue().get(1), integerListEntry.getKey(), port_rel_pos).withY(ypos);

                var mid = new BlockPos(startpos.getX(), ypos, endpos.getZ());
                for (BlockPos blockPos : BlockPos.iterate(startpos, mid))
                {
                    w.setBlockState(blockPos, placebl, 2 | 816);
                }
                for (BlockPos blockPos : BlockPos.iterate(mid, endpos))
                {
                    w.setBlockState(blockPos, placebl, 2 | 816);
                }
                ypos++;
            }*/
        }
        else if(false)
        {
            var hp = hy.get(0);
            BuildHyperGraph(hp, w, 0);
            /*
            for (Pair<BlockPos, Boolean> pinPo : hy.get(3).pin_pos)
            {
                w.setBlockState(pinPo.getLeft().withY(0),pinPo.getRight() ? Blocks.YELLOW_WOOL.getDefaultState() : Blocks.BLUE_WOOL.getDefaultState());
                // context.getSource().sendFeedback(() -> Text.literal("Pin or Steiner point at: " + pinPo.getLeft()), false);
            }
            */
        }

        cached_hy = hy;
        cached_tpn = tpn;
    }

    public static void RebuildCache(CommandContext<ServerCommandSource> context)
    {
        if(cached_hy == null || cached_tpn == null)
        {
            context.getSource().sendError(Text.literal("Cache empty"));
            return;
        }
        var w = context.getSource().getWorld();
        int i = 0;
        for (HyperGraphNet hp : cached_hy)
        {
            System.out.println("Building hypergraph " + (i + 1) + " of " + cached_hy.size());
            BuildHyperGraph(hp, w, hp.y_pos);
            i++;
        }
        for (TwoPinNet twoPinNet : cached_tpn)
        {
            System.out.println("Building two pin net " + (i + 1) + " of " + cached_tpn.size());
            BuildTwoPinNet(twoPinNet, w, twoPinNet.y_pos);
            i++;
        }
    }

    private static void BuildTwoPinNet(TwoPinNet tpn, ServerWorld w, int y)
    {
        BlockState placebl = Blocks.BROWN_WOOL.getDefaultState();
        for (int i = 1; i < tpn.point_list.size(); i++)
        {
            var p1 = tpn.point_list.get(i - 1).withY(y);
            var p2 = tpn.point_list.get(i).withY(y);
            for (BlockPos bp : BlockPos.iterate(p1, p2))
            {
                w.setBlockState(bp, placebl);
            }
        }
        for (int i = 0; i < tpn.point_list.size() - 1; i++)
        {
            w.setBlockState(tpn.point_list.get(i).withY(y), Blocks.CYAN_WOOL.getDefaultState());
        }
        w.setBlockState(tpn.p1.withY(y), Blocks.ORANGE_WOOL.getDefaultState());
        w.setBlockState(tpn.p2.withY(y), Blocks.ORANGE_WOOL.getDefaultState());
    }

    private static void RouteTwoPinNet(TwoPinNet tpn, int start_y, List<HashSet<Pair<Integer, Integer>>> occupied_map)
    {
        int i = start_y;
        boolean dfs_success = false;
        int iter_c = 0;
        do {
            if(occupied_map.size() >= iter_c)
                occupied_map.add(new HashSet<>());

            HashSet<Pair<Integer, Integer>> c = (HashSet<Pair<Integer, Integer>>) occupied_map.get(iter_c).clone();

            dfs_success = DFS_Mark_tpn(tpn, occupied_map.get(iter_c), c);
            if(dfs_success)
            {
                occupied_map.get(iter_c).addAll(c);
            }
            else
            {
                i++;
                iter_c++;
            }
            if(iter_c > 300)
            {
                throw new RuntimeException("RouteHyperGraph: max 300 DFS_Mark attempts exceeded");
            }
        } while(!dfs_success);

        tpn.y_pos = i;
    }

    private static void FixObstacleTwoPinNet(TwoPinNet tpn, HashSet<Pair<Integer, Integer>> port_map)
    {
        var pr1 = Pair.of(tpn.p1.getX(), tpn.p1.getZ());
        var pr2 = Pair.of(tpn.p2.getX(), tpn.p2.getZ());
        port_map.remove(pr1);
        port_map.remove(pr2);
        var p1 = tpn.p1;
        var p2 = tpn.p2;
        var mid1 = new BlockPos(p1.getX(), 0, p2.getZ());
        var mid2 = new BlockPos(p2.getX(), 0, p1.getZ());

        boolean mid1_possible = !IntersectsObstacles(p1, mid1, port_map) && !IntersectsObstacles(mid1, p2, port_map);
        boolean mid2_possible = !IntersectsObstacles(p1, mid2, port_map) && !IntersectsObstacles(mid2, p2, port_map);

        if(mid2_possible || mid1_possible)
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
            tpn.point_list = blks;
        }
        else
        {
            var leerez = LeeRouter.DoLeeRouter(port_map, p1, p2);
            var blks = leerez.stream().map(a -> new BlockPos(a.getLeft(), 0, a.getRight())).toList();
            List<List<Integer>> adjl = new ArrayList<>();
            adjl.add(new ArrayList<>());
            for (int i = 1; i < leerez.size(); i++)
            {
                adjl.add(new ArrayList<>());
                ConnBranches(adjl, i - 1, i);
            }
            tpn.point_list = blks;
            tpn.adj_list = adjl;
        }

        port_map.add(pr1);
        port_map.add(pr2);
    }

    private static List<TwoPinNet> HandleTwoPinNets(List<Map.Entry<Integer, List<AbstractCell>>> twopin, Map<CellInfo, BlockPos> cellmap, Map<Integer, BlockPos> port_rel_pos)
    {
        List<TwoPinNet> tpn = new ArrayList<>();
        for (Map.Entry<Integer, List<AbstractCell>> integerListEntry : twopin)
        {
            var np = new TwoPinNet(integerListEntry.getKey(), integerListEntry.getValue().getFirst(), integerListEntry.getValue().get(1));
            var p1 = GetRelPos(cellmap, integerListEntry.getValue().getFirst(), integerListEntry.getKey(), port_rel_pos);
            var p2 = GetRelPos(cellmap, integerListEntry.getValue().get(1), integerListEntry.getKey(), port_rel_pos);
            np.p1 = p1;
            np.p2 = p2;

            tpn.add(np);
        }
        return tpn;
    }

    private static void RouteHyperGraph(HyperGraphNet hyperGraphNet, int start_y, List<HashSet<Pair<Integer, Integer>>> occupied_map)
    {
        int i = start_y;
        boolean dfs_success = false;
        int iter_c = 0;
        do {
            if(occupied_map.size() >= iter_c)
                occupied_map.add(new HashSet<>());

            HashSet<Pair<Integer, Integer>> c = (HashSet<Pair<Integer, Integer>>) occupied_map.get(iter_c).clone();

            boolean[] vm = new boolean[hyperGraphNet.all_points.size()];

            dfs_success = DFS_Mark(hyperGraphNet, 0, -1, occupied_map.get(iter_c), 0, vm, c);
            if(dfs_success)
            {
                occupied_map.get(iter_c).addAll(c);
            }
            else
            {
                i++;
                iter_c++;
            }
            if(iter_c > 300)
            {
                throw new RuntimeException("RouteHyperGraph: max 300 DFS_Mark attempts exceeded");
            }
        } while(!dfs_success);

        hyperGraphNet.y_pos = i;
    }

    private static boolean DFS_Mark(HyperGraphNet hp, int current, int v_before, HashSet<Pair<Integer, Integer>> occupied_map, int depth, boolean[] visited_map, HashSet<Pair<Integer, Integer>> oc2)
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
                if(occupied_map.contains(np))
                    return false;
                oc2.add(np);
                oc2.addAll(GetSurroundingPoints(np));
            }

           if(!DFS_Mark(hp, integer, current, occupied_map, depth + 1, visited_map, oc2))
               return false;
        }

        return true;
    }

    private static boolean DFS_Mark_tpn(TwoPinNet tpn, HashSet<Pair<Integer, Integer>> occupied_map, HashSet<Pair<Integer, Integer>> oc2)
    {
        for (int i = 1; i < tpn.point_list.size(); i++)
        {
            var p1 = tpn.point_list.get(i);
            var p2 = tpn.point_list.get(i - 1);
            for (BlockPos bp : BlockPos.iterate(p1, p2))
            {
                var np = Pair.of(bp.getX(), bp.getZ());
                if(occupied_map.contains(np))
                    return false;
                oc2.add(np);
                oc2.addAll(GetSurroundingPoints(np));
            }
        }

        return true;
    }

    private static void BuildHyperGraph(HyperGraphNet hp, ServerWorld w, int y)
    {
        BlockState placebl = Blocks.BROWN_WOOL.getDefaultState();
        int i = 0;
        for (List<Integer> integers : hp.adj_list)
        {
            for (Integer integer : integers)
            {
                var pos1 = hp.all_points.get(i).withY(y);
                var pos2 = hp.all_points.get(integer).withY(y);
                for (BlockPos blockPos : BlockPos.iterate(pos1, pos2))
                {
                    w.setBlockState(blockPos, placebl, 2 | 816);
                }
                w.setBlockState(pos1, Blocks.YELLOW_WOOL.getDefaultState(), 2 | 816);
                w.setBlockState(pos2, Blocks.YELLOW_WOOL.getDefaultState(), 2 | 816);
            }
            i++;
        }
        /*
        for (Flute.Branch branch : hp.tree.branch)
        {
            var startpos = new BlockPos(branch.x, y, branch.y);
            var endpos = new BlockPos(hp.tree.branch[branch.n].x, y, hp.tree.branch[branch.n].y);
            if(AxisDiffer(startpos, endpos))
            {
                var mid = new BlockPos(startpos.getX(), y, endpos.getZ());
                for (BlockPos blockPos : BlockPos.iterate(startpos, mid))
                {
                    w.setBlockState(blockPos, placebl, 2 | 816);
                }
                for (BlockPos blockPos : BlockPos.iterate(mid, endpos))
                {
                    w.setBlockState(blockPos, placebl, 2 | 816);
                }
            }
            else
            {
                for (BlockPos blockPos : BlockPos.iterate(startpos, endpos))
                {
                    w.setBlockState(blockPos, placebl, 2 | 816);
                }
            }

            w.setBlockState(startpos, Blocks.YELLOW_WOOL.getDefaultState(), 2 | 816);
        }*/

        for (BlockPos pinPortPo : hp.pin_port_pos)
        {
            w.setBlockState(pinPortPo.withY(y), Blocks.BLUE_WOOL.getDefaultState(), 2 | 816);
        }
    }

    static List<HyperGraphNet> HandleHyperGraphs(List<Map.Entry<Integer, List<AbstractCell>>> h, Map<CellInfo, BlockPos> cellmap, Map<Integer, BlockPos> port_rel_pos)
    {
        List<HyperGraphNet> ret = new ArrayList<>();
        for (Map.Entry<Integer, List<AbstractCell>> integerListEntry : h)
        {
            List<BlockPos> rel_block_pos = new ArrayList<>();
            for (AbstractCell abstractCell : integerListEntry.getValue())
            {
                rel_block_pos.add(GetRelPos(cellmap, abstractCell, integerListEntry.getKey(), port_rel_pos));
            }

            // List<List<Integer>> adj = new ArrayList<>();

            int[] xarr = rel_block_pos.stream().map(Vec3i::getX).mapToInt(value -> value).toArray();
            int[] zarr = rel_block_pos.stream().map(Vec3i::getZ).mapToInt(value -> value).toArray();

            var treerez = flu.flute(xarr.length, xarr, zarr, Flute.ACCURACY);

            // var d = FindMinimumSteinerRectilinearTree(rel_block_pos, adj);

            ret.add(new HyperGraphNet(integerListEntry.getKey(), treerez, integerListEntry.getValue(), rel_block_pos));
        }
        return ret;
    }

    public static void SetupFlute(InputStream in1, InputStream in2)
    {
        flu = new Flute();
        try {
            flu.readLUT(in1 ,in2);
        } catch (IOException e) {
            System.out.println("Failed to load FLUTE library lookup tables");
        }
    }

    public static BlockPos GetRelPos(Map<CellInfo, BlockPos> cellmap, AbstractCell a, Integer conn, Map<Integer, BlockPos> port_rel_pos)
    {
        if(a instanceof CellInfo b)
        {
            String pin_name = "";
            for (Map.Entry<String, List<Integer>> stringListEntry : b.connections.entrySet())
            {
                for (Integer i : stringListEntry.getValue())
                {
                    if(Objects.equals(conn, i))
                    {
                        pin_name = stringListEntry.getKey();
                        break;
                    }
                }
            }
            var cell_pos = cellmap.get(b);
            return switch (pin_name) {
                case "A" -> cell_pos.add(0, 0, 5);
                case "B" -> cell_pos.add(2, 0, 5);
                case "Y" -> cell_pos.add(1, 0, 0); //cell_pos.add(2, 0, 0); -- currently always set to the right side
                default -> throw new RuntimeException("Invalid pin name: " + pin_name);
            };
        }
        else if(a instanceof JsonDesign.DesignPortInfo)
        {
            // TODO: fix
            return port_rel_pos.get(conn);
        }
        else
        {
            throw new RuntimeException("unimplemented abstract cell");
        }
    }
}
