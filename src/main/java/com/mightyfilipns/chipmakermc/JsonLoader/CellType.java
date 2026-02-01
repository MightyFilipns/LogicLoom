package com.mightyfilipns.chipmakermc.JsonLoader;

import com.google.gson.annotations.SerializedName;
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
    ;

    @Override
    public String asString() {
        return this.name();
    }
}
