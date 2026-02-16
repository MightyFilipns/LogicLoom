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
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(boolean.class, new JsonDesign.BooleanTypeAdapter());
        var d = builder.create();
        JsonDesign deg;

        String file_path = StringArgumentType.getString(context, "file_path");
        var ph = Path.of(file_path);
        var p = ph.toFile();
        if (!p.exists())
        {
            context.getSource().sendError(Text.of("File does not exist: " + file_path));
            return 0;
        }
        if (!p.isFile())
        {
            context.getSource().sendError(Text.of("Not a file: " + file_path));
            return 0;
        }
        var dir = context.getSource().getServer().getRunDirectory();

        // Allow singe player worlds to load from anywhere
        if (!ph.startsWith(dir) && context.getSource().getServer().isDedicated())
        {
            context.getSource().sendError(Text.of("Not in a subdirectory of: " + dir));
            return 0;
        }

        try(FileReader sf = new FileReader(file_path)) {
           deg = d.fromJson(sf, JsonDesign.class);
        } catch (Exception e) {
            context.getSource().sendMessage(Text.of("Failed json parsing\n Error:" + e.getMessage()));
           return 0;
        }
        var invalid_cells =  deg.modules.values().stream().toList().getFirst().cells.values().stream().anyMatch(a -> a.type == null);
        if (invalid_cells)
        {
            context.getSource().sendError(Text.of("Invalid cells found"));
            return 0;
        }
        context.getSource().sendMessage(Text.literal("Loaded module " + deg.modules.keySet().toArray(new String[] {})[0]));
        Chipmakermc.loaded_design = deg;
        return 1;
    }
}
