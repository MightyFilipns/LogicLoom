package com.mightyfilipns.chipmakermc.Placment;

import com.mightyfilipns.chipmakermc.JsonLoader.CellInfo;
import com.mightyfilipns.chipmakermc.JsonLoader.CellType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/// This is a Tetris like legalizer. It just iteratively checks in all four directions until it finds a valid position
public class TetrisLegalizer
{
    public static boolean Legalize(int[] x, int[] z, List<CellInfo> cell_list)
    {
        var ret = new HashSet<Pair<Integer, Integer>>();

        List<Integer> unplaced = new ArrayList<>();
        for (int i = 0; i < x.length; i++)
        {
            var xstart = x[i];
            var zstart = z[i];
            var t = new HashSet<Pair<Integer, Integer>>();
            if (AttemptPlace(xstart, zstart, ret, t, unplaced, i, cell_list.get(i).type))
            {
                ret.addAll(t);
            }
        }

        for (Integer i : unplaced)
        {
            int iter = 0;
            int x1 = x[i];
            int z1 = z[i];
            var t = new ArrayList<Pair<Integer, Integer>>();
            var ct = cell_list.get(i).type;

            while(true)
            {
                if(iter > 500)
                    throw new RuntimeException("Failed to legalize cell: " + i + " Org pos: X:" + x1 + " Z: " + z1);
                if(CheckPlaceAndUpdate(x, z, i, x1 + iter, z1, ret, t, ct)) break;
                if(CheckPlaceAndUpdate(x, z, i, x1, z1 + iter, ret, t, ct)) break;
                if(CheckPlaceAndUpdate(x, z, i, x1 - iter, z1, ret, t, ct)) break;
                if(CheckPlaceAndUpdate(x, z, i, x1, z1 - iter, ret, t, ct)) break;
                iter++;
            }
            ret.addAll(t);

        }
        return true;
    }

    private static boolean CheckPlaceAndUpdate(int[] x, int[] z, Integer i, int x1, int z1, HashSet<Pair<Integer, Integer>> ret, ArrayList<Pair<Integer, Integer>> t, CellType ct)
    {
        if(AttemptPlaceFix(x1, z1, ret, t, ct))
        {
            x[i] = x1;
            z[i] = z1;
            return true;
        }
        return false;
    }

    private static boolean AttemptPlaceFix(int xstart, int zstart, HashSet<Pair<Integer, Integer>> ret, List<Pair<Integer, Integer>> t, CellType ct)
    {
        for (int j = 0; j < ct.x_size; j++)
        {
            for (int i1 = 0; i1 < ct.z_size; i1++)
            {
                var p = Pair.of(xstart + j, zstart + i1);
                if(!ret.contains(p))
                {
                    t.add(p);
                }
                else
                {
                    t.clear();
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean AttemptPlace(int xstart, int zstart, HashSet<Pair<Integer, Integer>> ret, HashSet<Pair<Integer, Integer>> t, List<Integer> unplaced, int i, CellType ct)
    {
        for (int j = 0; j < ct.x_size; j++)
        {
            for (int i1 = 0; i1 < ct.z_size; i1++)
            {
                var p = Pair.of(xstart + j, zstart + i1);
                if(!ret.contains(p))
                {
                    t.add(p);
                }
                else
                {
                    unplaced.add(i);
                    return false;
                }
            }
        }
        return true;
    }
}
