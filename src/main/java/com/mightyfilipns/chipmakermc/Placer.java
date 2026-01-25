package com.mightyfilipns.chipmakermc;

import Jama.Matrix;
import com.mightyfilipns.chipmakermc.Placment.NewPlacer;
import com.mightyfilipns.chipmakermc.Placment.PlacerMisc;
import com.mightyfilipns.chipmakermc.Routing.RoutingPrep;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.CuboidBlockIterator;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static java.lang.Math.*;

public class Placer
{
    public final static int Z_CELL_SIZE = 7;
    public final static int X_CELL_SIZE = 6;
    public final static int Y_CELL_SIZE = 4;

    public static double force_mul = 0.04D;
    public static int max_iter = 140;
    public static int chip_size = 200;

    public static BlockPos last_pos = new BlockPos(74, -12, -189);

    public static boolean do_vertical = false;
    public static boolean do_overlap_fix_final = true;

    public static boolean do_actual_place = true;

    public static HashMap<Integer, BlockPos> rel_port_pos = null;
    public static Map<CellInfo, BlockPos> g_mp = null;
    static int[] g_xsa = null;
    static int[] g_zsa = null;
    public static int PlaceDesign(CommandContext<ServerCommandSource> context)
    {
        var des = Chipmakermc.loaded_design;
        var mod = (JsonDesign.DesignModule)des.modules.values().toArray()[0];

        NewPlacer.PlaceDesign(context);

        // DoSimplePlacer2(context, mod);

        VCDHandler.SetMap(mod);
        return 1;
    }

    public static int PlaceCache(CommandContext<ServerCommandSource> context)
    {
        if(g_zsa == null || g_xsa == null || g_mp == null)
        {
            context.getSource().sendError(Text.literal("Cache empty"));
            return 0;
        }

        var des = Chipmakermc.loaded_design;
        var mod = (JsonDesign.DesignModule)des.modules.values().toArray()[0];

        PlaceMtx(g_xsa, g_zsa, context, 0, mod, g_mp);
        RoutingPrep.SetupCellPorts(context, g_mp);

        return 1;
    }

    static void AddAtMatrix(Matrix mtx, int i, int j, int val)
    {
        mtx.set(i,j,mtx.get(i, j) + val);
    }

    static void AddAtMatrix(Matrix mtx, int i, int j, double val)
    {
        mtx.set(i,j,mtx.get(i, j) + val);
    }

    static void DoSimplePlacer2(CommandContext<ServerCommandSource> context, JsonDesign.DesignModule mod)
    {
        Matrix c_x = new Matrix(mod.cells.size(), 1);
        Matrix c_z = new Matrix(mod.cells.size(), 1);

        var pr = ConnMatrixBuilder.GetConnectivityMatrixHessian2();
        Matrix connection_m = pr.getLeft();

        var pos = BlockPosArgumentType.getBlockPos(context, "start_pos");
        var w = context.getSource().getWorld();
        last_pos = pos;

        int x_counter = 0;

        rel_port_pos = new HashMap<>();

        for (Map.Entry<String, JsonDesign.DesignPortInfo> value : mod.ports.entrySet())
        {
            for (Integer bit : value.getValue().bits)
            {
                int z = -1;
                int x = x_counter * 3;
                int xworldpos = x_counter * X_CELL_SIZE;
                var npos = pos.add(xworldpos,0,z);
                w.setBlockState(npos, Blocks.REDSTONE_WIRE.getDefaultState());
                w.setBlockState(npos.add(0, 0, -1),value.getValue().direction == JsonDesign.PortDirection.Input ?  Blocks.RED_WOOL.getDefaultState() : Blocks.REDSTONE_LAMP.getDefaultState());
                w.setBlockState(npos.add(0, 1, -1), Blocks.OAK_SIGN.getDefaultState().with(SignBlock.ROTATION, 8));
                ((SignBlockEntity)w.getBlockEntity(npos.add(0,1,-1))).setText(new SignText().withMessage(1, Text.of(String.format("%s - %d", value.getKey(), bit))), true);
                var conn = pr.getMiddle().get(bit).stream().mapToInt(a -> a.cell_ID).toArray();
                rel_port_pos.put(bit, npos);
                for (int i : conn)
                {
                    AddAtMatrix(c_x, i, 0, x);
                    AddAtMatrix(c_z, i, 0, z);
                    AddAtMatrix(connection_m, i, i, 1);
                }

                x_counter += 1;
            }
        }

        // Add virtual cells at all corners except at 0,0
        AddFixedVirtualCell(connection_m, c_x, c_z,  chip_size, 0);
        AddFixedVirtualCell(connection_m, c_x, c_z,  chip_size, chip_size);
        AddFixedVirtualCell(connection_m, c_x, c_z, 0, chip_size);
        AddFixedVirtualCell(connection_m, c_x, c_z, 0, 0);

        Matrix x_sol = null;
        Matrix z_sol = null;

        Matrix f_x = new Matrix(mod.cells.size(), 1);
        Matrix f_z = new Matrix(mod.cells.size(), 1);

        boolean has_overlap = false;

        int iter = 0;

        int[] xsa = null;
        int[] zsa = null;
        Map<CellInfo, BlockPos> mp = new HashMap<>();
        do
        {
            // Sanity check
            if(iter > max_iter)
            {
                var d = GetOverlapPos(xsa, zsa);
                context.getSource().sendFeedback(() -> Text.literal("Max iter reached - " + max_iter + " Overlaps: " + d.size()), false);
                if(!do_overlap_fix_final)
                    MarkOverlapPos(d, context, do_vertical ? iter : 1);
                break;
            }

            x_sol = connection_m.solve(c_x.plus(f_x));
            z_sol = connection_m.solve(c_z.plus(f_z));

            xsa = RoundMtxSol(x_sol);
            zsa = RoundMtxSol(z_sol);

            has_overlap = CheckOverlap(xsa, zsa);

            System.out.println("Iter: " + iter);

            if(has_overlap)
            {
                FixOverLapPossion(x_sol ,z_sol, f_x, f_z);
            }

            if(do_vertical)
            {
                PlaceMtx(xsa, zsa, context, iter, mod, mp);
            }
            iter++;
        } while(has_overlap);

        int finalIter = iter;
        context.getSource().sendFeedback(() -> Text.literal("Total iter - " + finalIter), false);

        if (do_overlap_fix_final)
        {
            FixOverlapFinal(xsa, zsa);
        }

        if(!do_vertical)
        {
            PlaceMtx(xsa, zsa, context, 0, mod, mp);
            RoutingPrep.SetupCellPorts(context, mp);
        }
        g_mp = mp;
        g_xsa = xsa;
        g_zsa = zsa;
    }

    static void FixOverlapFinal(int[] xsol, int[] zsol)
    {
        var m = MakeCellMap(xsol, zsol);
        var l = m.entrySet().stream().filter(a -> a.getValue().size() > 1).toList();
        for (Map.Entry<Pair<Integer, Integer>, List<Integer>> pl : l)
        {
            int x = pl.getKey().getLeft();
            int z = pl.getKey().getRight();
            for (Integer i : pl.getValue().stream().skip(1).toList())
            {
                if(!m.containsKey(Pair.of(x + 1, z)))
                {
                    xsol[i] = x + 1;
                    zsol[i] = z;
                    m.put(Pair.of(x + 1, z), new ArrayList<>());
                }
                else if(!m.containsKey(Pair.of(x, z + 1)))
                {
                    xsol[i] = x;
                    zsol[i] = z + 1;
                    m.put(Pair.of(x, z + 1), new ArrayList<>());
                }
                else if(!m.containsKey(Pair.of(x - 1, z)))
                {
                    xsol[i] = x - 1;
                    zsol[i] = z;
                    m.put(Pair.of(x - 1, z), new ArrayList<>());
                }
                else if(!m.containsKey(Pair.of(x, z - 1)))
                {
                    xsol[i] = x;
                    zsol[i] = z - 1;
                    m.put(Pair.of(x, z - 1), new ArrayList<>());
                }
                else
                {
                    throw new RuntimeException("Failed to resolve overlap");
                }
            }

            /*
            int dir = 0;
            int cx = 0;
            int cy = 0;
            int c_len = 0;
            while(true)
            {
                switch (dir)
                {
                    case 0: cx++; break;
                    case 1: cy++; break;
                    case 2: cx--; break;
                    case 3: cy--; break;
                }
            }*/
        }
    }

    static void PlaceMtx(int[] xvec, int[] zvec, CommandContext<ServerCommandSource> context, int y_offset, JsonDesign.DesignModule mod, Map<CellInfo, BlockPos> mp)
    {
        List<CellInfo> v = mod.cells.values().stream().sorted(Comparator.comparingInt(a -> a.cell_ID)).toList();
        BlockPos pos = BlockPosArgumentType.getBlockPos(context, "start_pos");
        for (int i = 0; i < xvec.length; i++)
        {
            int x_offset = max(min(xvec[i], chip_size), 0);
            int z_offset = max(min(zvec[i], chip_size), 0);
            BlockState defaultState = x_offset != xvec[i] || z_offset != zvec[i] ? Blocks.YELLOW_WOOL.getDefaultState() : Blocks.LIGHT_BLUE_WOOL.getDefaultState();
            if(do_actual_place)
            {
                PlaceCellAt(x_offset, z_offset, i, v, context, pos, mp);
            }
            else
            {
                context.getSource().getWorld().setBlockState(pos.add(x_offset,y_offset, z_offset), defaultState);
            }
        }
    }

    static void PlaceCellAt(int xoff, int zoff, int i, List<CellInfo> cil, CommandContext<ServerCommandSource> context, BlockPos pos, Map<CellInfo, BlockPos> mp)
    {
        var ci = cil.get(i);
        var model_pos = Chipmakermc.celltable.get(ci.type);

        BlockPos paste_pos = pos.add(X_CELL_SIZE*xoff,0,Z_CELL_SIZE*zoff);
        mp.put(ci, paste_pos);
        var w = context.getSource().getWorld();
        for (int x = 0; x < X_CELL_SIZE; x++) {
            for (int y = 0; y < Y_CELL_SIZE; y++) {
                for (int z = 0; z < Z_CELL_SIZE; z++) {
                   w.setBlockState(paste_pos.add(x,y,z), w.getBlockState(model_pos.add(x,y,z)));
                }
            }
        }
    }

    static int[] RoundMtxSol(Matrix d)
    {
        return Arrays.stream(d.getRowPackedCopy()).mapToInt(a -> (int) round(a)).toArray();
    }

    static void MarkOverlapPos(List<Pair<Integer, Integer>> pos, CommandContext<ServerCommandSource> context, int y)
    {
        var posw = BlockPosArgumentType.getBlockPos(context, "start_pos");
        var w = context.getSource().getWorld();
        for (Pair<Integer, Integer> po : pos)
        {
            w.setBlockState(posw.add(po.getLeft(), y, po.getRight()), Blocks.RED_WOOL.getDefaultState());
        }
    }

    static List<Pair<Integer, Integer>> GetOverlapPos(int[] x, int [] z)
    {
        var ret = new HashMap<Pair<Integer, Integer>, List<Integer>>();

        for (int i = 0; i < x.length; i++)
        {
            var p = Pair.of(x[i], z[i]);
            var v = ret.computeIfAbsent(p, a -> new ArrayList<>());
            v.add(i);
        }

        return ret.entrySet().stream().filter(a -> a.getValue().size() > 1).map(Map.Entry::getKey).toList();
    }

    static boolean CheckOverlap(int[] x, int [] z)
    {
        var ret = new HashSet<Pair<Integer, Integer>>();

        for (int i = 0; i < x.length; i++)
        {
            var p = Pair.of(x[i], z[i]);
            if(ret.contains(p))
                return true;
            ret.add(p);
        }

        return false;
    }

    static Map<Pair<Integer, Integer>, List<Integer>> MakeCellMap(int[] x, int [] z)
    {
        var ret = new HashMap<Pair<Integer, Integer>, List<Integer>>();

        for (int i = 0; i < x.length; i++)
        {
            var p = Pair.of(x[i], z[i]);
            var v = ret.computeIfAbsent(p, a -> new ArrayList<>());
            v.add(i);
        }

        return ret;
    }

    static void FixOverLapPossion(Matrix xsol, Matrix zsol, Matrix f_x, Matrix f_z)
    {
        for (int i = 0; i < xsol.getRowDimension(); i++)
        {
            double x = xsol.get(i,0);
            double z = zsol.get(i,0);

            double force_sumx = 0;
            double force_sumz = 0;
            for (int j = 0; j < xsol.getRowDimension(); j++)
            {
                if(i == j)
                    continue;
                double x2 = xsol.get(j,0);
                double z2 = zsol.get(j,0);
                double euclidean_dist = ((x - x2) * (x - x2)) + ((z - z2) * (z - z2));
                force_sumx += (x - x2) / euclidean_dist;
                force_sumz += (z - z2) / euclidean_dist;
            }
            AddAtMatrix(f_x, i, 0, force_sumx * force_mul);
            AddAtMatrix(f_z, i, 0, force_sumz * force_mul);
        }
    }


    static void AddFixedVirtualCell(Matrix q, Matrix c_x, Matrix c_z, int rel_x, int rel_z)
    {
        for (int i = 0; i < q.getRowDimension(); i++)
        {
            AddAtMatrix(c_x, i, 0, rel_x);
            AddAtMatrix(c_z, i, 0, rel_z);
            AddAtMatrix(q, i ,i , 1);
        }
    }
/*
    static boolean FixOverLap(Map<Pair<Integer, Integer>, List<Integer>> map, Matrix f_x, Matrix f_z)
    {
        // Find cells with overlap
        var l = map.entrySet().stream().filter(a -> a.getValue().size() > 1).toList();
        for (Map.Entry<Pair<Integer, Integer>, List<Integer>> pairListEntry : l)
        {
            int dir = 0;
            for (int i = 0; i < pairListEntry.getValue().size(); i++)
            {
                int val = pairListEntry.getValue().get(i);
                switch (dir)
                {
                    case 0: AddAtMatrix(f_x, val, 0, force_mul); dir++; break;
                    case 1: AddAtMatrix(f_z, val, 0, force_mul); dir++; break;
                    case 2: AddAtMatrix(f_x, val, 0, -force_mul); dir++; break;
                    case 3: AddAtMatrix(f_z, val, 0, -force_mul); dir = 0; break;
                    default: throw new RuntimeException("invalid dir");
                }
            }
        }
        return !l.isEmpty();

    static void DoSimplePlacer(CommandContext<ServerCommandSource> context, Matrix connection_m, int matrix_size)
    {
        Matrix diagonal_sums = new Matrix(matrix_size, matrix_size);
        for (int i = 0; i < matrix_size; i++)
        {
            double row_sum = 0;
            for (int j = 0; j < matrix_size; j++)
            {
                row_sum += connection_m.get(j, i);
            }
            diagonal_sums.set(i, i, row_sum);
        }

        Matrix B = diagonal_sums.minus(connection_m);

        var e2 = B.eig().getRealEigenvalues();

        var mat_identity = Matrix.identity(matrix_size, matrix_size);
        var mat1 = B.minus(mat_identity.times(e2[1]));

        var ns1 = Arrays.stream(GetNullSpace(mat1)).map(a -> a*force_mul).mapToInt(a -> (int) round(a));

        var mat2 = B.minus(mat_identity.times(e2[2]));
        var ns2 = Arrays.stream(GetNullSpace(mat2)).map(a -> a*force_mul).mapToInt(a -> (int) round(a));

        var xvec = ns1.toArray();
        var zvec = ns2.toArray();

        int max_x = Integer.max(Arrays.stream(xvec).map(a -> a *-1).max().getAsInt(), 0);
        int max_z = Integer.max(Arrays.stream(zvec).map(a -> a *-1).max().getAsInt(), 0);

        for (int i = 0; i < matrix_size; i++)
        {
            int x_offset = Integer.min(xvec[i], 100) + max_x;
            int z_offset = Integer.min(zvec[i], 100) + max_z;
            if(xvec[i] > 100 || zvec[i] > 100)
            {
                context.getSource().getWorld().setBlockState(BlockPosArgumentType.getBlockPos(context, "start_pos").add(x_offset,0, z_offset), Blocks.LIGHT_BLUE_WOOL.getDefaultState());
            }
            else
            {
                context.getSource().getWorld().setBlockState(BlockPosArgumentType.getBlockPos(context, "start_pos").add(x_offset,0, z_offset), Blocks.YELLOW_WOOL.getDefaultState());
            }
        }
    }

    static double[] GetNullSpace(Matrix mat1)
    {
        var svd = new SingularValueDecomposition(mat1);
        var sing_val = svd.getSingularValues();
        for (int i = 0; i < sing_val.length; i++)
        {
            if(abs(sing_val[i]) < 1e-2)
            {
                var nullspace = svd.getV().getMatrix(0, svd.getV().getRowDimension() - 1, i, i);
                return Arrays.stream(nullspace.getColumnPackedCopy()).toArray();
            }
        }
        throw new RuntimeException("Failed to find null space");
    }
    */
}
