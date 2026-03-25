package com.mightyfilipns.logicloom.Misc;


import com.mightyfilipns.logicloom.JsonLoader.CellType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;

public class CellTypeSuggestionProvider implements SuggestionProvider<CommandSourceStack>
{
    public static SuggestionProvider<CommandSourceStack> Provider(){ return new CellTypeSuggestionProvider();}

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        for (CellType value : CellType.values())
        {
            builder.suggest(value.name());
        }

        return builder.buildFuture();
    }
}
