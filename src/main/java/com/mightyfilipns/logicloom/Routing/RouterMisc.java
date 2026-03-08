package com.mightyfilipns.logicloom.Routing;

import com.mightyfilipns.logicloom.JsonLoader.CellInfo;
import com.mightyfilipns.logicloom.JsonLoader.JsonDesign;
import com.mightyfilipns.logicloom.JsonLoader.AbstractCell;
import com.mightyfilipns.logicloom.JsonLoader.PortDirection;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RouterMisc
{
    public static BlockPos GetRelPos(Map<CellInfo, BlockPos> cellmap, AbstractCell a, Integer conn, Map<Integer, BlockPos> port_rel_pos)
    {
        if(a instanceof CellInfo b)
        {
            String pin_name = "";
            for (Map.Entry<String, List<Integer>> stringListEntry : b.connections.entrySet())
            {
                for (Integer i : stringListEntry.getValue())
                {
                    if(Objects.equals(conn, i))
                    {
                        pin_name = stringListEntry.getKey();
                        break;
                    }
                }
            }

            return b.type.GetPort(pin_name).relpos().add(cellmap.get(b));
        }
        else if(a instanceof JsonDesign.DesignPortInfo)
        {
            // TODO: fix
            return port_rel_pos.get(conn);
        }
        else
        {
            throw new RuntimeException("GetRelPos: unimplemented abstract cell");
        }
    }

    public static PortDirection GetDir(AbstractCell a, Integer conn)
    {
        if(a instanceof CellInfo b)
        {
            String pin_name = "";

            for (Map.Entry<String, List<Integer>> stringListEntry : b.connections.entrySet())
            {
                for (Integer i : stringListEntry.getValue())
                {
                    if(Objects.equals(conn, i))
                    {
                        pin_name = stringListEntry.getKey();
                        break;
                    }
                }
            }

            return b.port_directions.get(pin_name);
        }
        else if(a instanceof JsonDesign.DesignPortInfo p)
        {
            return p.direction == PortDirection.Input ? PortDirection.Output : PortDirection.Input;
        }
        else
        {
            throw new RuntimeException("unimplemented abstract cell");
        }
    }
}
