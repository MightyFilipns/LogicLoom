package com.mightyfilipns.chipmakermc.Routing;

import com.mightyfilipns.chipmakermc.Chipmakermc;
import com.mightyfilipns.chipmakermc.JsonLoader.CellType;
import com.mightyfilipns.chipmakermc.JsonLoader.JsonDesign;
import com.mightyfilipns.chipmakermc.Placment.Placer;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TestCmds
{
    public static int TestTemplate(CommandContext<ServerCommandSource> context)
    {
        var t = context.getSource().getWorld().getStructureTemplateManager();
        var ct = CellType.NOT.getIdentifier();
        var opt = t.getTemplate(ct);
        var tmplt = opt.get();
        var p = BlockPosArgumentType.getBlockPos(context, "pos");
        var pld = new StructurePlacementData();
        tmplt.place(context.getSource().getWorld(), p, null, pld,null, 3);
        return 1;
    }

    public static int TestLeeRouter(CommandContext<ServerCommandSource> context)
    {
        HashSet<Pair<Integer, Integer>> mp = new HashSet<>();
        mp.add(Pair.of(12, 7));
        mp.add(Pair.of(13, 7));
        mp.add(Pair.of(12, 8));
        mp.add(Pair.of(13, 8));
        mp.add(Pair.of(12, 9));
        mp.add(Pair.of(13, 9));
        mp.add(Pair.of(12, 10));
        mp.add(Pair.of(13, 10));
        mp.add(Pair.of(12, 11));
        mp.add(Pair.of(13, 11));
        mp.add(Pair.of(12, 12));
        mp.add(Pair.of(13, 12));
        mp.add(Pair.of(12, 13));
        mp.add(Pair.of(13, 13));
        mp.add(Pair.of(12, 14));
        mp.add(Pair.of(13, 14));

        mp.add(Pair.of(16, 4));
        mp.add(Pair.of(17, 4));
        mp.add(Pair.of(18, 4));
        mp.add(Pair.of(19, 4));
        mp.add(Pair.of(16, 5));
        mp.add(Pair.of(17, 5));
        mp.add(Pair.of(18, 5));
        mp.add(Pair.of(19, 5));
        mp.add(Pair.of(16, 6));
        mp.add(Pair.of(17, 6));
        mp.add(Pair.of(18, 6));
        mp.add(Pair.of(19, 6));
        mp.add(Pair.of(16, 7));
        mp.add(Pair.of(17, 7));
        mp.add(Pair.of(18, 7));
        mp.add(Pair.of(19, 7));

        mp.add(Pair.of(19, 12));
        mp.add(Pair.of(19, 13));
        mp.add(Pair.of(20, 12));
        mp.add(Pair.of(20, 13));
        mp.add(Pair.of(21, 12));
        mp.add(Pair.of(21, 13));
        mp.add(Pair.of(22, 12));
        mp.add(Pair.of(22, 13));
        mp.add(Pair.of(23, 12));
        mp.add(Pair.of(23, 13));

        BlockPos start = new BlockPos(14 ,0 , 9);
        BlockPos endp = new BlockPos(22 ,0 , 14);

        // var res = LeeRouter.DoLeeRouter(mp, start, endp);
        return 1;
    }


    public static int TestHyperGraph(CommandContext<ServerCommandSource> context)
    {
        var des = Chipmakermc.loaded_design;
        var mod = (JsonDesign.DesignModule)des.modules.values().toArray()[0];
        Router.DoRouting(context, mod, Placer.g_mp, Placer.rel_port_pos);
        return 1;
    }

    public static int RebuildCached(CommandContext<ServerCommandSource> context)
    {
        Router.RebuildCache(context);
        return 1;
    }

    public static int BuildWire(CommandContext<ServerCommandSource> context)
    {
        int index = IntegerArgumentType.getInteger(context, "index");
        // HyperGraphNet h = Router.cached_hy.get(index);
        //RedstoneWireBuilder.FixHypergraphAdjList(0, h);
        //RedstoneWireBuilder.BuildHypergraph(context.getSource().getWorld(), h, Router.g_rep_map, NewPlacer.last_pos.getY() + NewPlacer.Y_CELL_SIZE);
        TwoPinNet tpn = Router.cached_tpn.get(index);
        RedstoneWireBuilder.BuildTwoPin(context.getSource().getWorld(), tpn, Placer.last_pos.getY() + Placer.Y_MAX_CELL_SIZE);
        return 1;
    }

    public static int TestTree(CommandContext<ServerCommandSource> context)
    {
        List<BlockPos> testp = new ArrayList<>();
        testp.add(new BlockPos(6,0,6));
        testp.add(new BlockPos(5,0,5));
        testp.add(new BlockPos(2,0,7));
        testp.add(new BlockPos(1,0,4));
        testp.add(new BlockPos(0,0,2));
        testp.add(new BlockPos(3,0,2));
        testp.add(new BlockPos(5,0,0));

        BlockPos pos = BlockPosArgumentType.getBlockPos(context, "start_pos");

        List<List<Integer>> d = new ArrayList<>();

        int[] xarr = testp.stream().map(Vec3i::getX).mapToInt(value -> value).toArray();
        int[] zarr = testp.stream().map(Vec3i::getZ).mapToInt(value -> value).toArray();

        var fr = Router.flu.flute(xarr.length, xarr, zarr, Flute.ACCURACY);

        // flu.printtree(fr);

        for (int i = 0; i < fr.deg; i++)
        {
            context.getSource().getWorld().setBlockState(pos.add(fr.branch[i].x, 0, fr.branch[i].y), Blocks.LIME_WOOL.getDefaultState());
        }

        for (int i = fr.deg; i < 2 * fr.deg - 2; i++)
        {
            context.getSource().getWorld().setBlockState(pos.add(fr.branch[i].x, 1, fr.branch[i].y), Blocks.BLUE_WOOL.getDefaultState());
        }
        /*
        var p = FindMinimumSteinerRectilinearTree(testp, d);
        for (BlockPos blockPos : p.stream().map(Pair::getLeft).toList())
        {
            context.getSource().getWorld().setBlockState(pos.add(blockPos), Blocks.LIME_WOOL.getDefaultState());
        }*/
        return 1;
    }

    public static int TestVerticalBuilder(CommandContext<ServerCommandSource> context)
    {
        BlockPos d = BlockPosArgumentType.getBlockPos(context, "down");
        BlockPos p = BlockPosArgumentType.getBlockPos(context, "up");
        List<BlockPos> m = new ArrayList<>();
        VerticalBuilder.BuildDownwards(context.getSource().getWorld(), d, p);
        return 1;
    }
}
