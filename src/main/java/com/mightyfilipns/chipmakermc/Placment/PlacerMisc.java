package com.mightyfilipns.chipmakermc.Placment;

import Jama.Matrix;

import java.util.Arrays;

import static java.lang.Math.round;

public class PlacerMisc
{
    static int[] RoundMtxSol(Matrix d, int cell_size)
    {
        return Arrays.stream(d.getRowPackedCopy()).mapToInt(a -> (int)round(a * cell_size)).toArray();
    }

    public static void AddAtMatrix(Matrix mtx, int i, int j, int val)
    {
        mtx.set(i,j,mtx.get(i, j) + val);
    }

    public static void AddAtMatrix(Matrix mtx, int i, int j, double val)
    {
        mtx.set(i,j,mtx.get(i, j) + val);
    }

    public static void AddFixedVirtualCell(Matrix q, Matrix c_x, Matrix c_z, int rel_x, int rel_z)
    {
        for (int i = 0; i < q.getRowDimension(); i++)
        {
            AddFixed(q, c_x, c_z, rel_x, rel_z, i, 1);
        }
    }

    public static void AddFixedVirtualCell(Matrix q, Matrix c_x, Matrix c_z, int rel_x, int rel_z, double strength)
    {
        for (int i = 0; i < q.getRowDimension(); i++)
        {
            AddFixed(q, c_x, c_z, rel_x, rel_z, i, strength);
        }
    }

    static void AddFixed(Matrix q, Matrix c_x, Matrix c_z, int rel_x, int rel_z, int i, double strength)
    {
        AddAtMatrix(c_x, i, 0, rel_x);
        AddAtMatrix(c_z, i, 0, rel_z);
        AddAtMatrix(q, i, i, strength);
    }
}
