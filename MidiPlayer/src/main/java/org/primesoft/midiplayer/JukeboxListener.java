package org.primesoft.midiplayer;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.primesoft.midiplayer.midiparser.MidiParser;
import org.primesoft.midiplayer.track.LocationTrack;

import java.io.File;
import java.util.HashMap;

public class JukeboxListener implements Listener {

    private MidiPlayerMain m_main;
    private HashMap<Location, LocationTrack> activeTracks = new HashMap<>();

    public JukeboxListener(MidiPlayerMain main){
        this.m_main = main;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event){
        if(activeTracks.containsKey(event.getBlock().getLocation())){
            m_main.getMusicPlayer().removeTrack(activeTracks.remove(event.getBlock().getLocation()));
        }
    }

    @EventHandler
    public void onJukeboxInteract(PlayerInteractEvent event){
        if(event.getAction()== Action.RIGHT_CLICK_BLOCK){
            if(event.getClickedBlock().getType()== Material.JUKEBOX){
                String filename = null;
                if(event.getItem() != null && event.getItem().getItemMeta().hasLore()){
                    for(String lore : event.getItem().getItemMeta().getLore()){
                        if(lore.contains("Filename:")){
                            filename=ChatColor.stripColor(lore.split(":")[1]);
                            break;
                        }
                    }
                }
                if(filename == null)
                    return;
                if(activeTracks.containsKey(event.getClickedBlock().getLocation())){
                    m_main.getMusicPlayer().removeTrack(activeTracks.remove(event.getClickedBlock().getLocation()));
                    return;
                }

                LocationTrack locationTrack = new LocationTrack(event.getClickedBlock().getLocation(),MidiParser.loadFile(new File(m_main.getDataFolder(),filename)).getNotes());
                activeTracks.put(event.getClickedBlock().getLocation(),locationTrack);
                for(Player player : event.getPlayer().getWorld().getPlayers())
                locationTrack.addPlayer(player);
                MidiPlayerMain.getInstance().getMusicPlayer().playTrack(locationTrack);
                String finalFilename = filename;
                new BukkitRunnable(){
                    public void run(){
                        for(Player player : event.getPlayer().getWorld().getPlayers()){
                            if(player.getLocation().distanceSquared(event.getClickedBlock().getLocation()) <= 250*250){
                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("Now Playing: "+ finalFilename.substring(0, finalFilename.length()-4), ChatColor.GOLD));
                                Jukebox jukebox = (Jukebox) event.getClickedBlock().getState();
                                jukebox.stopPlaying();
                            }
                        }
                    }
                }.runTaskLater(m_main,1);
            }
        }
    }
}
