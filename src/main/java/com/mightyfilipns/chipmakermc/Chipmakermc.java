package com.mightyfilipns.chipmakermc;

import com.mightyfilipns.chipmakermc.JsonLoader.CellType;
import com.mightyfilipns.chipmakermc.JsonLoader.JsonDesign;
import com.mightyfilipns.chipmakermc.JsonLoader.JsonLoadCommand;
import com.mightyfilipns.chipmakermc.JsonLoader.PortDirection;
import com.mightyfilipns.chipmakermc.Misc.CellTypeSuggestionProvider;
import com.mightyfilipns.chipmakermc.Misc.VCDHandler;
import com.mightyfilipns.chipmakermc.Placment.Placer;
import com.mightyfilipns.chipmakermc.Routing.Misc;
import com.mightyfilipns.chipmakermc.Routing.Router;
import com.mightyfilipns.chipmakermc.Routing.TestCmds;
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

import static com.mightyfilipns.chipmakermc.Placment.Placer.placement_data;

public class Chipmakermc implements ModInitializer
{
    public static JsonDesign loaded_design = null;

    @Override
    public void onInitialize()
    {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("chipmaker").requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                    .then(CommandManager.literal("load_json").then(CommandManager.argument("file_path" ,StringArgumentType.greedyString()).executes(JsonLoadCommand::LoadJSONDesign)).executes(JsonLoadCommand::LoadJSONDesign))
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
                            .then(CommandManager.literal("do_actual_place")
                                    .then(CommandManager.argument("do_actual_place", BoolArgumentType.bool()).executes(Chipmakermc::SetActualPlace))
                            .executes(Chipmakermc::GetActualPlace)
                            )
                    )
                    .then(CommandManager.literal("wipe").executes(Chipmakermc::Wipe))
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
                            .then(CommandManager.literal("test_template")
                                    .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                    .executes(TestCmds::TestTemplate)))
                    )
                    .then(CommandManager.literal("debug")
                            .then(CommandManager.literal("vcddebug").executes(Chipmakermc::VCDDebug))
                            .then(CommandManager.literal("vcdcomp").executes(Chipmakermc::VCDComp))
                            .then(CommandManager.literal("check_wires").executes(Chipmakermc::CheckWires))
                            .then(CommandManager.literal("set_block_pos").then(CommandManager.argument("block_pos", BlockPosArgumentType.blockPos()).executes(Chipmakermc::SetBlockPos)))
                            .then(CommandManager.literal("test_cell_port").then(CommandManager.argument("cell_type", StringArgumentType.word()).suggests(CellTypeSuggestionProvider.Provider()).then(CommandManager.argument("pos", BlockPosArgumentType.blockPos()).executes(Chipmakermc::TestCellPorts))))
                            .then(CommandManager.literal("show_boundary").executes(Chipmakermc::ShowBoundBox)))
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

    private static int SetBlockPos(CommandContext<ServerCommandSource> serverCommandSourceCommandContext)
    {
        Placer.last_pos = BlockPosArgumentType.getBlockPos(serverCommandSourceCommandContext, "block_pos");
        return 1;
    }

    public static int ShowBoundBox(CommandContext<ServerCommandSource> context)
    {
        var p = Placer.last_pos.add(0, 10 ,0);
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
        VCDHandler.LoadVCD("/home/Filip/.teroshdl/build/dump.vcd");
        return 1;
    }
    public static int VCDComp(CommandContext<ServerCommandSource> context)
    {
        VCDHandler.GetCurrentValuesAndCompare(context.getSource().getWorld());
        return 1;
    }
    public static int CheckWires(CommandContext<ServerCommandSource> context)
    {
        VCDHandler.CheckWires(context.getSource().getWorld());
        return 1;
    }
    public static int Wipe(CommandContext<ServerCommandSource> context)
    {
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
                maxy = Router.max_y - Placer.last_pos.getY();
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

        for (BlockPos blockPos : BlockPos.iterate(Placer.last_pos.add(maxx, maxy, maxz), Placer.last_pos.add(0, 0, -2)))
        {
            context.getSource().getWorld().setBlockState(blockPos, Blocks.AIR.getDefaultState(), 2 | 816);
        }
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
