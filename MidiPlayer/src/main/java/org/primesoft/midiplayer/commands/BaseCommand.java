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

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.primesoft.midiplayer.midiparser.MidiParser;
import org.primesoft.midiplayer.midiparser.NoteTrack;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 *
 * @author prime
 */
public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        return true;
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        return new ArrayList<>();
    }

    public static @Nullable List<Player> getPlayers(@NotNull CommandSender sender, @NotNull String @NotNull [] args, int argId, boolean acceptSender) {
        List<Entity> entities = getEntities(sender, args, argId, acceptSender);
        if (entities == null)
            return null;
        return entities.stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList();
    }

    public static @Nullable List<Entity> getEntities(@NotNull CommandSender sender, @NotNull String @NotNull [] args, int argId, boolean acceptSender) {
        List<Entity> audience = null;
        boolean hasTargetArg = args.length > argId;
        if (hasTargetArg) {
            Player target = Bukkit.getPlayer(args[argId]);
            if (target == null) {
                try {
                    audience = Bukkit.selectEntities(sender, args[argId]);
                }
                catch (IllegalArgumentException ex) {
                    sender.sendMessage("Unknown player: " + args[argId]);
                }
            }
            else {
                audience = List.of(target);
            }
        }
        else if (acceptSender) {
            if (sender instanceof Player player)
                audience = List.of(player);
            else
                sender.sendMessage("You must provide a target player if you are not one");
        }
        else {
            sender.sendMessage("You must provide a target player if you are not one");
        }
        return audience;
    }

    public static @Nullable String getTrackName(@NotNull JavaPlugin plugin, @NotNull CommandSender sender, @NotNull String @NotNull [] args, int argId) {
        String fileName = args.length > argId ? args[argId] : null;
        if (fileName == null) {
            sender.sendMessage("No MIDI track specified");
            return null;
        }

        if (!new File(plugin.getDataFolder(), fileName).exists()) {
            sender.sendMessage("The specified file does not exist");
            return null;
        }
        else if (!fileName.endsWith(".mid")) {
            sender.sendMessage("MIDI files must end with .mid");
            return null;
        }

        return fileName;
    }

    public static @Nullable NoteTrack getNoteTrack(@NotNull JavaPlugin plugin, @NotNull CommandSender sender, @NotNull String @NotNull [] args, int argId) {
        String fileName = args.length > argId ? args[argId] : null;
        if (fileName == null) {
            sender.sendMessage("No MIDI track specified");
            return null;
        }

        NoteTrack noteTrack = MidiParser.loadFile(new File(plugin.getDataFolder(), fileName));
        if (noteTrack == null) {
            sender.sendMessage("Error loading MIDI track");
            return null;
        } else if (noteTrack.isError()) {
            sender.sendMessage("Error loading MIDI track: " + noteTrack.getMessage());
        }

        return noteTrack;
    }

    public static @NotNull List<String> getMIDIList(@NotNull JavaPlugin plugin, @NotNull String nameStart) {
        List<String> midi = new LinkedList<>();
        File[] files = plugin.getDataFolder().listFiles();
        if (files == null)
            return midi;

        nameStart = nameStart.toLowerCase();
        for (File f : files) {
            String fileName = f.getName();
            if (fileName.endsWith(".mid") && fileName.toLowerCase().startsWith(nameStart))
                midi.add(f.getName());
        }
        return midi;
    }
}
