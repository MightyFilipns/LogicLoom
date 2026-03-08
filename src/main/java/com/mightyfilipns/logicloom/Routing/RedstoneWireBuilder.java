package com.mightyfilipns.logicloom.Routing;

import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RedstoneWireBuilder
{
    public static void BuildHypergraph(ServerWorld w, HyperGraphNet h, int start_y)
    {
        int real_y = start_y + h.y_pos * 2;
        boolean[] visited = new boolean[h.adj_list.size()];
        visited[h.allpoints_pos] = true;
        DFS_build(h.allpoints_pos, w, h, real_y, visited);
        for (int i = 0; i < visited.length; i++)
        {
            if(!visited[i])
            {
                System.out.println("Node not visited: " + h.all_points.get(i) + " Y: " + h.y_pos);
            }
        }
    }

    static void FixPointTooClose(int i, HyperGraphNet h)
    {
        boolean[] visited = new boolean[h.adj_list.size()];
        List<List<Integer>> new_adj = new ArrayList<>();
        for (List<Integer> integers : h.adj_list)
        {
            new_adj.add(new ArrayList<>(integers));
        }
        List<BlockPos> new_blk = new ArrayList<>(h.all_points);
        FixPointTooClose_DFS(i, h, visited, new_adj, new_blk);
        h.adj_list = new_adj;
        h.all_points = new_blk;
    }

    static void FixPointTooClose_DFS(int i, HyperGraphNet h, boolean[] visited, List<List<Integer>> new_adj, List<BlockPos> block_pos_new)
    {
        if(visited[i])
            return;
        visited[i] = true;

        var pts = h.adj_list.get(i);
        var cp = h.all_points.get(i);
        List<Pair<BlockPos, Integer>> candidates = pts.stream().map(a -> Pair.of(h.all_points.get(a).subtract(cp), a)).filter(a -> Math.abs(a.getLeft().getX()) == 1 || Math.abs(a.getLeft().getZ()) == 1).toList();
        if(candidates.size() > 1)
        {
            GetCandidates(candidates, h, i, new_adj, block_pos_new);
        }
        for (Integer integer : h.adj_list.get(i))
        {
            FixPointTooClose_DFS(integer, h, visited, new_adj, block_pos_new);
        }
    }

    static void GetCandidates(List<Pair<BlockPos, Integer>> c, HyperGraphNet h, int i, List<List<Integer>> new_adj, List<BlockPos> block_pos_new)
    {
        Pair<BlockPos, Integer> cnd1 = null;
        Pair<BlockPos, Integer> cnd2 = null;
        out:
        for (Pair<BlockPos, Integer> pr1 : c)
        {
            for (Pair<BlockPos, Integer> pr2 : c)
            {
                if(pr1 == pr2)
                    continue;
                if(AreDiagonally(pr1.getLeft(), pr2.getLeft()))
                {
                    cnd1 = pr1;
                    cnd2 = pr2;
                    break out;
                }
            }
        }

        var cnt = h.all_points.get(i);

        if(cnd1 == null)
            return;

        // the point should be connected to the origin point and to another if not ignore
        if(h.adj_list.get(cnd1.getRight()).size() != 2)
            return;
        if(h.adj_list.get(cnd2.getRight()).size() != 2)
            return;
        var op1 = h.adj_list.get(cnd1.getRight()).stream().filter(a -> a != i).findAny().get();
        var op2 = h.adj_list.get(cnd2.getRight()).stream().filter(a -> a != i).findAny().get();

        var p1 = h.all_points.get(cnd1.getRight());
        var p2 = h.all_points.get(cnd2.getRight());

        boolean along_x1 = p1.subtract(h.all_points.get(op1)).getX() != 0;
        boolean along_x2 = p2.subtract(h.all_points.get(op2)).getX() != 0;

        if(along_x2 ^ along_x1)
        {
            if(!along_x1)
            {
                // 1 - vertical
                // 2 - horizontal
                int vx = p1.getX();
                int minx = Math.min(p2.getX(), h.all_points.get(op2).getX());
                int maxx = Math.max(p2.getX(), h.all_points.get(op2).getX());
                if(vx > minx && vx < maxx)
                {
                    var rp1 = cnd1.getLeft();
                    var np = h.all_points.get(cnd2.getRight()).add(rp1);
                    System.out.println("Moving at: " + np + " Old: " + h.all_points.get(cnd2.getRight()) + " Center: " + cnt);
                    block_pos_new.set(cnd2.getRight(), np);
                    DisconnBranches(new_adj, i, cnd2.getRight());
                    ConnBranches(new_adj, cnd2.getRight(), cnd1.getRight());
                }
            }
            if(!along_x2)
            {
                // 1 - horizontal
                // 2 - vertical
                int vx = p2.getX();
                int minx = Math.min(p1.getX(), h.all_points.get(op1).getX());
                int maxx = Math.max(p1.getX(), h.all_points.get(op1).getX());
                if(vx > minx && vx < maxx)
                {
                    var rp1 = cnd1.getLeft();
                    var np = h.all_points.get(cnd2.getRight()).add(rp1);
                    System.out.println("Moving at: " + np + " Center: " + cnt);
                    block_pos_new.set(cnd2.getRight(), np);
                    DisconnBranches(new_adj, i, cnd2.getRight());
                    ConnBranches(new_adj, cnd2.getRight(), cnd1.getRight());
                }
            }
        }
    }

    static boolean AreDiagonally(BlockPos rp1, BlockPos rp2)
    {
       return !(rp1.getX() == rp2.getX() || rp1.getZ() == rp2.getZ());
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

    public static void DFS_build(int i, ServerWorld w, HyperGraphNet h, int real_y, boolean[] visited)
    {
        var p1 = h.all_points.get(i);
        for (Integer integer : h.adj_list.get(i))
        {
            if (visited[integer])
                continue;
            visited[integer] = true;

            var p2 = h.all_points.get(integer);
            var pr = p2.subtract(p1);

            var absx = Math.abs(pr.getX());
            var absz = Math.abs(pr.getZ());
            var nor = new BlockPos(Integer.signum(pr.getX()), 0, Integer.signum(pr.getZ()));
            Direction dir = Direction.fromVector(nor, Direction.UP).getOpposite(); // Intentionally invalid fall back
            // It appears that sometimes that Lee algorithm that route if obstacles are found will put Steiner point in the path of other branches in the same tree causing problems
            boolean on_x = absz != 0;
            List<BlockPos> g = null;
            if (on_x) {
                g = GetIntersectorsX(h.all_points, p1, p2);
            } else {
                g = GetIntersectorsZ(h.all_points, p1, p2);
            }
            if (g.size() == 2)
            {
                var y = real_y + 1;

                if(absx == 1 || absz == 1)
                {
                    // immediately nex to us, skip it
                }
                else
                {
                    var repp1 = p1.withY(real_y + 1).add(nor);
                    var repp2 = p2.withY(real_y + 1).subtract(nor);

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
                            w.setBlockState(new BlockPos(repp1.getX(), repp1.getY(), j), Blocks.REDSTONE_WIRE.getDefaultState(), 2 | 816);
                            i2++;
                        }

                        for (int j = Math.min(repp1.getZ(), repp2.getZ()); j < Math.max(repp2.getZ(), repp1.getZ()); j += 16)
                        {
                            w.setBlockState(new BlockPos(repp1.getX(), repp1.getY(), j), Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir), 2 | 816);
                        }
                    }
                    if(absz == 0)
                    {
                        for (int j = Math.min(repp1.getX(), repp2.getX()); j < Math.max(repp2.getX(), repp1.getX()); j++)
                        {
                            if(i2 % 16 == 0)
                            {
                                i2++;
                                continue;
                            }
                            w.setBlockState(new BlockPos(j, repp1.getY(), repp1.getZ()), Blocks.REDSTONE_WIRE.getDefaultState(), 2 | 816);
                            i2++;
                        }
                        for (int j = Math.min(repp1.getX(), repp2.getX()); j < Math.max(repp2.getX(), repp1.getX()); j += 16)
                        {
                            w.setBlockState(new BlockPos(j, repp1.getY(), repp1.getZ()), Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir), 2 | 816);
                        }
                    }

                    w.setBlockState(repp1, Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir), 2 | 816);
                    w.setBlockState(repp2, Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir), 2 | 816);
                }
                w.setBlockState(p1.withY(real_y + 1), Blocks.REDSTONE_WIRE.getDefaultState());
                w.setBlockState(p2.withY(real_y + 1), Blocks.REDSTONE_WIRE.getDefaultState());
                DFS_build(integer, w, h, real_y, visited);
            }
            else
            {
                int a = 0;
            }
        }
    }

    public static void BuildTwoPin(ServerWorld w, TwoPinNet tpn, int start_y)
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
            Direction dir = Direction.fromVector(nor, Direction.UP).getOpposite(); // Intentionally invalid fall back

            // It appears that sometimes that Lee algorithm that routes if obstacles are found will put a Steiner point in the path of other branches in the same tree causing problems
            
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
                        w.setBlockState(new BlockPos(repp1.getX(), repp1.getY(), j), Blocks.REDSTONE_WIRE.getDefaultState(), 2 | 816);
                        i2++;
                    }

                    for (int j = Math.min(repp1.getZ(), repp2.getZ()); j < Math.max(repp2.getZ(), repp1.getZ()); j += 16)
                    {
                        w.setBlockState(new BlockPos(repp1.getX(), repp1.getY(), j), Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir), 2 | 816);
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
                        w.setBlockState(new BlockPos(j, repp1.getY(), repp1.getZ()), Blocks.REDSTONE_WIRE.getDefaultState(), 2 | 816);
                        i2++;
                    }
                    for (int j = Math.min(repp1.getX(), repp2.getX()); j < Math.max(repp2.getX(), repp1.getX()); j += 16)
                    {
                        w.setBlockState(new BlockPos(j, repp1.getY(), repp1.getZ()), Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir), 2 | 816);
                    }
                }

                w.setBlockState(repp1, Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir), 2 | 816);
                w.setBlockState(repp2, Blocks.REPEATER.getDefaultState().with(HorizontalFacingBlock.FACING, dir), 2 | 816);
            }
            w.setBlockState(p1.withY(real_y + 1), Blocks.REDSTONE_WIRE.getDefaultState());
            w.setBlockState(p2.withY(real_y + 1), Blocks.REDSTONE_WIRE.getDefaultState());
        }
        w.updateNeighbors(tpn.point_list.getFirst().withY(real_y + 1), Blocks.REDSTONE_WIRE);
    }

    private static @NonNull List<BlockPos> GetIntersectorsZ(List<BlockPos> h, BlockPos p1, BlockPos p2) {
        return h.stream().filter(a -> a.getZ() == p1.getZ()).filter(a -> a.getX() >= Math.min(p1.getX(), p2.getX()) && a.getX() <= Math.max(p1.getX(), p2.getX())).sorted(Comparator.comparingInt(BlockPos::getX)).toList();
    }

    private static @NonNull List<BlockPos> GetIntersectorsX(List<BlockPos> h, BlockPos p1, BlockPos p2) {
        return h.stream().filter(a -> a.getX() == p1.getX()).filter(a -> a.getZ() >= Math.min(p1.getZ(), p2.getZ()) && a.getZ() <= Math.max(p1.getZ(), p2.getZ())).sorted(Comparator.comparingInt(BlockPos::getZ)).toList();
    }

}
