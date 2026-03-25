package com.mightyfilipns.logicloom;

import com.mightyfilipns.logicloom.JsonLoader.CellType;
import com.mightyfilipns.logicloom.JsonLoader.JsonDesign;
import com.mightyfilipns.logicloom.JsonLoader.JsonLoadCommand;
import com.mightyfilipns.logicloom.JsonLoader.PortDirection;
import com.mightyfilipns.logicloom.Misc.CellTypeSuggestionProvider;
import com.mightyfilipns.logicloom.Misc.ForceWireModeSuggestionProvider;
import com.mightyfilipns.logicloom.Misc.VCDHandler;
import com.mightyfilipns.logicloom.Placment.FixedPointsManager;
import com.mightyfilipns.logicloom.Placment.Placer;
import com.mightyfilipns.logicloom.Routing.Misc;
import com.mightyfilipns.logicloom.Routing.Router;
import com.mightyfilipns.logicloom.Routing.TestCmds;
import com.mightyfilipns.logicloom.Misc.WireDebugger;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
//import net.minecraft.resource.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;

import static com.mightyfilipns.logicloom.JsonLoader.JsonLoadCommand.ValidatePath;
import static com.mightyfilipns.logicloom.Placment.Placer.placement_data;

public class LogicLoom implements ModInitializer
{
    public static JsonDesign loaded_design = null;

    @Override
    public void onInitialize()
    {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("logicloom").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.literal("load_json").then(Commands.argument("file_path" ,StringArgumentType.greedyString()).executes(JsonLoadCommand::LoadJSONDesign)))
                    .then(Commands.literal("start_pos").executes(LogicLoom::PrintStartPos).then(Commands.argument("block_pos", BlockPosArgument.blockPos()).executes(LogicLoom::SetBlockPos)))
                    .then(Commands.literal("place").executes(Placer::PlaceDesign))
                    .then(Commands.literal("param")
                            .then(Commands.literal("max_iter")
                                            .then(Commands.argument("max_iter", IntegerArgumentType.integer(0, 10_000)).executes(LogicLoom::SetMaxIter))
                            .executes(LogicLoom::GetMaxIter)
                            )
                            .then(Commands.literal("router_max_search")
                                    .then(Commands.argument("router_max_search", IntegerArgumentType.integer(0)).executes(LogicLoom::SetMaxSearch))
                                    .executes(LogicLoom::GetMaxSearch)
                            )
                            .then(Commands.literal("force_const")
                                            .then(Commands.argument("force_const", DoubleArgumentType.doubleArg()).executes(LogicLoom::SetForceConst))
                            .executes(LogicLoom::GetForceConst)
                            )
                            .then(Commands.literal("force_mul")
                                    .then(Commands.argument("force_mul", DoubleArgumentType.doubleArg()).executes(LogicLoom::SetForceMul))
                                    .executes(LogicLoom::GetForceMul)
                            )
                            .then(Commands.literal("chip_size")
                                    .then(Commands.argument("chip_size", IntegerArgumentType.integer(0, 10_000)).executes(LogicLoom::SetChipSize))
                            .executes(LogicLoom::GetChipSize)
                            )
                            .then(Commands.literal("do_actual_place")
                                    .then(Commands.argument("do_actual_place", BoolArgumentType.bool()).executes(LogicLoom::SetActualPlace))
                            .executes(LogicLoom::GetActualPlace)
                            )
                            .then(Commands.literal("do_legalization")
                                    .then(Commands.argument("do_legalization", BoolArgumentType.bool()).executes(LogicLoom::SetLegalization))
                            .executes(LogicLoom::GetLegalization)
                            )
                    )
                    .then(Commands.literal("fixed_points")
                            .then(Commands.literal("add_abs")
                                    .then(Commands.argument("position", BlockPosArgument.blockPos())
                                            .then(Commands.argument("strength", DoubleArgumentType.doubleArg())
                                                    .executes(FixedPointsManager::AddPointAbs))))
                            .then(Commands.literal("add_rel")
                                    .then(Commands.argument("x", IntegerArgumentType.integer())
                                            .then(Commands.argument("z", IntegerArgumentType.integer())
                                                    .then(Commands.argument("strength", DoubleArgumentType.doubleArg())
                                                            .executes(FixedPointsManager::AddPointRel)))))
                            .then(Commands.literal("remove")
                                    .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                            .executes(FixedPointsManager::RemovePoint)))
                            .then(Commands.literal("list").executes(FixedPointsManager::ListPoints))
                            .then(Commands.literal("clear").executes(FixedPointsManager::ClearAllPoints))
                    )
                    .then(Commands.literal("misc")
                            .then(Commands.literal("force_power_wire").then(Commands.argument("type", StringArgumentType.word()).suggests(ForceWireModeSuggestionProvider.Provider()).executes(WireDebugger::ForcePowerWires)))
                            .then(Commands.literal("check_wires").executes(LogicLoom::CheckWires))
                            .then(Commands.literal("show_boundary").executes(LogicLoom::ShowBoundBox))
                            .then(Commands.literal("wipe").executes(LogicLoom::Wipe))
                    )
                    .then(Commands.literal("route").executes(Router::DoRouting))
                    .then(Commands.literal("debug")
                            .then(Commands.literal("place_cache").executes(Placer::PlaceCache))
                            .then(Commands.literal("vcddebug").then(Commands.argument("file_path", StringArgumentType.greedyString()).executes(LogicLoom::VCDDebug)))
                            .then(Commands.literal("vcdcomp").executes(LogicLoom::VCDComp))
                            .then(Commands.literal("test_cell_port").then(Commands.argument("cell_type", StringArgumentType.word()).suggests(CellTypeSuggestionProvider.Provider()).then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(LogicLoom::TestCellPorts))))
                            .then(Commands.literal("test_tree").then(Commands.argument("start_pos", BlockPosArgument.blockPos()).executes(TestCmds::TestTree)))
                            .then(Commands.literal("test_hyper").then(Commands.argument("start_pos", BlockPosArgument.blockPos()).executes(TestCmds::TestHyperGraph)))
                            .then(Commands.literal("test_lee_router").executes(TestCmds::TestLeeRouter))
                            .then(Commands.literal("rebuild_cached").executes(TestCmds::RebuildCached))
                            .then(Commands.literal("test_vert").then(Commands.argument("up", BlockPosArgument.blockPos()).then(Commands.argument("down", BlockPosArgument.blockPos()).executes(TestCmds::TestVerticalBuilder))))
                            .then(Commands.literal("test_wire").then(Commands.argument("index", IntegerArgumentType.integer(0)).executes(TestCmds::BuildWire)))
                            .then(Commands.literal("test_template").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(TestCmds::TestTemplate)))
                    )
            );
        });

        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
            Identifier.fromNamespaceAndPath("logicloom", "logic_loom_res"),
            (ResourceManagerReloadListener) manager -> {
                var dat2 = manager.getResource(Identifier.parse("logicloom:flute_data/post9.dat"));
                var dat1 = manager.getResource(Identifier.parse("logicloom:flute_data/powv9.dat"));

                try {
                    Misc.SetupFlute(dat1.get().open(), dat2.get().open());
                } catch (Exception e) {
                    System.out.println("Failed to setup flute Error: " +  e.getMessage());
                }
            }
        );
    }

    public static int PrintStartPos(CommandContext<CommandSourceStack> context)
    {
        if (Placer.start_pos == null)
        {
            context.getSource().sendSystemMessage(Component.nullToEmpty("No position set"));
        }
        else
        {
            context.getSource().sendSystemMessage(Component.nullToEmpty("Start set to " + Placer.start_pos));
        }
        return 1;
    }

    public static int TestCellPorts(CommandContext<CommandSourceStack> context)
    {
        var s = StringArgumentType.getString(context, "cell_type");
        CellType ct = CellType.valueOf(s);
        BlockPos paste_pos = BlockPosArgument.getBlockPos(context, "pos");

        ServerLevel w = context.getSource().getLevel();
        var t = w.getStructureManager();
        var opt = t.get(ct.getIdentifier());
        var tmplt = opt.get();

        tmplt.placeInWorld(w, paste_pos, null, placement_data, null, 3);

        for (CellType.PortInfo port : ct.ports)
        {
            var ps = paste_pos.offset(port.relpos());

            w.setBlockAndUpdate(ps, port.dir() == PortDirection.Input ? Blocks.BLUE_WOOL.defaultBlockState() : Blocks.RED_WOOL.defaultBlockState() );

            w.setBlockAndUpdate(ps.offset(0, 1, 0), Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8));
            ((SignBlockEntity) w.getBlockEntity(ps.offset(0, 1, 0))).setText(new SignText().setMessage(1, Component.nullToEmpty(port.name())), true);
        }

        return 1;
    }

    private static int SetBlockPos(CommandContext<CommandSourceStack> context)
    {
        Placer.start_pos = BlockPosArgument.getBlockPos(context, "block_pos");
        context.getSource().sendSystemMessage(Component.literal("Start pos set to " + Placer.start_pos));
        return 1;
    }

    public static int ShowBoundBox(CommandContext<CommandSourceStack> context)
    {
        if (Misc.CheckStartPos(context))
        {
            return 0;
        }

        var p = Placer.start_pos.offset(0, 10 ,0);
        for (BlockPos blockPos : BlockPos.betweenClosed(p, p.offset(Placer.chip_size, 0, 0)))
        {
            context.getSource().getLevel().setBlockAndUpdate(blockPos, Blocks.BLUE_WOOL.defaultBlockState());
        }
        for (BlockPos blockPos : BlockPos.betweenClosed(p, p.offset(0, 0, Placer.chip_size)))
        {
            context.getSource().getLevel().setBlockAndUpdate(blockPos, Blocks.BLUE_WOOL.defaultBlockState());
        }

        return 1;
    }

    public static int VCDDebug(CommandContext<CommandSourceStack> context)
    {
        String file_path = StringArgumentType.getString(context, "file_path");

        if (!ValidatePath(context, file_path))
            return 0;

        VCDHandler.LoadVCD(file_path);
        return 1;
    }
    public static int VCDComp(CommandContext<CommandSourceStack> context)
    {
        if (Misc.CheckStartPos(context))
        {
            return 0;
        }
        VCDHandler.GetCurrentValuesAndCompare(context.getSource().getLevel(), context.getSource());
        return 1;
    }
    public static int CheckWires(CommandContext<CommandSourceStack> context)
    {
        if(Router.cached_hy == null || Router.cached_tpn == null)
        {
            context.getSource().sendFailure(Component.literal("You must place wires using /logicloom route before using this command"));
            return 0;
        }
        if (Misc.CheckStartPos(context))
        {
            return 0;
        }

        WireDebugger.CheckWires(context);
        return 1;
    }
    public static int Wipe(CommandContext<CommandSourceStack> context)
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

        for (BlockPos blockPos : BlockPos.betweenClosed(Placer.start_pos.offset(maxx, maxy, maxz), Placer.start_pos.offset(0, 0, -2)))
        {
            context.getSource().getLevel().setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2 | 816);
        }
        return 1;
    }

    static int GetLegalization(CommandContext<CommandSourceStack> context)
    {
        context.getSource().sendSystemMessage(Component.literal("Do legalization - " + Placer.do_legalization));
        return 1;
    }

    static int SetLegalization(CommandContext<CommandSourceStack> context)
    {
        Placer.do_legalization = BoolArgumentType.getBool(context, "do_legalization");
        context.getSource().sendSystemMessage(Component.literal("Do legalization is now: " + Placer.do_legalization));
        return 1;
    }

    static int GetMaxIter(CommandContext<CommandSourceStack> context)
    {
        context.getSource().sendSystemMessage(Component.literal("Max iter - " + Placer.max_iter));
        return 1;
    }

    static int SetMaxIter(CommandContext<CommandSourceStack> context)
    {
        Placer.max_iter = IntegerArgumentType.getInteger(context, "max_iter");
        context.getSource().sendSystemMessage(Component.literal("Max iter is now: " + Placer.max_iter));
        return 1;
    }

    static int GetForceMul(CommandContext<CommandSourceStack> context)
    {
        context.getSource().sendSystemMessage(Component.literal("Force mul - " + Placer.force_mul));
        return 1;
    }

    static int SetForceMul(CommandContext<CommandSourceStack> context)
    {
        Placer.force_mul = DoubleArgumentType.getDouble(context, "force_mul");
        context.getSource().sendSystemMessage(Component.literal("Force mul is now: " + Placer.force_mul));
        return 1;
    }

    static int GetForceConst(CommandContext<CommandSourceStack> context)
    {
        context.getSource().sendSystemMessage(Component.literal("Force const - " + Placer.force_const));
        return 1;
    }

    static int SetForceConst(CommandContext<CommandSourceStack> context)
    {
        Placer.force_const = DoubleArgumentType.getDouble(context, "force_const");
        context.getSource().sendSystemMessage(Component.literal("Force const is now: " + Placer.force_const));
        return 1;
    }

    static int GetChipSize(CommandContext<CommandSourceStack> context)
    {
        context.getSource().sendSystemMessage(Component.literal("Chip size - " + Placer.chip_size));
        return 1;
    }
    static int SetChipSize(CommandContext<CommandSourceStack> context)
    {
        Placer.chip_size = IntegerArgumentType.getInteger(context, "chip_size");
        context.getSource().sendSystemMessage(Component.literal("Chip size is now: " + Placer.chip_size));
        return 1;
    }

    static int GetMaxSearch(CommandContext<CommandSourceStack> context)
    {
        context.getSource().sendSystemMessage(Component.literal("Router max search - " + Placer.LeeRouterMaxSearch));
        return 1;
    }
    static int SetMaxSearch(CommandContext<CommandSourceStack> context)
    {
        Placer.LeeRouterMaxSearch = IntegerArgumentType.getInteger(context, "router_max_search");
        context.getSource().sendSystemMessage(Component.literal("Router max search is now: " + Placer.LeeRouterMaxSearch));
        return 1;
    }

    static int GetActualPlace(CommandContext<CommandSourceStack> context)
    {
        context.getSource().sendSystemMessage(Component.literal("Do actual place - " + Placer.do_actual_place));
        return 1;
    }

    static int SetActualPlace(CommandContext<CommandSourceStack> context)
    {
        Placer.do_actual_place = BoolArgumentType.getBool(context, "do_actual_place");
        context.getSource().sendSystemMessage(Component.literal("Do actual place is now: " + Placer.do_actual_place));
        return 1;
    }
}
