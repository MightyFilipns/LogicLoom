package com.mightyfilipns.chipmakermc.Routing;

import com.mightyfilipns.chipmakermc.JsonLoader.CellInfo;
import com.mightyfilipns.chipmakermc.JsonLoader.JsonDesign;
import com.mightyfilipns.chipmakermc.JsonLoader.AbstractCell;
import com.mightyfilipns.chipmakermc.Placer;
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

import java.util.*;

public class Router
{
    static Flute flu = null;

    public static List<HyperGraphNet> cached_hy = null;
    public static List<TwoPinNet> cached_tpn = null;
    static List<HashSet<Pair<Integer, Integer>>> ocm = null;
    static List<HashSet<Pair<Integer, Integer>>> ocm_wire = null;
    static List<BlockPos> g_rep_map = null;
    public static List<BlockPos> g_pistonlist = new ArrayList<>();
    public static List<BlockPos> g_update_pos = new ArrayList<>();

    public static int max_y = 80;

    public static void DoRouting(CommandContext<ServerCommandSource> context, JsonDesign.DesignModule mod, Map<CellInfo, BlockPos> cellmap, Map<Integer, BlockPos> port_rel_pos)
    {
        g_pistonlist.clear();
        g_update_pos.clear();
        Map<Integer, List<AbstractCell>> dd = new HashMap<>();

        LeeRouter.w = context.getSource().getWorld();

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

        ObstacleMap obm = new ObstacleMap(hy, tpn);

        var w = context.getSource().getWorld();

        HashSet<Pair<Integer, Integer>> port_map = new HashSet<>();

        for (TwoPinNet tp : tpn)
        {
            port_map.addAll(Misc.MakeObstMapFromPort(tp.p1, tp.p1dir));
            port_map.addAll(Misc.MakeObstMapFromPort(tp.p2, tp.p1dir.Invert()));
        }

        for (HyperGraphNet hyperGraphNet : hy)
        {
            for (int i = 0; i < hyperGraphNet.pin_port_pos.size(); i++)
            {
                var pinPortPo = hyperGraphNet.pin_port_pos.get(i);
                JsonDesign.PortDirection dir = i == hyperGraphNet.out_port_pos ? JsonDesign.PortDirection.Output : JsonDesign.PortDirection.Input;
                port_map.addAll(Misc.MakeObstMapFromPort(pinPortPo, dir));
            }
        }

        int starty = pos.getY() + Placer.Y_CELL_SIZE;

        int i = 0;
        List<HashSet<Pair<Integer, Integer>>> occupied_map = new ArrayList<>();
        List<HashSet<Pair<Integer, Integer>>> occupied_map_wire = new ArrayList<>();
        List<BlockPos> rep_map = new ArrayList<>();
        g_rep_map = rep_map;
        ocm = occupied_map;
        ocm_wire = occupied_map_wire;
        cached_hy = hy;
        cached_tpn = tpn;
        for (HyperGraphNet hp : hy)
        {
            ObstFixAndFindPositionHypergraph(hp, w, obm, i, hy.size());
            System.out.println("Building hypergraph " + (i + 1) + " of " + hy.size());
            BuildHyperGraph(hp, w,  starty + hp.y_pos * 2);
            i++;
        }
        i = 0;
        for (TwoPinNet twoPinNet : tpn)
        {
            ObstFixAndFindPositionTwoPin(twoPinNet, w, obm, i, hy.size());
            System.out.println("Building two pin net " + (i + 1) + " of " + tpn.size());
            BuildTwoPinNet(twoPinNet, w, starty + twoPinNet.y_pos * 2);
            i++;
        }
        System.out.println("Building hypergraph vertical connectors");
        for (HyperGraphNet hyperGraphNet : hy)
        {
            for (int j = 0; j < hyperGraphNet.pin_port_pos.size(); j++)
            {
                BlockPos hn = hyperGraphNet.pin_port_pos.get(j);
                if (j == hyperGraphNet.out_port_pos)
                {
                    VerticalBuilder.BuildUpwards(w, hn, hn.withY(starty + hyperGraphNet.y_pos * 2), occupied_map_wire, pos, rep_map);
                }
                else
                {
                    VerticalBuilder.BuildDownwards(w, hn, hn.withY(starty + hyperGraphNet.y_pos * 2));
                }
            }
        }
        System.out.println("Building two pin vertical connectors");
        for (TwoPinNet tp : tpn)
        {
            if (tp.p1dir == JsonDesign.PortDirection.Input)
            {
                VerticalBuilder.BuildUpwards(w, tp.p2, tp.p2.withY(starty + tp.y_pos * 2), occupied_map_wire, pos, rep_map);
                VerticalBuilder.BuildDownwards(w, tp.p1, tp.p1.withY(starty + tp.y_pos * 2));
            }
            else
            {
                VerticalBuilder.BuildUpwards(w, tp.p1, tp.p1.withY(starty + tp.y_pos * 2), occupied_map_wire, pos, rep_map);
                VerticalBuilder.BuildDownwards(w, tp.p2, tp.p2.withY(starty + tp.y_pos * 2));
            }
        }
        System.out.println("Building hypergraph net wires");
        for (HyperGraphNet hyperGraphNet : hy)
        {
            RedstoneWireBuilder.FixPointTooClose(0, hyperGraphNet);
            RedstoneWireBuilder.FixHypergraphAdjList(0, hyperGraphNet);
            RedstoneWireBuilder.BuildHypergraph(w, hyperGraphNet, rep_map, starty);
        }
        i = 0;
        System.out.println("Building two pin net wires");
        for (TwoPinNet twoPinNet : tpn)
        {
            RedstoneWireBuilder.BuildTwoPin(w, twoPinNet, rep_map, starty);
            i++;
        }

        max_y = 50;//starty + occupied_map_wire.size() * 2 + 1;
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
        int starty = Placer.last_pos.getY() + Placer.Y_CELL_SIZE;
        for (HyperGraphNet hp : cached_hy)
        {
            System.out.println("Building hypergraph " + (i + 1) + " of " + cached_hy.size());
            BuildHyperGraph(hp, w, starty + hp.y_pos);
            i++;
        }
        for (TwoPinNet twoPinNet : cached_tpn)
        {
            System.out.println("Building two pin net " + (i + 1) + " of " + cached_tpn.size());
            BuildTwoPinNet(twoPinNet, w, starty + twoPinNet.y_pos);
            i++;
        }
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

        for (BlockPos pinPortPo : hp.pin_port_pos)
        {
            w.setBlockState(pinPortPo.withY(y), Blocks.BLUE_WOOL.getDefaultState(), 2 | 816);
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

    private static void ObstFixAndFindPositionHypergraph(HyperGraphNet hyperGraphNet, ServerWorld w, ObstacleMap obm, int i, int imax)
    {
        System.out.println("Placing Hypergraph " + (i + 1) + " of " + imax);
        int y = 0;
        boolean dfs_success = false;
        do {
            System.out.println("Attempting to place Hypergraph " + (i + 1) + " at Y: " + y);
            obm.AssureY(y);
            ObstacleFixer.FixObstaclesHyperGraph(hyperGraphNet, obm, w, y);

            boolean[] vm = new boolean[hyperGraphNet.all_points.size()];

            dfs_success = DFSWireFinder.DFS_Mark(hyperGraphNet, 0, obm, 0, vm, y);
            if(dfs_success)
            {
                obm.CommitWire(y);
                obm.FullRemove(y);
            }
            else
            {
                obm.FlushTemp();
                y++;
            }
            if(y > 300)
            {
                throw new RuntimeException("ObstFixAndFindPositionHypergraph: max 300 DFS_Mark attempts exceeded");
            }
        } while(!dfs_success);
        hyperGraphNet.y_pos = y;
    }

    private static void ObstFixAndFindPositionTwoPin(TwoPinNet tpn, ServerWorld w, ObstacleMap obm, int i, int imax)
    {
        System.out.println("Placing two pin net " + (i + 1) + " of" + imax);
        int y = 0;
        boolean dfs_success = false;
        do {
            System.out.println("Attempting to place twopin " + (i + 1) + " at Y: " + y);
            obm.AssureY(y);
            ObstacleFixer.FixObstacleTwoPinNet(tpn, obm, y);
            dfs_success = DFSWireFinder.DFS_Mark_tpn(tpn, obm ,y);
            if(dfs_success)
            {
                obm.CommitWire(y);
                obm.FullRemove(y);
            }
            else
            {
                obm.FlushTemp();
                y++;
            }
            if(y > 300)
            {
                throw new RuntimeException("ObstFixAndFindPositionTwoPin: max 300 DFS_Mark attempts exceeded");
            }
        } while(!dfs_success);
        tpn.y_pos = y;
    }

    static List<HyperGraphNet> HandleHyperGraphs(List<Map.Entry<Integer, List<AbstractCell>>> h, Map<CellInfo, BlockPos> cellmap, Map<Integer, BlockPos> port_rel_pos)
    {
        List<HyperGraphNet> ret = new ArrayList<>();
        for (Map.Entry<Integer, List<AbstractCell>> integerListEntry : h)
        {
            List<BlockPos> rel_block_pos = new ArrayList<>();
            int out_index = -1;
            for (AbstractCell abstractCell : integerListEntry.getValue())
            {
                rel_block_pos.add(RouterMisc.GetRelPos(cellmap, abstractCell, integerListEntry.getKey(), port_rel_pos));
                if (RouterMisc.GetDir(abstractCell, integerListEntry.getKey()) == JsonDesign.PortDirection.Output)
                {
                    out_index = rel_block_pos.size() - 1;
                }
            }
            if (out_index == -1)
                throw new RuntimeException("Failed to find output");

            int[] xarr = rel_block_pos.stream().map(Vec3i::getX).mapToInt(value -> value).toArray();
            int[] zarr = rel_block_pos.stream().map(Vec3i::getZ).mapToInt(value -> value).toArray();

            var treerez = flu.flute(xarr.length, xarr, zarr, Flute.ACCURACY);

            ret.add(new HyperGraphNet(integerListEntry.getKey(), treerez, integerListEntry.getValue(), rel_block_pos, out_index));
        }
        return ret;
    }

    private static List<TwoPinNet> HandleTwoPinNets(List<Map.Entry<Integer, List<AbstractCell>>> twopin, Map<CellInfo, BlockPos> cellmap, Map<Integer, BlockPos> port_rel_pos)
    {
        List<TwoPinNet> tpn = new ArrayList<>();
        for (Map.Entry<Integer, List<AbstractCell>> integerListEntry : twopin)
        {
            var np = new TwoPinNet(integerListEntry.getKey(), integerListEntry.getValue().getFirst(), integerListEntry.getValue().get(1));
            var p1 = RouterMisc.GetRelPos(cellmap, integerListEntry.getValue().getFirst(), integerListEntry.getKey(), port_rel_pos);
            var p2 = RouterMisc.GetRelPos(cellmap, integerListEntry.getValue().get(1), integerListEntry.getKey(), port_rel_pos);
            np.p1 = p1;
            np.p2 = p2;
            np.p1dir = RouterMisc.GetDir(integerListEntry.getValue().getFirst(), integerListEntry.getKey());

            tpn.add(np);
        }
        return tpn;
    }
}
