package com.mightyfilipns.logicloom.JsonLoader;

import com.google.gson.GsonBuilder;
import com.mightyfilipns.logicloom.LogicLoom;
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
        if (!ValidatePath(context, file_path))
            return 0;

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
        if (deg.modules.isEmpty())
        {
            context.getSource().sendError(Text.of("No modules found"));
            return 0;
        }
        if (deg.modules.size() > 1)
        {
            context.getSource().sendError(Text.of("More than 1 module found. All modules after the first one will be ignored."));
        }
        var mod = (JsonDesign.DesignModule)deg.modules.values().toArray()[0];
        boolean inouts = mod.cells.values().stream().anyMatch(a -> a.port_directions.values().stream().anyMatch(b -> b == PortDirection.Inout));
        inouts |= mod.ports.values().stream().anyMatch(a -> a.direction == PortDirection.Inout);

        if (inouts)
        {
            context.getSource().sendError(Text.of("Inouts are unsupported"));
            return 0;
        }

        context.getSource().sendMessage(Text.literal("Loaded module " + deg.modules.keySet().toArray(new String[] {})[0]));
        LogicLoom.loaded_design = deg;
        return 1;
    }

    public static boolean ValidatePath(CommandContext<ServerCommandSource> context, String file_path) {
        var ph = Path.of(file_path);
        var p = ph.toFile();
        if (!p.exists())
        {
            context.getSource().sendError(Text.of("File does not exist: " + file_path));
            return false;
        }
        if (!p.isFile())
        {
            context.getSource().sendError(Text.of("Not a file: " + file_path));
            return false;
        }
        var dir = context.getSource().getServer().getRunDirectory();

        // Allow singe player worlds to load from anywhere
        if (!ph.startsWith(dir) && context.getSource().getServer().isDedicated())
        {
            context.getSource().sendError(Text.of("Not in a subdirectory of: " + dir));
            return false;
        }
        return true;
    }
}
