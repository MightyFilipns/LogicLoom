package com.mightyfilipns.logicloom.JsonLoader;

import com.google.gson.annotations.SerializedName;

public enum PortDirection {
    @SerializedName("input")
    Input,
    @SerializedName("output")
    Output,
    @SerializedName("inout")
    Inout;

    public PortDirection Invert() {
        return this == Input ? Output : Input;
    }
}
