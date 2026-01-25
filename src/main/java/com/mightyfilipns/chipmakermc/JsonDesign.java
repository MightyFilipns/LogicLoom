package com.mightyfilipns.chipmakermc;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.mightyfilipns.chipmakermc.JsonLoader.AbstractCell;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class JsonDesign
{
    public String creator;
    public Map<String, DesignModule> modules;

    public class DesignModule
    {
        public Map<String, String> attributes;
        public Map<String, DesignPortInfo> ports;
        public Map<String, CellInfo> cells;
        public Map<String, JsonWire> netnames;
    }
    public class DesignPortInfo extends AbstractCell
    {
        public PortDirection direction;
        public List<Integer> bits;
    }

    public enum PortDirection
    {
        @SerializedName("input")
        Input,
        @SerializedName("output")
        Output,
        @SerializedName("inout")
        Inout;

        public PortDirection Invert()
        {
            return this == Input ? Output : Input;
        }
    }

    public class JsonWire
    {
        public boolean hide_name;
        public List<Integer> bits;
    }

    static class BooleanTypeAdapter implements JsonDeserializer<Boolean> {
        public Boolean deserialize(JsonElement json, Type typeOfT,
                                   JsonDeserializationContext context) throws JsonParseException {
            if (((JsonPrimitive) json).isBoolean()) {
                return json.getAsBoolean();
            }
            if (((JsonPrimitive) json).isString()) {
                String jsonValue = json.getAsString();
                if (jsonValue.equalsIgnoreCase("true")) {
                    return true;
                } else if (jsonValue.equalsIgnoreCase("false")) {
                    return false;
                } else {
                    return null;
                }
            }

            int code = json.getAsInt();
            return code == 0 ? false :
                    code == 1 ? true : null;
        }
    }
}


