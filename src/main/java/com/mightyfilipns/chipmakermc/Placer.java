package com.mightyfilipns.chipmakermc;

import com.mightyfilipns.chipmakermc.JsonLoader.CellInfo;
import com.mightyfilipns.chipmakermc.JsonLoader.JsonDesign;
import com.mightyfilipns.chipmakermc.Misc.VCDHandler;
import com.mightyfilipns.chipmakermc.Placment.NewPlacer;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.*;

import static java.lang.Math.*;

public class Placer {
    public final static int Z_CELL_SIZE = 7;
    public final static int X_CELL_SIZE = 6;
    public final static int Y_CELL_SIZE = 4;

    public static double force_mul = 0.05D;
    public static int max_iter = 137;
    public static int chip_size = 400;

    public static BlockPos last_pos = new BlockPos(74, -12, -189);

    public static boolean do_vertical = false;
    public static boolean do_overlap_fix_final = true;

    public static boolean do_actual_place = true;

    public static HashMap<Integer, BlockPos> rel_port_pos = null;
    public static Map<CellInfo, BlockPos> g_mp = null;
    public static int[] g_xsa = null;
    public static int[] g_zsa = null;

    public static int PlaceDesign(CommandContext<ServerCommandSource> context)
    {
        var des = Chipmakermc.loaded_design;
        var mod = (JsonDesign.DesignModule) des.modules.values().toArray()[0];

        NewPlacer.PlaceDesign(context);

        VCDHandler.SetMap(mod);
        return 1;
    }

    public static int PlaceCache(CommandContext<ServerCommandSource> context)
    {
        if (g_zsa == null || g_xsa == null || g_mp == null) {
            context.getSource().sendError(Text.literal("Cache empty"));
            return 0;
        }

        var des = Chipmakermc.loaded_design;
        var mod = (JsonDesign.DesignModule) des.modules.values().toArray()[0];

        PlaceMtx(g_xsa, g_zsa, context, 0, mod, g_mp);
        // RoutingPrep.SetupCellPorts(context, g_mp);

        return 1;
    }


    static void PlaceMtx(int[] xvec, int[] zvec, CommandContext<ServerCommandSource> context, int y_offset, JsonDesign.DesignModule mod, Map<CellInfo, BlockPos> mp)
    {
        List<CellInfo> v = mod.cells.values().stream().sorted(Comparator.comparingInt(a -> a.cell_ID)).toList();
        BlockPos pos = BlockPosArgumentType.getBlockPos(context, "start_pos");
        for (int i = 0; i < xvec.length; i++) {
            int x_offset = max(min(xvec[i], chip_size), 0);
            int z_offset = max(min(zvec[i], chip_size), 0);
            BlockState defaultState = x_offset != xvec[i] || z_offset != zvec[i] ? Blocks.YELLOW_WOOL.getDefaultState() : Blocks.LIGHT_BLUE_WOOL.getDefaultState();
            if (do_actual_place) {
                PlaceCellAt(x_offset, z_offset, i, v, context, pos, mp);
            } else {
                context.getSource().getWorld().setBlockState(pos.add(x_offset, y_offset, z_offset), defaultState);
            }
        }
    }

    static void PlaceCellAt(int xoff, int zoff, int i, List<CellInfo> cil, CommandContext<ServerCommandSource> context, BlockPos pos, Map<CellInfo, BlockPos> mp)
    {
        var ci = cil.get(i);
        var model_pos = Chipmakermc.celltable.get(ci.type);

        BlockPos paste_pos = pos.add(X_CELL_SIZE * xoff, 0, Z_CELL_SIZE * zoff);
        mp.put(ci, paste_pos);
        var w = context.getSource().getWorld();
        for (int x = 0; x < X_CELL_SIZE; x++) {
            for (int y = 0; y < Y_CELL_SIZE; y++) {
                for (int z = 0; z < Z_CELL_SIZE; z++) {
                    w.setBlockState(paste_pos.add(x, y, z), w.getBlockState(model_pos.add(x, y, z)));
                }
            }
        }
    }

}
