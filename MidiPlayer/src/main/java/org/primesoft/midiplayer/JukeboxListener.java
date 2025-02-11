package org.primesoft.midiplayer;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitScheduler;
import org.primesoft.midiplayer.commands.GiveDiscCommand;
import org.primesoft.midiplayer.midiparser.MidiParser;
import org.primesoft.midiplayer.track.LocationTrack;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class JukeboxListener implements Listener {

    private MidiPlayerMain m_main;
    private HashMap<Location, LocationTrack> activeTracks = new HashMap<>();
    private NamespacedKey discKey;

    public JukeboxListener(MidiPlayerMain main){
        m_main = main;
        discKey = new NamespacedKey(m_main, GiveDiscCommand.SONG_KEY);
    }

    @EventHandler
    public void onJukeboxBreak(BlockBreakEvent event){
        Location loc = event.getBlock().getLocation();
        if(activeTracks.containsKey(loc))
            m_main.getMusicPlayer().removeTrack(activeTracks.remove(loc));
    }

    @EventHandler
    public void onJukeboxInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        Location loc = event.getClickedBlock().getLocation();
        if(activeTracks.containsKey(loc))
            m_main.getMusicPlayer().removeTrack(activeTracks.remove(loc));
    }

    @EventHandler
    public void onJukeboxPlay(GenericGameEvent event) {
        if (event.getEvent() == GameEvent.JUKEBOX_PLAY) {
            Jukebox jukebox = (Jukebox) event.getLocation().getBlock().getState();
            ItemStack current = jukebox.getInventory().getRecord();
            if (current == null)
                return;
            AtomicReference<String> fileNameAtomic = new AtomicReference<>(null);
            current.editMeta((meta) -> {
                PersistentDataContainer data = meta.getPersistentDataContainer();
                fileNameAtomic.set(data.get(discKey, PersistentDataType.STRING));
            });
            String fileName = fileNameAtomic.get();
            if (fileName == null)
                return;
            jukebox.stopPlaying();
            Location boxLocation = event.getLocation();
            LocationTrack locationTrack = new LocationTrack(boxLocation, MidiParser.loadFile(new File(m_main.getDataFolder(), fileName)).getNotes());
            activeTracks.put(boxLocation, locationTrack);
            for(Player player : boxLocation.getWorld().getPlayers())
                locationTrack.addPlayer(player);
            MidiPlayerMain.getInstance().getMusicPlayer().playTrack(locationTrack);
            for(Player player : boxLocation.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(boxLocation) <= 250 * 250) {
                    String text = "Now Playing: " + fileName.substring(0, fileName.length() - 4).replace('_', ' ');
                    String step0 = "97fc2c";  // 10 first ticks
                    String step1 = "972c2c";  // -> 2 ticks
                    String step2 = "972c97";  // -> 8 ticks
                    String step3 = "2c2c97";  // -> 8 ticks
                    String step4 = "2c9797";  // -> 8 ticks
                    String step5 = "2c972c";  // -> 8 ticks
                    String transition = String.format("<transition:#%s:#%s:#%s:#%s:#%s:", step1, step2, step3, step4, step5) + "%f>";
                    player.sendActionBar(MiniMessage.miniMessage()
                            .deserialize(String.format("<color:#%s>", step0) + text));
                    BukkitScheduler sched = Bukkit.getScheduler();
                    for (int tick = 0; tick <= 2; tick++) {
                        int finalTick = tick;
                        sched.runTaskLater(m_main, () -> player.sendActionBar(MiniMessage.miniMessage()
                                .deserialize(String.format(Locale.US, "<transition:#%s:#%s:%f>", step0, step1, finalTick / 2F) + text + "</transition>")), 10 + tick);
                    }
                    for (int tick = 1; tick <= 32; tick++) {
                        int finalTick = tick;
                        sched.runTaskLater(m_main, () -> player.sendActionBar(MiniMessage.miniMessage()
                                .deserialize(String.format(Locale.US, transition, finalTick / 32F) + text + "</transition>")), 12 + tick);
                    }
                    /*
                    for (int tick = 0; tick <= 28; tick++) {
                        int finalTick = tick;
                        sched.runTaskLater(m_main, () -> {
                            String mess = String.format("<color:#%02x%s>", (int) (0xFF * (28 - finalTick) / 28F), step5);
                                player.sendActionBar(MiniMessage.miniMessage()
                                        .deserialize(mess + text));
                            m_main.getLogger().info(mess);
                        }, 32 + tick);
                    }
                    */
                }
            }
        }
    }
}
