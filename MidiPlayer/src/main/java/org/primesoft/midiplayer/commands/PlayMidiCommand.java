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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.primesoft.midiplayer.MusicPlayer;
import org.primesoft.midiplayer.midiparser.NoteFrame;
import org.primesoft.midiplayer.midiparser.NoteTrack;
import org.primesoft.midiplayer.track.BasePlayerTrack;
import org.primesoft.midiplayer.track.LocationTrack;
import org.primesoft.midiplayer.track.PlayerTrack;

import java.util.*;


/**
 * Play midi command
 * @author SBPrime
 */
public class PlayMidiCommand extends BaseCommand implements Listener {

    private final MusicPlayer m_player;
    protected static final Map<UUID, BasePlayerTrack> m_tracks = new HashMap<>();
    private final JavaPlugin m_plugin;

    public PlayMidiCommand(@NotNull JavaPlugin plugin, @NotNull MusicPlayer player) {
        m_plugin = plugin;
        m_player = player;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();
        synchronized (m_tracks) {
            BasePlayerTrack track = m_tracks.remove(uuid);
            if (track != null)
                m_player.removeTrack(track);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (args.length < 1 || args.length > 2)
            return false;

        List<Player> audience = BaseCommand.getPlayers(sender, args, 1, true);
        if (audience == null || audience.isEmpty())
            return true;

        NoteTrack noteTrack = BaseCommand.getNoteTrack(m_plugin, sender, args, 0);
        if (noteTrack == null)
            return false;
        else if (noteTrack.isError())
            return true;

        final NoteFrame[] notes = noteTrack.getNotes();

        final PlayerTrack track = new PlayerTrack(audience.toArray(new Player[0]), notes);
        audience.forEach(player -> {
            UUID uuid = player.getUniqueId();
            synchronized (m_tracks) {
                BasePlayerTrack oldTrack = m_tracks.put(uuid, track);
                if (oldTrack != null) {
                    oldTrack.removePlayer(player);
                    if (oldTrack.countPlayers() == 0) {
                        m_player.removeTrack(oldTrack);
                        if (oldTrack instanceof LocationTrack lt)
                            JukeboxListener.activeJukebox.remove(lt.getLocation());
                    }
                }
            }
        });
        m_player.playTrack(track);

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1)
            return getMIDIList(m_plugin, args[0]);
        else if (args.length == 2)
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return null;
    }
}
