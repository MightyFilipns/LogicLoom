package com.mightyfilipns.chipmakermc.JsonLoader;

import com.google.gson.annotations.SerializedName;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public enum CellType implements StringIdentifiable {
    @SerializedName("$_NOT_")
    NOT(4, 4 ,5, GATE_A(1, 3), GATE_Y(0, 0)),
    @SerializedName("$_AND_")
    AND(6, 4 ,5, GATE_A(0, 3), GATE_B(3, 3),  GATE_Y(0, 0)),
    @SerializedName("$_OR_")
    OR(5, 4 ,6, GATE_A(2, 1), GATE_B(2, 3),  GATE_Y(0, 3)),
    @SerializedName("$_XOR_")
    XOR(6, 4 ,7, GATE_A(0, 5), GATE_B(3, 5),  GATE_Y(0, 0)),
    @SerializedName("$_NOR_")
    NOR(6, 4 ,5, GATE_A(0, 3), GATE_B(3, 3),  GATE_Y(0, 0)),
    @SerializedName("$_NAND_")
    NAND(6, 4 ,5, GATE_A(0, 3), GATE_B(3, 3),  GATE_Y(0, 0)),
    @SerializedName("$_XNOR_")
    XNOR(6, 4 ,6, GATE_A(0, 4), GATE_B(3, 4),  GATE_Y(0, 0)),
    @SerializedName("$_ANDNOT_")
    ANDNOT(6, 4 ,5,  GATE_A(0, 3), GATE_B(3, 3),  GATE_Y(0, 0)),
    @SerializedName("$_ORNOT_")
    ORNOT(7, 4 ,4, GATE_A(1, 2), GATE_B(4, 2),  GATE_Y(0, 0)),
    @SerializedName("$_MUX_")
    MUX(8, 4 ,8, GATE_A(0, 4), GATE_B(5, 3), MUX_S(4,6),  GATE_Y(0, 0)),
    @SerializedName("$_DFF_P_")
    DFF(6, 4 ,7,  PORT_C(0, 5), PORT_D(3, 5),  PORT_Q(1, 0)),
    @SerializedName("$_DFFE_PP_")
    DFFE(8, 4 ,6,  PORT_C(0, 4), PORT_D(2, 3),  PORT_Q(0, 0), PORT_E(5,4)),
    @SerializedName("$_DLATCH_P_")
    DLATCH(5, 4 ,6,  PORT_E(0, 4), PORT_D(2, 3),  PORT_Q(1, 0)),
    @SerializedName("$_MUX4_")
    MUX4(18, 6, 11, GATE_Y(1, 5), MUX_S(12, 1), MUX_T(12, 5), MUX_IN_A(9, 1), MUX_IN_B(6, 1), MUX_IN_C(3, 1), MUX_IN_D(0, 1)),
    @SerializedName("$_MUX8_")
    MUX8(32, 6, 11, GATE_Y(0, 4), MUX_S(26, 2), MUX_T(28, 6), MUX_U(29, 9), MUX_IN_A(23, 1), MUX_IN_B(20, 1), MUX_IN_C(17, 1), MUX_IN_D(14, 1), MUX_IN_E(11, 1), MUX_IN_F(8, 1), MUX_IN_G(5, 1), MUX_IN_D(2, 1)),
    @SerializedName("$_MUX16_")
    MUX16(34, 6, 26, GATE_Y(0, 12), MUX_S(26, 24), MUX_T(29, 24), MUX_U(29, 1), MUX_V(26, 1), MUX_IN_A(23, 1), MUX_IN_B(20, 1), MUX_IN_C(17, 1), MUX_IN_D(14, 1), MUX_IN_E(11, 1), MUX_IN_F(8, 1), MUX_IN_G(5, 1), MUX_IN_H(2, 1),
            MUX_IN_I(23, 24), MUX_IN_J(20, 24), MUX_IN_K(17, 24), MUX_IN_L(14, 24), MUX_IN_M(11, 24), MUX_IN_N(8, 24), MUX_IN_O(5, 24), MUX_IN_P(2, 24)),
    ;

    public static PortInfo GATE_A(int x, int z){ return new PortInfo("A", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo GATE_B(int x, int z){ return new PortInfo("B", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo GATE_Y(int x, int z){ return new PortInfo("Y", new BlockPos(x, 0, z), PortDirection.Output); }

    public static PortInfo MUX_S(int x, int z){ return new PortInfo("S", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_T(int x, int z){ return new PortInfo("T", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_U(int x, int z){ return new PortInfo("U", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_V(int x, int z){ return new PortInfo("V", new BlockPos(x, 0, z), PortDirection.Input); }

    public static PortInfo MUX_IN_A(int x, int z){ return new PortInfo("A", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_IN_B(int x, int z){ return new PortInfo("B", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_IN_C(int x, int z){ return new PortInfo("C", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_IN_D(int x, int z){ return new PortInfo("D", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_IN_E(int x, int z){ return new PortInfo("E", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_IN_F(int x, int z){ return new PortInfo("F", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_IN_G(int x, int z){ return new PortInfo("G", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_IN_H(int x, int z){ return new PortInfo("H", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_IN_I(int x, int z){ return new PortInfo("I", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_IN_J(int x, int z){ return new PortInfo("J", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_IN_K(int x, int z){ return new PortInfo("K", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_IN_L(int x, int z){ return new PortInfo("L", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_IN_M(int x, int z){ return new PortInfo("M", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_IN_N(int x, int z){ return new PortInfo("N", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_IN_O(int x, int z){ return new PortInfo("O", new BlockPos(x, 0, z), PortDirection.Input); }
    public static PortInfo MUX_IN_P(int x, int z){ return new PortInfo("P", new BlockPos(x, 0, z), PortDirection.Input); }


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

    public PortInfo GetPort(String port)
    {
        for (PortInfo portInfo : ports)
        {
            if (Objects.equals(portInfo.name, port))
                return portInfo;
        }
        throw new RuntimeException("GetPort: cant find port " + port + " in cell " + this);
    }

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
            case MUX -> Identifier.of("mcchipmaker", "logic_gate_library/mux_gate");
            case DFF -> Identifier.of("mcchipmaker", "logic_gate_library/dff");
            case DFFE -> Identifier.of("mcchipmaker", "logic_gate_library/dffe");
            case DLATCH -> Identifier.of("mcchipmaker", "logic_gate_library/dlatch");
            case MUX4 -> Identifier.of("mcchipmaker", "logic_gate_library/mux4");
            case MUX8 -> Identifier.of("mcchipmaker", "logic_gate_library/mux8");
            case MUX16 -> Identifier.of("mcchipmaker", "logic_gate_library/mux16");
            default -> throw new RuntimeException("getIdentifier: unknown cell type");
        };
    }
}
