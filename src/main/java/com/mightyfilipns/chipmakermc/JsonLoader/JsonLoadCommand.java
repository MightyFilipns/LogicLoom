package com.mightyfilipns.chipmakermc.JsonLoader;

import com.google.gson.GsonBuilder;
import com.mightyfilipns.chipmakermc.Chipmakermc;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.text.Text;
import net.minecraft.server.command.*;

import java.io.FileReader;
import java.nio.file.Path;


public class JsonLoadCommand
{
    public static int LoadJSONDesign(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> Text.literal("Called load json"), false);
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(boolean.class, new JsonDesign.BooleanTypeAdapter());
        var d = builder.create();
        JsonDesign deg;

        String file_path = "";
        try {
            file_path = StringArgumentType.getString(context, "file_path");
            var ph = Path.of(file_path);
            var p = ph.toFile();
            if (!p.exists())
            {
                context.getSource().sendError(Text.literal("Given file does not exist: " + file_path));
                return 0;
            }
            if (!p.isFile())
            {
                context.getSource().sendError(Text.literal("Not a file: " + file_path));
                return 0;
            }
            if (!p.isFile())
            {
                context.getSource().sendError(Text.literal("Not a file: " + file_path));
                return 0;
            }
            var dir = context.getSource().getServer().getRunDirectory();
            // Allow singe player worlds to load from anywhere
            if (!ph.startsWith(dir) && context.getSource().getServer().isDedicated())
            {
                context.getSource().sendError(Text.literal("Not in a subdirectory of: " + dir));
                return 0;
            }
        } catch (IllegalArgumentException e) {
            file_path = "/run/media/Filip/E_Volume/programming/verilog/Yosys-test/Counter.json";
        }

        try(FileReader sf = new FileReader(file_path)) {
           deg = d.fromJson(sf, JsonDesign.class);
        } catch (Exception e) {
            context.getSource().sendFeedback(() -> Text.literal("Failed json parsing\n Error:" + e.getMessage()), false);
           return 0;
        }
        Chipmakermc.loaded_design = deg;
        return 1;
    }
}
