package com.mightyfilipns.chipmakermc.Routing;

import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SteinerCalc {
    // TODO: optimize
    public static List<Pair<BlockPos, Boolean>> FindMinimumSteinerRectilinearTree(List<BlockPos> b, List<List<Integer>> adj_list)
    {
        // Matrix c = new Matrix(b.size(), b.size());
        var orgb = b.stream().map(a -> a.withY(0)).toList();

        HashSet<BlockPos> exisitng_points = new HashSet<>();

        var P = new ArrayList<>(orgb);

        var pari = FindClosestPair(orgb);
        var p_a = pari.getLeft();
        var p_b = pari.getRight();
        List<Pair<BlockPos, Boolean>> res = new ArrayList<>();
        // List<List<Integer>> adj_list = new ArrayList<>();
        HashMap<BlockPos, Integer> translation_map = new HashMap<>();

        translation_map.put(p_a, 0);
        res.add(Pair.of(p_a, false));
        translation_map.put(p_b, res.size());
        res.add(Pair.of(p_b, false));
        adj_list.add(new ArrayList<>());
        adj_list.add(new ArrayList<>());
        exisitng_points.add(p_a);
        exisitng_points.add(p_b);

        var curr_mbb = GetMBBPoints(pari);

        var p_mbb = p_a;
        var p_c = p_b;

        P.remove(p_a);
        P.remove(p_b);

        while (!P.isEmpty())
        {
            var pari2 = FindClosestPair2(curr_mbb, P);

            var p_mbb_p = pari2.getLeft();
            var p_c_p = pari2.getRight();

            if(!exisitng_points.contains(p_c_p))
            {
                translation_map.put(p_c_p, res.size());
                res.add(Pair.of(p_c_p, false));
                adj_list.add(new ArrayList<>());
                P.remove(p_c_p);
                exisitng_points.add(p_c_p);
            }
            else
            {
                int a = 0;
            }

            if(orgb.contains(p_mbb_p))
            {
                // ADD l shape
                var stpoint = new BlockPos(p_mbb.getX(), 0 ,p_c.getZ());

                if(!exisitng_points.contains(stpoint))
                {
                    translation_map.put(stpoint, res.size());
                    res.add(Pair.of(stpoint, true));
                    adj_list.add(new ArrayList<>());
                    exisitng_points.add(stpoint);
                }

                if(p_mbb != stpoint)
                {
                    EnforceSingleAxis(p_mbb, stpoint);
                    adj_list.get(translation_map.get(p_mbb)).add(translation_map.get(stpoint));
                    adj_list.get(translation_map.get(stpoint)).add(translation_map.get(p_mbb));
                }

                if(stpoint != p_c)
                {
                    EnforceSingleAxis(p_c, stpoint);
                    adj_list.get(translation_map.get(stpoint)).add(translation_map.get(p_c));
                    adj_list.get(translation_map.get(p_c)).add(translation_map.get(stpoint));
                }
            }
            else
            {
                if(!exisitng_points.contains(p_mbb_p))
                {
                    int pos = res.size();
                    translation_map.put(p_mbb_p, pos);
                    res.add(Pair.of(p_mbb_p, true));
                    adj_list.add(new ArrayList<>());
                    exisitng_points.add(p_mbb_p);
                }

                // ADD l shape
                if(p_mbb_p != p_mbb)
                {
                    EnforceSingleAxis(p_mbb_p, p_mbb);
                    adj_list.get(translation_map.get(p_mbb_p)).add(translation_map.get(p_mbb));
                    adj_list.get(translation_map.get(p_mbb)).add(translation_map.get(p_mbb_p));
                }

                if(p_mbb_p != p_c)
                {
                    EnforceSingleAxis(p_mbb_p, p_c);
                    adj_list.get(translation_map.get(p_mbb_p)).add(translation_map.get(p_c));
                    adj_list.get(translation_map.get(p_c)).add(translation_map.get(p_mbb_p));
                }
            }
            curr_mbb = GetMBBPoints(pari2);
            p_mbb = p_mbb_p;
            p_c = p_c_p;
        }
        // ADD final l shape
        var p_mbb_p = new BlockPos(p_c.getX(),0, p_mbb.getZ());
        if(!exisitng_points.contains(p_mbb_p))
        {
            exisitng_points.add(p_mbb_p);
            translation_map.put(p_mbb_p, res.size());
            res.add(Pair.of(p_mbb_p, true));
            adj_list.add(new ArrayList<>());
        }

        if(p_mbb_p != p_mbb)
        {
            EnforceSingleAxis(p_mbb_p, p_mbb);
            adj_list.get(translation_map.get(p_mbb_p)).add(translation_map.get(p_mbb));
            adj_list.get(translation_map.get(p_mbb)).add(translation_map.get(p_mbb_p));
        }

        if(p_mbb_p != p_c)
        {
            EnforceSingleAxis(p_mbb_p, p_c);
            adj_list.get(translation_map.get(p_mbb_p)).add(translation_map.get(p_c));
            adj_list.get(translation_map.get(p_c)).add(translation_map.get(p_mbb_p));
        }

        return res;
    }

    static void EnforceSingleAxis(BlockPos p1, BlockPos p2)
    {
        if(p1.getX() != p2.getX() && p1.getZ() != p2.getZ())
        {
            throw new RuntimeException("only one axis allowed");
        }
    }

    static List<BlockPos> GetMBBPoints(Pair<BlockPos, BlockPos> a)
    {
        var d = new ArrayList<BlockPos>();
        d.add(new BlockPos(a.getLeft().getX(),0, a.getRight().getZ()));
        d.add(new BlockPos(a.getRight().getX(), 0, a.getLeft().getZ()));
        d.add(a.getRight());
        d.add(a.getLeft());
        return d;
    }

    private static Pair<BlockPos, BlockPos> FindClosestPair2(List<BlockPos> b, List<BlockPos> c)
    {
        int low = Integer.MAX_VALUE;
        Pair<BlockPos, BlockPos> ret = null;
        for (BlockPos blockPos : b)
        {
            for (BlockPos pos : c)
            {
                var dist = pos.getManhattanDistance(blockPos);
                if(dist == 0)
                    continue;

                if(dist < low)
                {
                    ret = Pair.of(blockPos, pos);
                    low = dist;
                }
            }
        }
        return ret;
    }

    private static Pair<BlockPos, BlockPos> FindClosestPair(List<BlockPos> b)
    {
        int low = Integer.MAX_VALUE;
        Pair<BlockPos, BlockPos> ret = null;
        for (BlockPos blockPos : b)
        {
            for (BlockPos pos : b)
            {
                var dist = pos.getManhattanDistance(blockPos);
                if(dist == 0)
                    continue;

                if(dist < low)
                {
                    ret = Pair.of(blockPos, pos);
                    low = dist;
                }
            }
        }
        return ret;
    }
}
