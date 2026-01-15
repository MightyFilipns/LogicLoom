package com.mightyfilipns.chipmakermc;


import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class CellTypeSuggestionProvider implements SuggestionProvider<ServerCommandSource>
{
    public static SuggestionProvider<ServerCommandSource> Provider(){ return new CellTypeSuggestionProvider();}

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        for (CellType value : CellType.values())
        {
            builder.suggest(value.name());
        }

        return builder.buildFuture();
    }
}
