package com.mightyfilipns.chipmakermc.JsonLoader;

import com.google.gson.annotations.SerializedName;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;

public enum CellType implements StringIdentifiable {
    @SerializedName("$_NOT_")
    NOT(4, 4 ,5, GATE_A(1, 3), GATE_Y(0, 0)),
    @SerializedName("$_AND_")
    AND(6, 4 ,5, GATE_A(0, 3), GATE_B(3, 3),  GATE_Y(0, 0)),
    @SerializedName("$_OR_")
    OR(6, 4 ,8, GATE_A(0, 0), GATE_B(0, 2),  GATE_Y(3, 1)),
    @SerializedName("$_XOR_")
    XOR(6, 4 ,7, GATE_A(0, 5), GATE_B(3, 5),  GATE_Y(0, 0)),
    @SerializedName("$_NOR_")
    NOR(6, 4 ,5, GATE_A(0, 3), GATE_B(3, 3),  GATE_Y(0, 0)),
    @SerializedName("$_NAND_")
    NAND(6, 4 ,5, GATE_A(0, 3), GATE_B(3, 3),  GATE_Y(0, 0)),
    @SerializedName("$_XNOR_")
    XNOR(6, 4 ,8, GATE_A(0, 4), GATE_B(3, 4),  GATE_Y(0, 0)),
    @SerializedName("$_ANDNOT_")
    ANDNOT(6, 4 ,5,  GATE_A(0, 3), GATE_B(3, 3),  GATE_Y(0, 0)),
    @SerializedName("$_ORNOT_")
    ORNOT(7, 4 ,4, GATE_A(1, 2), GATE_B(4, 2),  GATE_Y(0, 0)),
    @SerializedName("$_DFF_P_")
    DFF(6, 4 ,7,  PORT_C(0, 5), PORT_D(3, 5),  PORT_Q(1, 0)),
    @SerializedName("$_DLATCH_P_")
    DLATCH(6, 4 ,8,  PORT_E(0, 4), PORT_D(2, 3),  PORT_Q(1, 0)),
    ;

    public static PortInfo GATE_A(int x, int z){ return new PortInfo("A", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo GATE_B(int x, int z){ return new PortInfo("B", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo GATE_Y(int x, int z){ return new PortInfo("Y", new BlockPos(x, 0, z), PortDirection.Output); }

    public static PortInfo PORT_Q(int x, int z){ return new PortInfo("Q", new BlockPos(x, 0, z), PortDirection.Output); }
    public static PortInfo PORT_C(int x, int z){ return new PortInfo("C", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo PORT_D(int x, int z){ return new PortInfo("D", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo PORT_E(int x, int z){ return new PortInfo("E", new BlockPos(x, 0, z), PortDirection.Input); }

    public final int x_size;
    public final int y_size;
    public final int z_size;
    public final int area;

    public final PortInfo[] ports;

    public record PortInfo(String name, BlockPos relpos, PortDirection dir) {}

    CellType(int xSize, int ySize, int zSize, PortInfo... ports)
    {
        x_size = xSize;
        y_size = ySize;
        z_size = zSize;
        area = xSize * zSize;
        this.ports = ports;
    }

    @Override
    public String asString() {
        return this.name();
    }

    public Identifier getIdentifier()
    {
        return switch (this) {
            case NOT -> Identifier.of("mcchipmaker", "logic_gate_library/not_gate");
            case AND -> Identifier.of("mcchipmaker", "logic_gate_library/and_gate");
            case OR -> Identifier.of("mcchipmaker", "logic_gate_library/or_gate");
            case XOR -> Identifier.of("mcchipmaker", "logic_gate_library/xor_gate");
            case NOR -> Identifier.of("mcchipmaker", "logic_gate_library/nor_gate");
            case NAND -> Identifier.of("mcchipmaker", "logic_gate_library/nand_gate");
            case XNOR -> Identifier.of("mcchipmaker", "logic_gate_library/xnor_gate");
            case ANDNOT -> Identifier.of("mcchipmaker", "logic_gate_library/andnot_gate");
            case ORNOT -> Identifier.of("mcchipmaker", "logic_gate_library/ornot_gate");
            case DFF -> Identifier.of("mcchipmaker", "logic_gate_library/dff");
            case DLATCH -> Identifier.of("mcchipmaker", "logic_gate_library/dlatch");
            default -> throw new RuntimeException("unknown cell type");
        };
    }
}
