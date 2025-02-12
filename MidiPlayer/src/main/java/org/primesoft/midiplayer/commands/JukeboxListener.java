package org.primesoft.midiplayer.commands;

import net.kyori.adventure.audience.Audience;
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
import org.jetbrains.annotations.NotNull;
import org.primesoft.midiplayer.MidiPlayerMain;
import org.primesoft.midiplayer.MusicPlayer;
import org.primesoft.midiplayer.midiparser.MidiParser;
import org.primesoft.midiplayer.track.BasePlayerTrack;
import org.primesoft.midiplayer.track.LocationTrack;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class JukeboxListener implements Listener {

    private MidiPlayerMain m_main;
    protected static final Map<Location, LocationTrack> activeJukebox = new ConcurrentHashMap<>();
    private NamespacedKey discKey;

    public JukeboxListener(@NotNull MidiPlayerMain main) {
        m_main = main;
        discKey = new NamespacedKey(m_main, GiveDiscCommand.SONG_KEY);
    }

    @EventHandler
    public void onJukeboxBreak(BlockBreakEvent event){
        Location loc = event.getBlock().getLocation();
        m_main.getMusicPlayer().removeTrack(activeJukebox.remove(loc));
    }

    @EventHandler
    public void onJukeboxInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        Location loc = event.getClickedBlock().getLocation();
        m_main.getMusicPlayer().removeTrack(activeJukebox.remove(loc));
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
            Player[] audience = boxLocation.getWorld().getPlayers().parallelStream()
                    .filter(p -> p.getLocation().distanceSquared(boxLocation) <= 62500)
                    .toArray(Player[]::new);
            LocationTrack locationTrack = new LocationTrack(
                    boxLocation,
                    audience,
                    MidiParser.loadFile(new File(m_main.getDataFolder(), fileName)).getNotes());
            activeJukebox.put(boxLocation, locationTrack);
            MusicPlayer m_player = m_main.getMusicPlayer();
            Arrays.stream(audience).forEach(p -> {
                synchronized (PlayMidiCommand.m_tracks) {
                    BasePlayerTrack oldTrack = PlayMidiCommand.m_tracks.put(p.getUniqueId(), locationTrack);
                    if (oldTrack != null) {
                        oldTrack.removePlayer(p);
                        if (oldTrack.countPlayers() == 0) {
                            m_player.removeTrack(oldTrack);
                            if (oldTrack instanceof LocationTrack lt)
                                activeJukebox.remove(lt.getLocation());
                        }
                    }
                }
            });

            m_player.playTrack(locationTrack);

            Audience targets = Audience.audience(audience);  // 250^2 = 62 500

            String text = "Now Playing: " + fileName.substring(0, fileName.length() - 4).replace('_', ' ');
            String step0 = "97fc2c";  // 10 first ticks
            String step1 = "972c2c";  // -> 2 ticks
            String step2 = "972c97";  // -> 8 ticks
            String step3 = "2c2c97";  // -> 8 ticks
            String step4 = "2c9797";  // -> 8 ticks
            String step5 = "2c972c";  // -> 8 ticks
            String transition = String.format("<transition:#%s:#%s:#%s:#%s:#%s:", step1, step2, step3, step4, step5) + "%f>";
            targets.sendActionBar(MiniMessage.miniMessage()
                    .deserialize(String.format("<color:#%s>", step0) + text));
            BukkitScheduler sched = Bukkit.getScheduler();
            for (int tick = 0; tick <= 2; tick++) {
                int finalTick = tick;
                sched.runTaskLater(m_main, () -> targets.sendActionBar(MiniMessage.miniMessage()
                        .deserialize(String.format(Locale.US, "<transition:#%s:#%s:%f>", step0, step1, finalTick / 2F) + text + "</transition>")), 10 + tick);
            }
            for (int tick = 1; tick <= 32; tick++) {
                int finalTick = tick;
                sched.runTaskLater(m_main, () -> targets.sendActionBar(MiniMessage.miniMessage()
                        .deserialize(String.format(Locale.US, transition, finalTick / 32F) + text + "</transition>")), 12 + tick);
            }
            /*
            for (int tick = 0; tick <= 28; tick++) {
                int finalTick = tick;
                sched.runTaskLater(m_main, () -> {
                    String mess = String.format("<color:#%02x%s>", (int) (0xFF * (28 - finalTick) / 28F), step5);
                        targets.sendActionBar(MiniMessage.miniMessage()
                                .deserialize(mess + text));
                    m_main.getLogger().info(mess);
                }, 32 + tick);
            }
            */
        }
    }
}
