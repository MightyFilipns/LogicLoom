package com.mightyfilipns.chipmakermc.Routing;

import com.mojang.datafixers.types.templates.Check;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RedstoneWireBuilder
{
    public static void BuildHypergraph(ServerWorld w, HyperGraphNet h, List<BlockPos> rep_map, int start_y)
    {
        int real_y = start_y + h.y_pos * 2;
        boolean[] visited = new boolean[h.adj_list.size()];
        DFS_build(h.out_port_pos, w, h, rep_map, real_y, visited);
    }

    public static void FixHypergraphAdjList(int i, HyperGraphNet h)
    {
        boolean[] visited = new boolean[h.adj_list.size()];
        List<List<Integer>> new_adj = new ArrayList<>();
        for (List<Integer> integers : h.adj_list)
        {
            new_adj.add(new ArrayList<>(integers));
        }
        visited[i] = true;
        FixHypergraphAdjList_DFS(i, h, visited, new_adj);
        h.adj_list = new_adj;
    }

    private static void FixHypergraphAdjList_DFS(int i, HyperGraphNet h, boolean[] visited, List<List<Integer>> new_adj)
    {
        var p1 = h.all_points.get(i);
        for (Integer integer : h.adj_list.get(i))
        {
            if (visited[integer])
                continue;
            visited[integer] = true;
            var p2 = h.all_points.get(integer);
            var pr = p1.subtract(p2);

            var absz = Math.abs(pr.getZ());
            // It appears that sometimes that Lee algorithm that route if obstacles are found will put Steiner point in the path of other branches in the same tree causing problems
            boolean on_x = absz != 0;
            List<BlockPos> g = null;
            if (on_x) {
                g = GetIntersectorsX(h.all_points, p1, p2);
            } else {
                g = GetIntersectorsZ(h.all_points, p1, p2);
            }
            if (g.size() != 2)
            {
                DisconnBranches(new_adj, i, integer);
                for (int j = 1; j < g.size(); j++)
                {
                    int i1 = FindIndex(h, g, j - 1);
                    int i2 = FindIndex(h, g, j);
                    if(!CheckConn(i1, h, i2))
                    {
                        ConnBranches(new_adj, i1, i2);
                    }
                }

                /*
                if(g.size() == 3)
                {
                    int mi = FindIndex(h, g, 1);
                    if(CheckConn(i, h, mi))
                    {
                        ConnBranches(new_adj, mi, integer);
                    }
                    else
                    {
                        ConnBranches(new_adj, i, mi);
                    }
                }
                else
                    throw new RuntimeException("FixHypergraphAdjList_DFS: unhandled case");*/
                // FIX
            }
            FixHypergraphAdjList_DFS(integer, h, visited, new_adj);
        }
    }

    private static boolean CheckConn(int i, HyperGraphNet h, int mi) {
        return h.adj_list.get(i).contains(mi);
    }

    private static int FindIndex(HyperGraphNet h, List<BlockPos> g, int i) {
        int mi = -1;
        for (int j = 0; j < h.all_points.size(); j++)
        {
            if (h.all_points.get(j) == g.get(i))
            {
                mi = j;
                break;
            }
        }
        if(mi == -1)
            throw new RuntimeException("Failed to find index of interfering block?????");
        return mi;
    }

    public static void ConnBranches(List<List<Integer>> adj_list, int si, int mi)
    {
        ObstacleFixer.ConnBranches(adj_list, si, mi);
    }
    public static void DisconnBranches(List<List<Integer>> adj_list, int si, int mi)
    {
        if (si == mi)
            throw new RuntimeException("Can not disconnect node from itself");

        adj_list.get(si).remove((Object)mi);
        adj_list.get(mi).remove((Object)si);
    }

    public static void DFS_build(int i, ServerWorld w, HyperGraphNet h, List<BlockPos> rep_map, int real_y, boolean[] visited)
    {
        var p1 = h.all_points.get(i);
        for (Integer integer : h.adj_list.get(i))
        {
            if (visited[integer])
                continue;
            visited[integer] = true;

            var p2 = h.all_points.get(integer);
            var pr = p1.subtract(p2);

            var absx = Math.abs(pr.getX());
            var absz = Math.abs(pr.getZ());
            var nor = new BlockPos(Integer.signum(pr.getX()), 0, Integer.signum(pr.getZ()));
            Direction dir = Direction.fromVector(nor, Direction.UP); // Intentionally invalid fall back
            // It appears that sometimes that Lee algorithm that route if obstacles are found will put Steiner point in the path of other branches in the same tree causing problems
            boolean on_x = absz != 0;
            List<BlockPos> g = null;
            List<BlockPos> rep = null;
            if (on_x) {
                g = GetIntersectorsX(h.all_points, p1, p2);
                rep = GetIntersectorsX(rep_map, p1, p2);
            } else {
                g = GetIntersectorsZ(h.all_points, p1, p2);
                rep = GetIntersectorsZ(rep_map, p1, p2);
            }
            if (g.size() == 2)
            {
                var y = real_y + 1;
                for (BlockPos blockPos : rep)
                {
                    if(blockPos.getY() == y)
                    {
                        w.setBlockState(blockPos.withY(real_y + 1), Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir));
                    }
                }

                if(absx == 1 || absz == 1)
                {
                    // immediately nex to us, skip it
                }
                else
                {
                    var repp1 = p1.withY(real_y + 1).subtract(nor);
                    var repp2 = p2.withY(real_y + 1).add(nor);

                    int i2 = 0;
                    if(absx == 0)
                    {
                        for (int j = Math.min(repp1.getZ(), repp2.getZ()); j < Math.max(repp2.getZ(), repp1.getZ()); j++)
                        {
                            if(i2 % 16 == 0)
                            {
                                i2++;
                                continue;
                            }
                            w.setBlockState(new BlockPos(repp1.getX(), repp1.getY(), j), Blocks.REDSTONE_WIRE.getDefaultState());
                            i2++;
                        }

                        for (int j = Math.min(repp1.getZ(), repp2.getZ()); j < Math.max(repp2.getZ(), repp1.getZ()); j += 16)
                        {
                            w.setBlockState(new BlockPos(repp1.getX(), repp1.getY(), j), Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir));
                        }
                    }
                    if(absz == 0)
                    {
                        for (int j = Math.min(repp1.getX(), repp2.getX()); j < Math.max(repp2.getX(), repp1.getX()); j ++)
                        {
                            if(i2 % 16 == 0)
                            {
                                i2++;
                                continue;
                            }
                            w.setBlockState(new BlockPos(j, repp1.getY(), repp1.getZ()), Blocks.REDSTONE_WIRE.getDefaultState());
                            i2++;
                        }
                        for (int j = Math.min(repp1.getX(), repp2.getX()); j < Math.max(repp2.getX(), repp1.getX()); j += 16)
                        {
                            w.setBlockState(new BlockPos(j, repp1.getY(), repp1.getZ()), Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir));
                        }
                    }
                    w.setBlockState(repp1, Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir));
                    if(h.out_port_pos != i)
                    {
                        w.setBlockState(p1.withY(real_y + 1), Blocks.REDSTONE_WIRE.getDefaultState());
                    }
                    if(h.out_port_pos != integer)
                    {
                        w.setBlockState(p2.withY(real_y + 1), Blocks.REDSTONE_WIRE.getDefaultState());
                    }
                    w.setBlockState(repp2, Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir));
                }
                DFS_build(integer, w, h, rep_map, real_y, visited);
            }
            else
            {
                int a = 0;
            }
        }
    }

    public static void BuildTwoPin(ServerWorld w, TwoPinNet tpn, List<BlockPos> rep_map, int start_y)
    {
        int real_y = start_y + tpn.y_pos * 2;
        var y = real_y + 1;
        for (int i = 1; i < tpn.point_list.size(); i++)
        {
            var p2 = tpn.point_list.get(i - 1).withY(real_y);
            var p1 = tpn.point_list.get(i).withY(real_y);
            var pr = p1.subtract(p2);

            var absx = Math.abs(pr.getX());
            var absz = Math.abs(pr.getZ());
            var nor = new BlockPos(Integer.signum(pr.getX()), 0, Integer.signum(pr.getZ()));
            Direction dir = Direction.fromVector(nor, Direction.UP); // Intentionally invalid fall back
            // It appears that sometimes that Lee algorithm that route if obstacles are found will put Steiner point in the path of other branches in the same tree causing problems
            boolean on_x = absz != 0;
            List<BlockPos> rep = null;

            if (on_x) {
                rep = GetIntersectorsX(rep_map, p1, p2);
            } else {
                rep = GetIntersectorsZ(rep_map, p1, p2);
            }

            for (BlockPos blockPos : rep)
            {
                if(blockPos.getY() == y)
                {
                    w.setBlockState(blockPos.withY(real_y + 1), Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir));
                }
            }

            if(absx == 1 || absz == 1)
            {
                // immediately nex to us, skip it
            }
            else
            {
                var repp1 = p1.withY(real_y + 1).subtract(nor);
                var repp2 = p2.withY(real_y + 1).add(nor);

                int i2 = 0;
                if(absx == 0)
                {
                    for (int j = Math.min(repp1.getZ(), repp2.getZ()); j < Math.max(repp2.getZ(), repp1.getZ()); j++)
                    {
                        if(i2 % 16 == 0)
                        {
                            i2++;
                            continue;
                        }
                        w.setBlockState(new BlockPos(repp1.getX(), repp1.getY(), j), Blocks.REDSTONE_WIRE.getDefaultState());
                        i2++;
                    }

                    for (int j = Math.min(repp1.getZ(), repp2.getZ()); j < Math.max(repp2.getZ(), repp1.getZ()); j += 16)
                    {
                        w.setBlockState(new BlockPos(repp1.getX(), repp1.getY(), j), Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir));
                    }
                }
                if(absz == 0)
                {
                    for (int j = Math.min(repp1.getX(), repp2.getX()); j < Math.max(repp2.getX(), repp1.getX()); j ++)
                    {
                        if(i2 % 16 == 0)
                        {
                            i2++;
                            continue;
                        }
                        w.setBlockState(new BlockPos(j, repp1.getY(), repp1.getZ()), Blocks.REDSTONE_WIRE.getDefaultState());
                        i2++;
                    }
                    for (int j = Math.min(repp1.getX(), repp2.getX()); j < Math.max(repp2.getX(), repp1.getX()); j += 16)
                    {
                        w.setBlockState(new BlockPos(j, repp1.getY(), repp1.getZ()), Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir));
                    }
                }
                w.setBlockState(repp1, Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir));
                w.setBlockState(p1.withY(real_y + 1), Blocks.REDSTONE_WIRE.getDefaultState());
                w.setBlockState(p2.withY(real_y + 1), Blocks.REDSTONE_WIRE.getDefaultState());
                w.setBlockState(repp2, Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir));
            }

        }
        for (int i = 0; i < tpn.point_list.size() - 1; i++)
        {
            w.setBlockState(tpn.point_list.get(i).withY(y), Blocks.REDSTONE_WIRE.getDefaultState());
        }
    }

    private static @NonNull List<BlockPos> GetIntersectorsZ(List<BlockPos> h, BlockPos p1, BlockPos p2) {
        return h.stream().filter(a -> a.getZ() == p1.getZ()).filter(a -> a.getX() >= Math.min(p1.getX(), p2.getX()) && a.getX() <= Math.max(p1.getX(), p2.getX())).sorted(Comparator.comparingInt(BlockPos::getX)).toList();
    }

    private static @NonNull List<BlockPos> GetIntersectorsX(List<BlockPos> h, BlockPos p1, BlockPos p2) {
        return h.stream().filter(a -> a.getX() == p1.getX()).filter(a -> a.getZ() >= Math.min(p1.getZ(), p2.getZ()) && a.getZ() <= Math.max(p1.getZ(), p2.getZ())).sorted(Comparator.comparingInt(BlockPos::getZ)).toList();
    }

}
