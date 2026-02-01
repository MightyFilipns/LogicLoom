package com.mightyfilipns.chipmakermc.Routing;

import com.mightyfilipns.chipmakermc.JsonLoader.CellInfo;
import com.mightyfilipns.chipmakermc.JsonLoader.JsonDesign;
import com.mightyfilipns.chipmakermc.JsonLoader.AbstractCell;
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
            var cell_pos = cellmap.get(b);
            return switch (pin_name) {
                case "A" -> cell_pos.add(-1, 0, 5);
                case "B" -> cell_pos.add(2, 0, 5);
                case "Y" -> cell_pos.add(-1, 0, 0);
                default -> throw new RuntimeException("Invalid pin name: " + pin_name);
            };
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

    public static JsonDesign.PortDirection GetDir(AbstractCell a, Integer conn)
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
            return switch (pin_name) {
                case "A", "B" -> JsonDesign.PortDirection.Input;
                case "Y" -> JsonDesign.PortDirection.Output;
                default -> throw new RuntimeException("Invalid pin name: " + pin_name);
            };
        }
        else if(a instanceof JsonDesign.DesignPortInfo p)
        {
            return p.direction == JsonDesign.PortDirection.Input ? JsonDesign.PortDirection.Output : JsonDesign.PortDirection.Input;
        }
        else
        {
            throw new RuntimeException("unimplemented abstract cell");
        }
    }
}
