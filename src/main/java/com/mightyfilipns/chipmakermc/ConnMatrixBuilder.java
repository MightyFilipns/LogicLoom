package com.mightyfilipns.chipmakermc;

import Jama.Matrix;
import com.mightyfilipns.chipmakermc.JsonLoader.AbstractCell;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnMatrixBuilder
{
    static Matrix GetConnectivityMatrix()
    {
        var des = Chipmakermc.loaded_design;
        var mod = (JsonDesign.DesignModule)des.modules.values().toArray()[0];
        int matrix_size = mod.cells.size() + mod.ports.size();
        Matrix connection_m = new Matrix(matrix_size, matrix_size);

        int new_id = 0;
        for (CellInfo value : mod.cells.values())
        {
            value.cell_ID = new_id;
            new_id++;
        }

        for (JsonDesign.DesignPortInfo value : mod.ports.values())
        {
            value.cell_ID = new_id;
            new_id++;
        }

        List<CellInfo> cell_list = new ArrayList<>(mod.cells.size());

        mod.cells.forEach((name, CI) -> cell_list.add(CI));
        Map<Integer, List<AbstractCell>> dd = new HashMap<>();

        for (CellInfo cellInfo : cell_list)
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

        mod.ports.forEach((a,b) ->
        {
            for (Integer bit : b.bits)
            {
                dd.get(bit).add(b);
            }
        });

        var d1 = dd.values().stream().filter(a -> a.size() > 2).map((a) -> {
            List<Pair<AbstractCell, AbstractCell>> clique_edges = new ArrayList<>();
            for (AbstractCell ca : a)
            {
                for (AbstractCell cb : a)
                {
                    if(cb != ca)
                        clique_edges.add(Pair.of(ca, cb));
                }
            }
            return clique_edges;
        }).reduce(new ArrayList<>(), (l1, l2) ->
        {
            List<Pair<AbstractCell, AbstractCell>> arr1 = new ArrayList<>();
            arr1.addAll(l1);
            arr1.addAll(l2);
            return arr1;
        });

        var d2 = dd.values().stream().filter(a -> a.size() == 2).map((a) -> {
            return Pair.of(a.get(0), a.get(1));
        }).toList();

        for (Pair<AbstractCell, AbstractCell> cell_pair : d1)
        {
            // Construct the connectivity matrix
            if(cell_pair.getLeft().cell_ID == cell_pair.getRight().cell_ID)
            {
                throw new RuntimeException();
            }
            connection_m.set(cell_pair.getLeft().cell_ID, cell_pair.getRight().cell_ID, 1);
            connection_m.set(cell_pair.getRight().cell_ID, cell_pair.getLeft().cell_ID, 1);
        }

        for (Pair<AbstractCell, AbstractCell> cell_pair : d2)
        {
            // Construct the connectivity matrix
            connection_m.set(cell_pair.getLeft().cell_ID, cell_pair.getRight().cell_ID, 1);
            connection_m.set(cell_pair.getRight().cell_ID, cell_pair.getLeft().cell_ID, 1);
        }

        return connection_m;
    }

    static Pair<Matrix, Map<Integer, List<AbstractCell>>> GetConnectivityMatrixHessian()
    {
        var des = Chipmakermc.loaded_design;
        var mod = (JsonDesign.DesignModule)des.modules.values().toArray()[0];
        int matrix_size = mod.cells.size() + mod.ports.size();
        Matrix connection_m = new Matrix(matrix_size, matrix_size);

        int new_id = 0;
        for (CellInfo value : mod.cells.values())
        {
            value.cell_ID = new_id;
            new_id++;
        }

        for (JsonDesign.DesignPortInfo value : mod.ports.values())
        {
            value.cell_ID = new_id;
            new_id++;
        }

        List<CellInfo> cell_list = new ArrayList<>(mod.cells.size());

        mod.cells.forEach((name, CI) -> cell_list.add(CI));
        Map<Integer, List<AbstractCell>> dd = new HashMap<>();

        for (CellInfo cellInfo : cell_list)
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

        mod.ports.forEach((a,b) ->
        {
            for (Integer bit : b.bits)
            {
                dd.get(bit).add(b);
            }
        });

        var d1 = dd.values().stream().filter(a -> a.size() > 2).map((a) -> {
            List<Pair<AbstractCell, AbstractCell>> clique_edges = new ArrayList<>();
            for (AbstractCell ca : a)
            {
                for (AbstractCell cb : a)
                {
                    if(cb != ca)
                        clique_edges.add(Pair.of(ca, cb));
                }
            }
            return clique_edges;
        }).reduce(new ArrayList<>(), (l1, l2) ->
        {
            List<Pair<AbstractCell, AbstractCell>> arr1 = new ArrayList<>();
            arr1.addAll(l1);
            arr1.addAll(l2);
            return arr1;
        });

        var d2 = dd.values().stream().filter(a -> a.size() == 2).map((a) -> Pair.of(a.get(0), a.get(1))).toList();

        SetMatrixConns(d1, connection_m);
        SetMatrixConns(d2, connection_m);

        for (int i = 0; i < matrix_size; i++)
        {
            SetDiagonal(connection_m, i);
        }

        return Pair.of(connection_m, dd);
    }

    public static Triple<Matrix, Map<Integer, List<AbstractCell>>, List<Pair<AbstractCell, AbstractCell>>> GetConnectivityMatrixHessian2()
    {
        var des = Chipmakermc.loaded_design;
        var mod = (JsonDesign.DesignModule)des.modules.values().toArray()[0];
        int matrix_size = mod.cells.size();
        Matrix connection_m = new Matrix(matrix_size, matrix_size);

        int new_id = 0;
        for (CellInfo value : mod.cells.values())
        {
            value.cell_ID = new_id;
            new_id++;
        }

        for (JsonDesign.DesignPortInfo value : mod.ports.values())
        {
            value.cell_ID = new_id;
            new_id++;
        }

        List<CellInfo> cell_list = new ArrayList<>(mod.cells.size());

        mod.cells.forEach((name, CI) -> cell_list.add(CI));
        Map<Integer, List<AbstractCell>> dd = new HashMap<>();

        for (CellInfo cellInfo : cell_list)
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

        var d1 = dd.values().stream().filter(a -> a.size() > 2).map((a) -> {
            List<Pair<AbstractCell, AbstractCell>> clique_edges = new ArrayList<>();
            for (AbstractCell ca : a)
            {
                for (AbstractCell cb : a)
                {
                    if(cb != ca)
                        clique_edges.add(Pair.of(ca, cb));
                }
            }
            return clique_edges;
        }).reduce(new ArrayList<>(), (l1, l2) ->
        {
            List<Pair<AbstractCell, AbstractCell>> arr1 = new ArrayList<>();
            arr1.addAll(l1);
            arr1.addAll(l2);
            return arr1;
        });

        var d2 = dd.values().stream().filter(a -> a.size() == 2).map((a) -> Pair.of(a.get(0), a.get(1))).toList();

        SetMatrixConns(d1, connection_m);
        SetMatrixConns(d2, connection_m);

        var d3 = new ArrayList<>(d1.stream().filter(a -> a.getLeft() instanceof JsonDesign.DesignPortInfo || a.getRight() instanceof JsonDesign.DesignPortInfo).toList());
        d3.addAll(d2.stream().filter(a -> a.getLeft() instanceof JsonDesign.DesignPortInfo || a.getRight() instanceof JsonDesign.DesignPortInfo).toList());

        for (int i = 0; i < matrix_size; i++)
        {
            SetDiagonal(connection_m, i);
        }

        return Triple.of(connection_m, dd, d3);
    }

    public static Pair<Matrix, Map<Integer, List<AbstractCell>>> GetConnectivityMatrixHessian3()
    {
        var des = Chipmakermc.loaded_design;
        var mod = (JsonDesign.DesignModule)des.modules.values().toArray()[0];
        int matrix_size = mod.cells.size();
        Matrix connection_m = new Matrix(matrix_size, matrix_size);

        int new_id = 0;
        for (CellInfo value : mod.cells.values())
        {
            value.cell_ID = new_id;
            new_id++;
        }

        for (JsonDesign.DesignPortInfo value : mod.ports.values())
        {
            value.cell_ID = new_id;
            new_id++;
        }

        List<CellInfo> cell_list = new ArrayList<>(mod.cells.size());

        mod.cells.forEach((name, CI) -> cell_list.add(CI));
        Map<Integer, List<AbstractCell>> dd = new HashMap<>();

        for (CellInfo cellInfo : cell_list)
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

        dd.values().stream().filter(a -> a.size() > 2).map((a) -> {
            List<Pair<AbstractCell, AbstractCell>> clique_edges = new ArrayList<>();
            for (AbstractCell ca : a)
            {
                for (AbstractCell cb : a)
                {
                    if(cb != ca)
                        clique_edges.add(Pair.of(ca, cb));
                }
            }
            return clique_edges;
        }).forEach(a -> {
            SetMatrixConns(a, connection_m, ((double) -2) / a.size());
        });

        var d2 = dd.values().stream().filter(a -> a.size() == 2).map((a) -> Pair.of(a.get(0), a.get(1))).toList();

        SetMatrixConns(d2, connection_m, -1);

        for (int i = 0; i < matrix_size; i++)
        {
            SetDiagonal(connection_m, i);
        }

        return Pair.of(connection_m, dd);
    }

    static void SetMatrixConns(List<Pair<AbstractCell, AbstractCell>> cells, Matrix connection_m, double strength)
    {
        for (Pair<AbstractCell, AbstractCell> cell_pair : cells.stream().filter(a -> a.getLeft() instanceof CellInfo && a.getRight() instanceof CellInfo).toList())
        {
            // Construct the connectivity matrix
            connection_m.set(cell_pair.getLeft().cell_ID, cell_pair.getRight().cell_ID, strength);
            connection_m.set(cell_pair.getRight().cell_ID, cell_pair.getLeft().cell_ID, strength);
        }
    }

    static void SetMatrixConns(List<Pair<AbstractCell, AbstractCell>> cells, Matrix connection_m)
    {
        for (Pair<AbstractCell, AbstractCell> cell_pair : cells.stream().filter(a -> a.getLeft() instanceof CellInfo && a.getRight() instanceof CellInfo).toList())
        {
            // Construct the connectivity matrix
            connection_m.set(cell_pair.getLeft().cell_ID, cell_pair.getRight().cell_ID, -1);
            connection_m.set(cell_pair.getRight().cell_ID, cell_pair.getLeft().cell_ID, -1);
        }
    }

    static void AddAtMatrix(Matrix mtx, int i, int j, int val)
    {
        mtx.set(i,j,mtx.get(i, j) + val);
    }

    static void SetDiagonal(Matrix connection_m, int i)
    {
        double sum = 0;
        for (int j = 0; j < connection_m.getRowDimension(); j++)
        {
            if(j == i)
                continue;
            sum += connection_m.get(j,i);
        }
        connection_m.set(i,i, (sum * -1));
    }

}
