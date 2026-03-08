package com.mightyfilipns.logicloom.JsonLoader;

import java.util.List;
import java.util.Map;

public class CellInfo extends AbstractCell
{
    public boolean hide_name;
    public CellType type;
    public Map<String, PortDirection> port_directions;
    public Map<String, List<Integer>> connections;
}
