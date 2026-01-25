package com.mightyfilipns.chipmakermc.Placment;

import Jama.Matrix;
import com.mightyfilipns.chipmakermc.*;
import com.mightyfilipns.chipmakermc.JsonLoader.AbstractCell;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;

import static com.mightyfilipns.chipmakermc.Placer.*;
import static com.mightyfilipns.chipmakermc.Placment.PlacerMisc.*;
import static java.lang.Math.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class NewPlacer
{
    public final static int Z_CELL_SIZE = 7;
    public final static int X_CELL_SIZE = 6;
    public final static int Y_CELL_SIZE = 4;

    public static int PlaceDesign(CommandContext<ServerCommandSource> context)
    {
        var des = Chipmakermc.loaded_design;
        var mod = (JsonDesign.DesignModule)des.modules.values().toArray()[0];

        int avaliable_area = chip_size * chip_size;
        int min_area = mod.cells.size() * CELL_AREA;
        if (min_area > avaliable_area)
        {
            context.getSource().sendError(Text.literal(String.format("Build area too small: Avaible area (X: %d * Z: %d) = %d : Minimum area: %d", chip_size, chip_size, avaliable_area, min_area)));
            return 0;
        }

        DoPlace(context, mod);

        VCDHandler.SetMap(mod);
        return 1;
    }

    private static void DoPlace(CommandContext<ServerCommandSource> context, JsonDesign.DesignModule mod)
    {
        Matrix c_x = new Matrix(mod.cells.size(), 1);
        Matrix c_z = new Matrix(mod.cells.size(), 1);

        var pr = ConnMatrixBuilder.GetConnectivityMatrixHessian2();
        Matrix connection_m = pr.getLeft();

        var pos = BlockPosArgumentType.getBlockPos(context, "start_pos");
        var w = context.getSource().getWorld();

        int x_counter = 0;

        var abs_port_pos = new HashMap<>();

        SetupPorts(mod, x_counter, pos, w, pr, abs_port_pos, c_x, c_z, connection_m);

        // TODO: add size for x and z

        //AddFixed(connection_m, c_x, c_z, 0,             chip_size, 0, 2);
        //AddFixed(connection_m, c_x, c_z, chip_size / 2, chip_size , 0 ,2);
        //AddFixed(connection_m, c_x, c_z,  chip_size,          chip_size, 0, 2);
        // AddFixedVirtualCell(connection_m, c_x, c_z,  chip_size / 2, chip_size /2, 1);
        //AddFixedVirtualCell(connection_m, c_x, c_z,  chip_size, chip_size / 2, Double.MIN_VALUE);

        //AddFixedVirtualCell(connection_m, c_x, c_z,  chip_size, 0);
        AddFixedVirtualCell(connection_m, c_x, c_z,  (chip_size / X_CELL_SIZE) / 2 , (chip_size/ Z_CELL_SIZE) / 2);
        //AddFixedVirtualCell(connection_m, c_x, c_z, 0, chip_size);
        // AddFixedVirtualCell(connection_m, c_x, c_z, 0, 0);

        Matrix x_sol = null;
        Matrix z_sol = null;

        Matrix f_x = new Matrix(mod.cells.size(), 1);
        Matrix f_z = new Matrix(mod.cells.size(), 1);

        int iter = 0;
        boolean has_overlap = false;
        boolean obb = false;
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
                MarkOverlapPos(d, context, 1);
                break;
            }

            x_sol = connection_m.solve(c_x.plus(f_x));
            z_sol = connection_m.solve(c_z.plus(f_z));

            xsa = RoundMtxSol(x_sol, X_CELL_SIZE);
            zsa = RoundMtxSol(z_sol, Z_CELL_SIZE);

            has_overlap = CheckOverlap(xsa, zsa);

            System.out.println("Iter: " + iter);

            // obb = FixOutOfBounds(x_sol, z_sol, f_x, f_z, xsa, zsa);

            if(has_overlap)
            {
                FixOverLapPossion(x_sol ,z_sol, f_x, f_z);
            }

            iter++;
        } while(has_overlap || obb);

        PlaceDebug(xsa, zsa, context, 0);
    }

    static final int CELL_AREA = X_CELL_SIZE * Z_CELL_SIZE;

    private static void PlaceDebug(int[] xvec, int[] zvec, CommandContext<ServerCommandSource> context, int y_offset)
    {
        BlockPos pos = BlockPosArgumentType.getBlockPos(context, "start_pos");
        for (int i = 0; i < xvec.length; i++)
        {
            int x_offset = ClampVal(xvec[i], chip_size);
            int z_offset = ClampVal(zvec[i], chip_size);
            BlockState defaultState = x_offset != xvec[i] || z_offset != zvec[i] ? Blocks.YELLOW_WOOL.getDefaultState() : Blocks.LIGHT_BLUE_WOOL.getDefaultState();
            for (int x = 0; x < X_CELL_SIZE; x++)
            {
                for (int z = 0; z < Z_CELL_SIZE; z++)
                {
                    context.getSource().getWorld().setBlockState(pos.add(x_offset + x, y_offset, z_offset + z), defaultState);
                }
            }
        }
    }

    private static int ClampVal(int xvec, int chipsz)
    {
        return max(min(xvec, chipsz), 0);
    }

    private static void FixOverLapPossion(Matrix xsol, Matrix zsol, Matrix f_x, Matrix f_z)
    {
        HashMap<Pair<Integer, Integer>, Integer> density = new HashMap<>();
        for (int x = 0; x < xsol.getRowDimension(); x++)
        {
            double xv = xsol.get(x,0);
            double zv = zsol.get(x,0);
            var pr = Pair.of((int)xv /X_CELL_SIZE, (int)zv/Z_CELL_SIZE);
            var dn = density.computeIfAbsent(pr, (a) -> 0);
            dn++;
            density.put(pr, dn);
        }
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
                var pr = Pair.of((int)x2 / X_CELL_SIZE, (int)z2 / Z_CELL_SIZE);
                var ld = density.getOrDefault(Pair.of(pr), 0);
                double euclidean_dist = ((x - x2) * (x - x2)) + ((z - z2) * (z - z2));
                force_sumx += (ld * (x - x2)) / euclidean_dist;
                force_sumz += (ld * (z - z2)) / euclidean_dist;
            }
            AddAtMatrix(f_x, i, 0, force_sumx * force_mul);
            AddAtMatrix(f_z, i, 0, force_sumz * force_mul);
        }
    }

    private static boolean FixOutOfBounds(Matrix xsol, Matrix zsol, Matrix f_x, Matrix f_z, int[] xsa, int[] zsa)
    {
        boolean found = false;
        for (int i = 0; i < xsol.getRowDimension(); i++)
        {
            int xc = xsa[i];
            int zc = zsa[i];
            int redirx = 0;
            int redirz = 0;
            if (xc < 0)
                redirx = abs(xc);
            if (xc > chip_size)
                redirx = xc - chip_size;
            if (zc < 0)
                redirz = abs(zc);
            if (zc > chip_size)
                redirz = zc - chip_size;

            double fx = redirx != 0 ? redirx : 0;
            double fz = redirz != 0 ? redirz : 0;
            if(fx != 0 || fz != 0)
            {
                found = true;
            }
            AddAtMatrix(f_x, i, 0, -fx);
            AddAtMatrix(f_z, i, 0, -fz);
        }
        return found;
    }

    private static void MarkOverlapPos(List<Pair<Integer, Integer>> pos, CommandContext<ServerCommandSource> context, int y)
    {
        var posw = BlockPosArgumentType.getBlockPos(context, "start_pos");
        var w = context.getSource().getWorld();
        for (Pair<Integer, Integer> po : pos)
        {
            w.setBlockState(posw.add(po.getLeft(), y, po.getRight()), Blocks.RED_WOOL.getDefaultState());
        }
    }

    private static List<Pair<Integer, Integer>> GetOverlapPos(int[] x, int [] z)
    {
        var ret = new HashMap<Pair<Integer, Integer>, List<Integer>>();

        for (int i = 0; i < x.length; i++)
        {
            var xstart = x[i];
            var zstart = z[i];
            for (int j = 0; j < X_CELL_SIZE; j++)
            {
                for (int i1 = 0; i1 < Z_CELL_SIZE; i1++)
                {
                    var p = Pair.of(ClampVal(xstart + j, chip_size), ClampVal(zstart + i1, chip_size));
                    var v = ret.computeIfAbsent(p, a -> new ArrayList<>());
                    v.add(i);
                }
            }
        }

        return ret.entrySet().stream().filter(a -> a.getValue().size() > 1).map(Map.Entry::getKey).toList();
    }

    private static boolean CheckOverlap(int[] x, int [] z)
    {
        var ret = new HashSet<Pair<Integer, Integer>>();

        for (int i = 0; i < x.length; i++)
        {
            var xstart = x[i];
            var zstart = z[i];
            for (int j = 0; j < X_CELL_SIZE; j++)
            {
                for (int i1 = 0; i1 < Z_CELL_SIZE; i1++)
                {
                    if(!ret.add(Pair.of(xstart + j, zstart + i1)))
                        return true;
                }
            }
        }

        return false;
    }

    private static void SetupPorts(JsonDesign.DesignModule mod, int x_counter, BlockPos pos, ServerWorld w, Triple<Matrix, Map<Integer, List<AbstractCell>>, List<Pair<AbstractCell, AbstractCell>>> pr, HashMap<Object, Object> abs_port_pos, Matrix c_x, Matrix c_z, Matrix connection_m)
    {
        int z = -1;
        for (Map.Entry<String, JsonDesign.DesignPortInfo> value : mod.ports.entrySet())
        {
            for (Integer bit : value.getValue().bits)
            {
                int x = x_counter * 3;
                int xworldpos = x_counter * X_CELL_SIZE;

                var npos = pos.add(xworldpos,0,z);

                SetPortSignAndBlock(w, value, bit, npos);

                var conn = pr.getMiddle().get(bit).stream().mapToInt(a -> a.cell_ID).toArray();
                abs_port_pos.put(bit, npos);
                for (int i : conn)
                {
                    AddFixed(connection_m, c_x, c_z, x , z, i, 1);

                    /*
                    AddAtMatrix(c_x, i, 0, x);
                    AddAtMatrix(c_z, i, 0, z);
                    AddAtMatrix(connection_m, i, i, 1.1D);*/
                }

                x_counter += 1;
            }
        }
    }

    private static void SetPortSignAndBlock(ServerWorld w, Map.Entry<String, JsonDesign.DesignPortInfo> value, Integer bit, BlockPos npos)
    {
        w.setBlockState(npos, Blocks.REDSTONE_WIRE.getDefaultState());
        w.setBlockState(npos.add(0, 0, -1), value.getValue().direction == JsonDesign.PortDirection.Input ?  Blocks.RED_WOOL.getDefaultState() : Blocks.REDSTONE_LAMP.getDefaultState());
        w.setBlockState(npos.add(0, 1, -1), Blocks.OAK_SIGN.getDefaultState().with(SignBlock.ROTATION, 8));
        ((SignBlockEntity) w.getBlockEntity(npos.add(0,1,-1))).setText(new SignText().withMessage(1, Text.of(String.format("%s - %d", value.getKey(), bit))), true);
    }
}
