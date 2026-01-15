package com.mightyfilipns.chipmakermc;

import com.mightyfilipns.chipmakermc.JsonLoader.AbstractCell;

import java.util.List;
import java.util.Map;

public class CellInfo extends AbstractCell
{
    public boolean hide_name;
    public CellType type;
    public Map<String, JsonDesign.PortDirection> port_directions;
    public Map<String, List<Integer>> connections;
}
