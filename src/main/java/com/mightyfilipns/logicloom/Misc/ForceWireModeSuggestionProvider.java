package com.mightyfilipns.logicloom.Misc;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class ForceWireModeSuggestionProvider  implements SuggestionProvider<ServerCommandSource>
{
    public static SuggestionProvider<ServerCommandSource> Provider(){ return new ForceWireModeSuggestionProvider();}

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        for (var value : WireDebugger.ForceWireMode.values())
        {
            builder.suggest(value.name());
        }

        return builder.buildFuture();
    }
}
