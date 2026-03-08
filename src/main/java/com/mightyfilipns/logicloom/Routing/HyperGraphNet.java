package com.mightyfilipns.logicloom.Routing;

import com.mightyfilipns.logicloom.JsonLoader.AbstractCell;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class HyperGraphNet
{
    public Integer net_id;
    public Flute.Tree tree;
    public List<AbstractCell> cells;
    public List<BlockPos> pin_port_pos;
    public List<List<Integer>> adj_list;
    public List<BlockPos> all_points;
    public int y_pos;
    public int out_port_pos;
    public int allpoints_pos;

    public HyperGraphNet(Integer net_id, Flute.Tree tree, List<AbstractCell> cells, List<BlockPos> pin_port_pos, int out_port_pos) {
        this.net_id = net_id;
        this.tree = tree;
        this.cells = cells;
        this.pin_port_pos = pin_port_pos;
        this.out_port_pos = out_port_pos;
    }

    public void SetAdjList(List<List<Integer>> adj_list, List<BlockPos> all_points) {
        this.adj_list = adj_list;
        this.all_points = all_points;
    }
}
