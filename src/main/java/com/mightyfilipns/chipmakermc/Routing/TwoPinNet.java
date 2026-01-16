package com.mightyfilipns.chipmakermc.Routing;

import com.mightyfilipns.chipmakermc.JsonDesign;
import com.mightyfilipns.chipmakermc.JsonLoader.AbstractCell;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class TwoPinNet
{
    public Integer id;
    public AbstractCell c1;
    public AbstractCell c2;
    public BlockPos p1;
    public BlockPos p2;
    public JsonDesign.PortDirection p1dir;

    public List<List<Integer>> adj_list;
    public List<BlockPos> point_list;

    public int y_pos;

    public TwoPinNet(Integer id, AbstractCell c1, AbstractCell c2) {
        this.id = id;
        this.c1 = c1;
        this.c2 = c2;
    }


}
