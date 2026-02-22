package com.mightyfilipns.chipmakermc;

import com.mightyfilipns.chipmakermc.JsonLoader.CellType;
import com.mightyfilipns.chipmakermc.JsonLoader.JsonDesign;
import com.mightyfilipns.chipmakermc.JsonLoader.JsonLoadCommand;
import com.mightyfilipns.chipmakermc.JsonLoader.PortDirection;
import com.mightyfilipns.chipmakermc.Misc.CellTypeSuggestionProvider;
import com.mightyfilipns.chipmakermc.Misc.ForceWireModeSuggestionProvider;
import com.mightyfilipns.chipmakermc.Misc.VCDHandler;
import com.mightyfilipns.chipmakermc.Placment.FixedPointsManager;
import com.mightyfilipns.chipmakermc.Placment.Placer;
import com.mightyfilipns.chipmakermc.Routing.Misc;
import com.mightyfilipns.chipmakermc.Routing.Router;
import com.mightyfilipns.chipmakermc.Routing.TestCmds;
import com.mightyfilipns.chipmakermc.Misc.WireDebugger;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.block.Blocks;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.resource.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import static com.mightyfilipns.chipmakermc.JsonLoader.JsonLoadCommand.ValidatePath;
import static com.mightyfilipns.chipmakermc.Placment.Placer.placement_data;

public class Chipmakermc implements ModInitializer
{
    public static JsonDesign loaded_design = null;

    @Override
    public void onInitialize()
    {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("chipmaker").requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                    .then(CommandManager.literal("load_json").then(CommandManager.argument("file_path" ,StringArgumentType.greedyString()).executes(JsonLoadCommand::LoadJSONDesign)))
                    .then(CommandManager.literal("start_pos").executes(Chipmakermc::PrintStartPos).then(CommandManager.argument("block_pos", BlockPosArgumentType.blockPos()).executes(Chipmakermc::SetBlockPos)))
                    .then(CommandManager.literal("place").executes(Placer::PlaceDesign))
                    .then(CommandManager.literal("param")
                            .then(CommandManager.literal("max_iter")
                                            .then(CommandManager.argument("max_iter", IntegerArgumentType.integer(0, 10_000)).executes(Chipmakermc::SetMaxIter))
                            .executes(Chipmakermc::GetMaxIter)
                            )
                            .then(CommandManager.literal("router_max_search")
                                    .then(CommandManager.argument("router_max_search", IntegerArgumentType.integer(0)).executes(Chipmakermc::SetMaxSearch))
                                    .executes(Chipmakermc::GetMaxSearch)
                            )
                            .then(CommandManager.literal("force_const")
                                            .then(CommandManager.argument("force_const", DoubleArgumentType.doubleArg()).executes(Chipmakermc::SetForceConst))
                            .executes(Chipmakermc::GetForceConst)
                            )
                            .then(CommandManager.literal("force_mul")
                                    .then(CommandManager.argument("force_mul", DoubleArgumentType.doubleArg()).executes(Chipmakermc::SetForceMul))
                                    .executes(Chipmakermc::GetForceMul)
                            )
                            .then(CommandManager.literal("chip_size")
                                    .then(CommandManager.argument("chip_size", IntegerArgumentType.integer(0, 10_000)).executes(Chipmakermc::SetChipSize))
                            .executes(Chipmakermc::GetChipSize)
                            )
                            .then(CommandManager.literal("do_actual_place")
                                    .then(CommandManager.argument("do_actual_place", BoolArgumentType.bool()).executes(Chipmakermc::SetActualPlace))
                            .executes(Chipmakermc::GetActualPlace)
                            )
                            .then(CommandManager.literal("do_legalization")
                                    .then(CommandManager.argument("do_legalization", BoolArgumentType.bool()).executes(Chipmakermc::SetLegalization))
                            .executes(Chipmakermc::GetLegalization)
                            )
                    )
                    .then(CommandManager.literal("fixed_points")
                            .then(CommandManager.literal("add_abs")
                                    .then(CommandManager.argument("position", BlockPosArgumentType.blockPos())
                                            .then(CommandManager.argument("strength", DoubleArgumentType.doubleArg())
                                                    .executes(FixedPointsManager::AddPointAbs))))
                            .then(CommandManager.literal("add_rel")
                                    .then(CommandManager.argument("x", IntegerArgumentType.integer())
                                            .then(CommandManager.argument("z", IntegerArgumentType.integer())
                                                    .then(CommandManager.argument("strength", DoubleArgumentType.doubleArg())
                                                            .executes(FixedPointsManager::AddPointRel)))))
                            .then(CommandManager.literal("remove")
                                    .then(CommandManager.argument("index", IntegerArgumentType.integer(0))
                                            .executes(FixedPointsManager::RemovePoint)))
                            .then(CommandManager.literal("list").executes(FixedPointsManager::ListPoints))
                            .then(CommandManager.literal("clear").executes(FixedPointsManager::ClearAllPoints))
                    )
                    .then(CommandManager.literal("misc")
                            .then(CommandManager.literal("force_power_wire").then(CommandManager.argument("type", StringArgumentType.word()).suggests(ForceWireModeSuggestionProvider.Provider()).executes(WireDebugger::ForcePowerWires)))
                            .then(CommandManager.literal("check_wires").executes(Chipmakermc::CheckWires))
                            .then(CommandManager.literal("show_boundary").executes(Chipmakermc::ShowBoundBox))
                            .then(CommandManager.literal("wipe").executes(Chipmakermc::Wipe))
                    )
                    .then(CommandManager.literal("route").executes(Router::DoRouting))
                    .then(CommandManager.literal("debug")
                            .then(CommandManager.literal("place_cache").executes(Placer::PlaceCache))
                            .then(CommandManager.literal("vcddebug").then(CommandManager.argument("file_path", StringArgumentType.greedyString()).executes(Chipmakermc::VCDDebug)))
                            .then(CommandManager.literal("vcdcomp").executes(Chipmakermc::VCDComp))
                            .then(CommandManager.literal("test_cell_port").then(CommandManager.argument("cell_type", StringArgumentType.word()).suggests(CellTypeSuggestionProvider.Provider()).then(CommandManager.argument("pos", BlockPosArgumentType.blockPos()).executes(Chipmakermc::TestCellPorts))))
                            .then(CommandManager.literal("test_tree").then(CommandManager.argument("start_pos", BlockPosArgumentType.blockPos()).executes(TestCmds::TestTree)))
                            .then(CommandManager.literal("test_hyper").then(CommandManager.argument("start_pos", BlockPosArgumentType.blockPos()).executes(TestCmds::TestHyperGraph)))
                            .then(CommandManager.literal("test_lee_router").executes(TestCmds::TestLeeRouter))
                            .then(CommandManager.literal("rebuild_cached").executes(TestCmds::RebuildCached))
                            .then(CommandManager.literal("test_vert").then(CommandManager.argument("up", BlockPosArgumentType.blockPos()).then(CommandManager.argument("down", BlockPosArgumentType.blockPos()).executes(TestCmds::TestVerticalBuilder))))
                            .then(CommandManager.literal("test_wire").then(CommandManager.argument("index", IntegerArgumentType.integer(0)).executes(TestCmds::BuildWire)))
                            .then(CommandManager.literal("test_template").then(CommandManager.argument("pos", BlockPosArgumentType.blockPos()).executes(TestCmds::TestTemplate)))
                    )
            );
        });

        ResourceLoader.get(ResourceType.SERVER_DATA).registerReloader(
            Identifier.of("mcchipmaker", "chpi_maker_res"),
            (SynchronousResourceReloader) manager -> {
                var dat2 = manager.getResource(Identifier.of("mcchipmaker:flute_data/post9.dat"));
                var dat1 = manager.getResource(Identifier.of("mcchipmaker:flute_data/powv9.dat"));

                try {
                    Misc.SetupFlute(dat1.get().getInputStream(), dat2.get().getInputStream());
                } catch (Exception e) {
                    System.out.println("Failed to setup flute Error: " +  e.getMessage());
                }
            }
        );
    }

    public static int PrintStartPos(CommandContext<ServerCommandSource> context)
    {
        if (Placer.start_pos == null)
        {
            context.getSource().sendMessage(Text.of("No position set"));
        }
        else
        {
            context.getSource().sendMessage(Text.of("Start set to " + Placer.start_pos));
        }
        return 1;
    }

    public static int TestCellPorts(CommandContext<ServerCommandSource> context)
    {
        var s = StringArgumentType.getString(context, "cell_type");
        CellType ct = CellType.valueOf(s);
        BlockPos paste_pos = BlockPosArgumentType.getBlockPos(context, "pos");

        ServerWorld w = context.getSource().getWorld();
        var t = w.getStructureTemplateManager();
        var opt = t.getTemplate(ct.getIdentifier());
        var tmplt = opt.get();

        tmplt.place(w, paste_pos, null, placement_data, null, 3);

        for (CellType.PortInfo port : ct.ports)
        {
            var ps = paste_pos.add(port.relpos());

            w.setBlockState(ps, port.dir() == PortDirection.Input ? Blocks.BLUE_WOOL.getDefaultState() : Blocks.RED_WOOL.getDefaultState() );

            w.setBlockState(ps.add(0, 1, 0), Blocks.OAK_SIGN.getDefaultState().with(SignBlock.ROTATION, 8));
            ((SignBlockEntity) w.getBlockEntity(ps.add(0, 1, 0))).setText(new SignText().withMessage(1, Text.of(port.name())), true);
        }

        return 1;
    }

    private static int SetBlockPos(CommandContext<ServerCommandSource> context)
    {
        Placer.start_pos = BlockPosArgumentType.getBlockPos(context, "block_pos");
        context.getSource().sendMessage(Text.literal("Start pos set to " + Placer.start_pos));
        return 1;
    }

    public static int ShowBoundBox(CommandContext<ServerCommandSource> context)
    {
        if (Misc.CheckStartPos(context))
        {
            return 0;
        }

        var p = Placer.start_pos.add(0, 10 ,0);
        for (BlockPos blockPos : BlockPos.iterate(p, p.add(Placer.chip_size, 0, 0)))
        {
            context.getSource().getWorld().setBlockState(blockPos, Blocks.BLUE_WOOL.getDefaultState());
        }
        for (BlockPos blockPos : BlockPos.iterate(p, p.add(0, 0, Placer.chip_size)))
        {
            context.getSource().getWorld().setBlockState(blockPos, Blocks.BLUE_WOOL.getDefaultState());
        }

        return 1;
    }

    public static int VCDDebug(CommandContext<ServerCommandSource> context)
    {
        String file_path = StringArgumentType.getString(context, "file_path");

        if (!ValidatePath(context, file_path))
            return 0;

        VCDHandler.LoadVCD(file_path);
        return 1;
    }
    public static int VCDComp(CommandContext<ServerCommandSource> context)
    {
        if (Misc.CheckStartPos(context))
        {
            return 0;
        }
        VCDHandler.GetCurrentValuesAndCompare(context.getSource().getWorld(), context.getSource());
        return 1;
    }
    public static int CheckWires(CommandContext<ServerCommandSource> context)
    {
        if(Router.cached_hy == null || Router.cached_tpn == null)
        {
            context.getSource().sendError(Text.literal("You must place wires using /chipmaker route before using this command"));
            return 0;
        }
        if (Misc.CheckStartPos(context))
        {
            return 0;
        }

        WireDebugger.CheckWires(context);
        return 1;
    }
    public static int Wipe(CommandContext<ServerCommandSource> context)
    {
        if(Misc.CheckStartPos(context))
        {
            return 0;
        }

        int maxx = Placer.chip_size;
        int maxz = Placer.chip_size;
        int maxy = 0;

        if(Placer.do_vertical)
        {
            maxy = Placer.max_iter;
        }
        else if(Placer.do_actual_place)
        {
            if(Router.max_y != 0)
            {
                maxy = Router.max_y - Placer.start_pos.getY();
            }
            else
            {
                maxy = Placer.Y_MAX_CELL_SIZE;
            }
        }
        else
        {
            maxy = 20;
        }

        for (BlockPos blockPos : BlockPos.iterate(Placer.start_pos.add(maxx, maxy, maxz), Placer.start_pos.add(0, 0, -2)))
        {
            context.getSource().getWorld().setBlockState(blockPos, Blocks.AIR.getDefaultState(), 2 | 816);
        }
        return 1;
    }

    static int GetLegalization(CommandContext<ServerCommandSource> context)
    {
        context.getSource().sendMessage(Text.literal("Do legalization - " + Placer.do_legalization));
        return 1;
    }

    static int SetLegalization(CommandContext<ServerCommandSource> context)
    {
        Placer.do_legalization = BoolArgumentType.getBool(context, "do_legalization");
        context.getSource().sendMessage(Text.literal("Do legalization is now: " + Placer.do_legalization));
        return 1;
    }

    static int GetMaxIter(CommandContext<ServerCommandSource> context)
    {
        context.getSource().sendMessage(Text.literal("Max iter - " + Placer.max_iter));
        return 1;
    }

    static int SetMaxIter(CommandContext<ServerCommandSource> context)
    {
        Placer.max_iter = IntegerArgumentType.getInteger(context, "max_iter");
        context.getSource().sendMessage(Text.literal("Max iter is now: " + Placer.max_iter));
        return 1;
    }

    static int GetForceMul(CommandContext<ServerCommandSource> context)
    {
        context.getSource().sendMessage(Text.literal("Force mul - " + Placer.force_mul));
        return 1;
    }

    static int SetForceMul(CommandContext<ServerCommandSource> context)
    {
        Placer.force_mul = DoubleArgumentType.getDouble(context, "force_mul");
        context.getSource().sendMessage(Text.literal("Force mul is now: " + Placer.force_mul));
        return 1;
    }

    static int GetForceConst(CommandContext<ServerCommandSource> context)
    {
        context.getSource().sendMessage(Text.literal("Force const - " + Placer.force_const));
        return 1;
    }

    static int SetForceConst(CommandContext<ServerCommandSource> context)
    {
        Placer.force_const = DoubleArgumentType.getDouble(context, "force_const");
        context.getSource().sendMessage(Text.literal("Force const is now: " + Placer.force_const));
        return 1;
    }

    static int GetChipSize(CommandContext<ServerCommandSource> context)
    {
        context.getSource().sendMessage(Text.literal("Chip size - " + Placer.chip_size));
        return 1;
    }
    static int SetChipSize(CommandContext<ServerCommandSource> context)
    {
        Placer.chip_size = IntegerArgumentType.getInteger(context, "chip_size");
        context.getSource().sendMessage(Text.literal("Chip size is now: " + Placer.chip_size));
        return 1;
    }

    static int GetMaxSearch(CommandContext<ServerCommandSource> context)
    {
        context.getSource().sendMessage(Text.literal("Router max search - " + Placer.LeeRouterMaxSearch));
        return 1;
    }
    static int SetMaxSearch(CommandContext<ServerCommandSource> context)
    {
        Placer.LeeRouterMaxSearch = IntegerArgumentType.getInteger(context, "router_max_search");
        context.getSource().sendMessage(Text.literal("Router max search is now: " + Placer.LeeRouterMaxSearch));
        return 1;
    }

    static int GetActualPlace(CommandContext<ServerCommandSource> context)
    {
        context.getSource().sendMessage(Text.literal("Do actual place - " + Placer.do_actual_place));
        return 1;
    }

    static int SetActualPlace(CommandContext<ServerCommandSource> context)
    {
        Placer.do_actual_place = BoolArgumentType.getBool(context, "do_actual_place");
        context.getSource().sendMessage(Text.literal("Do actual place is now: " + Placer.do_actual_place));
        return 1;
    }
}
