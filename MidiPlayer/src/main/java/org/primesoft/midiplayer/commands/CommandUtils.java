/*
 * MidiPlayer a plugin that allows you to play custom music.
 * Copyright (c) 2014, SBPrime <https://github.com/SBPrime/>
 * Copyright (c) MidiPlayer contributors
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted free of charge provided that the following 
 * conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution,
 * 3. Redistributions of source code, with or without modification, in any form 
 *    other then free of charge is not allowed,
 * 4. Redistributions in binary form in any form other then free of charge is 
 *    not allowed.
 * 5. Any derived work based on or containing parts of this software must reproduce 
 *    the above copyright notice, this list of conditions and the following 
 *    disclaimer in the documentation and/or other materials provided with the 
 *    derived work.
 * 6. The original author of the software is allowed to change the license 
 *    terms or the entire license of the software as he sees fit.
 * 7. The original author of the software is allowed to sublicense the software 
 *    or its parts using any license terms he sees fit.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.primesoft.midiplayer.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.primesoft.midiplayer.midiparser.MidiParser;
import org.primesoft.midiplayer.midiparser.NoteTrack;

import java.io.File;
import java.util.List;


public class CommandUtils {

    @Contract("_, _, _, !null -> !null")
    public static <T> @Nullable T getArgumentOrDefault(CommandContext<CommandSourceStack> ctx, String name, Class<T> clazz, T def) {
        T arg = def;
        try {
            arg = ctx.getArgument(name, clazz);
        }
        catch (IllegalArgumentException ex) {
            if (!ex.getMessage().equals("No such argument '" + name + "' exists on this command")) {
                ctx.getSource().getSender().sendRichMessage("<red>There was an error while running this command");
                ctx.getSource().getSender().sendRichMessage("<red>Please check the console for details");
                throw ex;
            }
        }
        return arg;
    }

    public static @Nullable List<Player> getPlayers(CommandContext<CommandSourceStack> ctx, String argument, boolean defaultToSelf) throws CommandSyntaxException {
        List<Player> audience;
        PlayerSelectorArgumentResolver selector = CommandUtils.getArgumentOrDefault(ctx, argument, PlayerSelectorArgumentResolver.class, null);
        if (selector == null) {
            if (defaultToSelf && ctx.getSource().getExecutor() instanceof Player player) {
                audience = List.of(player);
            }
            else {
                ctx.getSource().getSender().sendRichMessage("<red>You have to specify a player if the command is not run by one");
                return null;
            }
        }
        else {
            audience = selector.resolve(ctx.getSource());
        }
        return audience;
    }

    public static @Nullable String getTrackName(@NotNull JavaPlugin plugin, CommandContext<CommandSourceStack> ctx, String argument) {
        String fileName = ctx.getArgument(argument, String.class);
        if (fileName == null) {
            ctx.getSource().getSender().sendRichMessage("<red>No MIDI track specified");
            return null;
        }

        if (!new File(plugin.getDataFolder(), fileName).exists()) {
            ctx.getSource().getSender().sendRichMessage("<red>The specified file does not exist");
            return null;
        }
        else if (!fileName.endsWith(".mid")) {
            ctx.getSource().getSender().sendRichMessage("<red>MIDI files must end with .mid");
            return null;
        }

        return fileName;
    }

    public static @Nullable NoteTrack getNoteTrack(@NotNull JavaPlugin plugin, CommandContext<CommandSourceStack> ctx, String argument) {
        String fileName = ctx.getArgument(argument, String.class);
        if (fileName == null) {
            ctx.getSource().getSender().sendRichMessage("<red>No MIDI track specified");
            return null;
        }

        NoteTrack noteTrack = MidiParser.loadFile(new File(plugin.getDataFolder(), fileName));
        if (noteTrack == null) {
            ctx.getSource().getSender().sendRichMessage("<red>Error loading MIDI track");
            return null;
        } else if (noteTrack.isError()) {
            ctx.getSource().getSender().sendRichMessage("<red>Error loading MIDI track: " + noteTrack.getMessage());
        }

        return noteTrack;
    }
}
