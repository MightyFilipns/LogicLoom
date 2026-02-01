package com.mightyfilipns.chipmakermc.JsonLoader;

import com.google.gson.GsonBuilder;
import com.mightyfilipns.chipmakermc.Chipmakermc;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.text.Text;
import net.minecraft.server.command.*;

import java.io.FileReader;


public class JsonLoadCommand
{
    public static int LoadJSONDesign(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> Text.literal("Called load json"), false);
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(boolean.class, new JsonDesign.BooleanTypeAdapter());
        var d = builder.create();
        FileReader f = null;
        JsonDesign deg;
        try(FileReader sf = new FileReader("/run/media/Filip/E_Volume/programming/verilog/Yosys-test/Counter.json")) {
           deg = d.fromJson(sf, JsonDesign.class);
        } catch (Exception e) {
            context.getSource().sendFeedback(() -> Text.literal("Failed json parsing\n Error:" + e.getMessage()), false);
           return 0;
        }
        Chipmakermc.loaded_design = deg;
        return 1;
    }
}
