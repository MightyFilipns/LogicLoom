package com.mightyfilipns.chipmakermc;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mightyfilipns.chipmakermc.Routing.Router;
import com.mightyfilipns.chipmakermc.Routing.TestCmds;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.resource.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Chipmakermc implements ModInitializer
{
    public static JsonDesign loaded_design = null;

    public static HashMap<CellType, BlockPos> celltable = HashMap.newHashMap(10);

    static String celldata = "{\"OR\":{\"x\":43,\"y\":-11,\"z\":-66},\"XNOR\":{\"x\":61,\"y\":-11,\"z\":-66},\"NAND\":{\"x\":19,\"y\":-11,\"z\":-66},\"ANDNOT\":{\"x\":67,\"y\":-11,\"z\":-66},\"XOR\":{\"x\":37,\"y\":-11,\"z\":-66},\"AND\":{\"x\":31,\"y\":-11,\"z\":-66},\"ORNOT\":{\"x\":12,\"y\":-11,\"z\":-75},\"NOR\":{\"x\":12,\"y\":-11,\"z\":-66},\"NOT\":{\"x\":25,\"y\":-11,\"z\":-66}}";

    @Override
    public void onInitialize()
    {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("chipmaker")
                    .then(CommandManager.literal("load_json").executes(JsonLoadCommand::LoadJSONDesign))
                    .then(CommandManager.literal("place")
                            .then(CommandManager.argument("start_pos", BlockPosArgumentType.blockPos()).executes(Placer::PlaceDesign)))
                    .then(CommandManager.literal("place_cache")
                            .then(CommandManager.argument("start_pos", BlockPosArgumentType.blockPos()).executes(Placer::PlaceCache)))
                    .then(CommandManager.literal("param")
                            .then(CommandManager.literal("max_iter")
                                            .then(CommandManager.argument("max_iter", IntegerArgumentType.integer(0, 10_000)).executes(Chipmakermc::SetMaxIter))
                            .executes(Chipmakermc::GetMaxIter)
                            )
                            .then(CommandManager.literal("force_mul")
                                            .then(CommandManager.argument("force_mul", DoubleArgumentType.doubleArg()).executes(Chipmakermc::SetForceMul))
                            .executes(Chipmakermc::GetForceMul)
                            )
                            .then(CommandManager.literal("chip_size")
                                    .then(CommandManager.argument("chip_size", IntegerArgumentType.integer(0, 10_000)).executes(Chipmakermc::SetChipSize))
                            .executes(Chipmakermc::GetChipSize)
                            )
                            .then(CommandManager.literal("final_overlap_fix")
                                    .then(CommandManager.argument("final_overlap_fix", BoolArgumentType.bool()).executes(Chipmakermc::SetFinalOverlap))
                            .executes(Chipmakermc::GetFinalOverlap)
                            )
                            .then(CommandManager.literal("do_actual_place")
                                    .then(CommandManager.argument("do_actual_place", BoolArgumentType.bool()).executes(Chipmakermc::SetActualPlace))
                            .executes(Chipmakermc::GetActualPlace)
                            )
                    )
                    .then(CommandManager.literal("wipe").executes(Chipmakermc::Wipe))
                    .then(CommandManager.literal("cell_registry")
                            .then(RegisterCellCmd())
                            .then(RegisterJSONCell())
                            .executes(Chipmakermc::HandleCellMapPrint)
                    )
                    .then(CommandManager.literal("route")
                            .then(CommandManager.literal("test_tree").then(CommandManager.argument("start_pos", BlockPosArgumentType.blockPos()).executes(TestCmds::TestTree)))
                            .then(CommandManager.literal("test_hyper").then(CommandManager.argument("start_pos", BlockPosArgumentType.blockPos()).executes(TestCmds::TestHyperGraph)))
                            .then(CommandManager.literal("test_lee_router").executes(TestCmds::TestLeeRouter))
                            .then(CommandManager.literal("rebuild_cached").executes(TestCmds::RebuildCached))
                            .then(CommandManager.literal("test_vert")
                                    .then(CommandManager.argument("up", BlockPosArgumentType.blockPos())
                                    .then(CommandManager.argument("down", BlockPosArgumentType.blockPos())
                                    .executes(TestCmds::TestVerticalBuilder))))
                            .then(CommandManager.literal("test_wire")
                                    .then(CommandManager.argument("index", IntegerArgumentType.integer(0))
                                    .executes(TestCmds::BuildWire)))
                    )
                    .then(CommandManager.literal("debug")
                            .then(CommandManager.literal("vcddebug").executes(Chipmakermc::VCDDebug))
                            .then(CommandManager.literal("vcdcomp").executes(Chipmakermc::VCDComp))
                            .then(CommandManager.literal("check_piston").executes(TestCmds::CheckPistons)))
            );
        });

        ResourceLoader.get(ResourceType.SERVER_DATA).registerReloader(
            Identifier.of("mcchipmaker", "chpi_maker_res"),
            (SynchronousResourceReloader) manager -> {
                var dat2 = manager.getResource(Identifier.of("mcchipmaker:flute_data/post9.dat"));
                var dat1 = manager.getResource(Identifier.of("mcchipmaker:flute_data/powv9.dat"));


                try {
                    Router.SetupFlute(dat1.get().getInputStream(), dat2.get().getInputStream());
                } catch (Exception e) {
                    System.out.println("Failed to setup flute Error: " +  e.getMessage());
                }
            }
        );
    }


    public static int VCDDebug(CommandContext<ServerCommandSource> context)
    {
        VCDHandler.LoadVCD("/home/Filip/.teroshdl/build/dump.vcd");
        return 1;
    }
    public static int VCDComp(CommandContext<ServerCommandSource> context)
    {
        VCDHandler.GetCurrentValuesAndCompare(context.getSource().getWorld());
        return 1;
    }
    public static int Wipe(CommandContext<ServerCommandSource> context)
    {
        int maxx = Placer.do_actual_place ? 100 * Placer.X_CELL_SIZE : 100;
        int maxz = Placer.do_actual_place ? 100 * Placer.Z_CELL_SIZE : 100;
        int maxy = 0;

        if(Placer.do_vertical)
        {
            maxy = Placer.max_iter;
        }
        else if(Placer.do_actual_place)
        {
            if(Router.max_y != 0)
            {
                maxy = Router.max_y - Placer.last_pos.getY();
            }
            else
            {
                maxy = Placer.Y_CELL_SIZE;
            }
        }
        else
        {
            maxy = 1;
        }

        for (BlockPos blockPos : BlockPos.iterate(Placer.last_pos.add(maxx, maxy, maxz), Placer.last_pos.add(0, 0, -1)))
        {
            context.getSource().getWorld().setBlockState(blockPos, Blocks.AIR.getDefaultState(), 2 | 816);
        }
        return 1;
    }

    public ArgumentBuilder<ServerCommandSource, ?> RegisterJSONCell()
    {
        return CommandManager.literal("load").executes(Chipmakermc::HandleCellMapRead);
    }

    public ArgumentBuilder<ServerCommandSource, ?> RegisterCellCmd()
    {
        return CommandManager.literal("register_cell").then(CommandManager.argument("block_pos", BlockPosArgumentType.blockPos()).then(CommandManager.argument("cell_enum", StringArgumentType.word()).suggests(CellTypeSuggestionProvider.Provider()).executes(Chipmakermc::HandleCellRegister)));
    }

    static int HandleCellMapRead(CommandContext<ServerCommandSource> context)
    {
        GsonBuilder gb = new GsonBuilder();
        gb.setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE);

        Gson g = gb.create();

        var d = g.fromJson(celldata, new TypeToken<HashMap<CellType, BlockPos>>(){}.getType());

        celltable = (HashMap<CellType, BlockPos>) d;

        context.getSource().sendFeedback(() -> Text.literal("Loaded table from json"), false);

        return 1;
    }

    static int HandleCellMapPrint(CommandContext<ServerCommandSource> context)
    {
        for (Map.Entry<CellType, BlockPos> cte : celltable.entrySet())
        {
            context.getSource().sendFeedback(() -> Text.literal(String.format("%s - %s", cte.getKey(), cte.getValue())), false);
        }
        GsonBuilder gb = new GsonBuilder();
        gb.setFormattingStyle(FormattingStyle.COMPACT);
        Gson g = gb.create();
        var s = g.toJson(celltable);
        System.out.println(s);
        return 1;
    }

    static int HandleCellRegister(CommandContext<ServerCommandSource> context)
    {
        var cellt = CellType.valueOf(StringArgumentType.getString(context, "cell_enum"));
        var pos = BlockPosArgumentType.getBlockPos(context, "block_pos");

        celltable.put(cellt, pos);

        context.getSource().sendFeedback(() -> Text.literal("Added"), false);

        return 1;
    }

    static int GetMaxIter(CommandContext<ServerCommandSource> context)
    {
        context.getSource().sendFeedback(() -> Text.literal("Max iter - " + Placer.max_iter), false);
        return 1;
    }

    static int SetMaxIter(CommandContext<ServerCommandSource> context)
    {
        Placer.max_iter = IntegerArgumentType.getInteger(context, "max_iter");
        context.getSource().sendFeedback(() -> Text.literal("Max iter is now: " + Placer.max_iter), false);
        return 1;
    }

    static int GetForceMul(CommandContext<ServerCommandSource> context)
    {
        context.getSource().sendFeedback(() -> Text.literal("Force mul - " + Placer.force_mul), false);
        return 1;
    }

    static int SetForceMul(CommandContext<ServerCommandSource> context)
    {
        Placer.force_mul = DoubleArgumentType.getDouble(context, "force_mul");
        context.getSource().sendFeedback(() -> Text.literal("Force mul is now: " + Placer.force_mul), false);
        return 1;
    }

    static int GetChipSize(CommandContext<ServerCommandSource> context)
    {
        context.getSource().sendFeedback(() -> Text.literal("Chip size - " + Placer.chip_size), false);
        return 1;
    }
    static int SetChipSize(CommandContext<ServerCommandSource> context)
    {
        Placer.chip_size = IntegerArgumentType.getInteger(context, "chip_size");
        context.getSource().sendFeedback(() -> Text.literal("Chip size is now: " + Placer.chip_size), false);
        return 1;
    }

    static int GetFinalOverlap(CommandContext<ServerCommandSource> context)
    {
        context.getSource().sendFeedback(() -> Text.literal("Do overlap fix - " + Placer.do_overlap_fix_final), false);
        return 1;
    }

    static int SetFinalOverlap(CommandContext<ServerCommandSource> context)
    {
        Placer.do_overlap_fix_final = BoolArgumentType.getBool(context, "final_overlap_fix");
        context.getSource().sendFeedback(() -> Text.literal("Do overlap fix is now: " + Placer.do_overlap_fix_final), false);
        return 1;
    }

    static int GetActualPlace(CommandContext<ServerCommandSource> context)
    {
        context.getSource().sendFeedback(() -> Text.literal("Do actual place - " + Placer.do_actual_place), false);
        return 1;
    }

    static int SetActualPlace(CommandContext<ServerCommandSource> context)
    {
        Placer.do_actual_place = BoolArgumentType.getBool(context, "do_actual_place");
        context.getSource().sendFeedback(() -> Text.literal("Do actual place is now: " + Placer.do_actual_place), false);
        return 1;
    }
}
