package com.mightyfilipns.chipmakermc.JsonLoader;

import com.google.gson.annotations.SerializedName;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

public enum CellType implements StringIdentifiable {
    @SerializedName("$_NOT_")
    NOT,
    @SerializedName("$_AND_")
    AND,
    @SerializedName("$_OR_")
    OR,
    @SerializedName("$_XOR_")
    XOR,
    @SerializedName("$_NOR_")
    NOR,
    @SerializedName("$_NAND_")
    NAND,
    @SerializedName("$_XNOR_")
    XNOR,
    @SerializedName("$_ANDNOT_")
    ANDNOT,
    @SerializedName("$_ORNOT_")
    ORNOT,
    @SerializedName("$_DFF_P_")
    DFF,
    @SerializedName("$_DLATCH_P_")
    DLATCH,
    ;

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
