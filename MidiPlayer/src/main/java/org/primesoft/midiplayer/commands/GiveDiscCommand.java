package org.primesoft.midiplayer.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.primesoft.midiplayer.MidiPlayerMain;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GiveDiscCommand extends BaseCommand {
    public static String SONG_KEY = "SONG_NAME";

    private MidiPlayerMain m_plugin;
    private NamespacedKey discKey;

    public GiveDiscCommand(MidiPlayerMain main){
        m_plugin = main;
        discKey = new NamespacedKey(m_plugin, SONG_KEY);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String name, String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage("You must be a player to send this command.");
            return true;
        }
        Player player = (Player) sender;
        if(args.length < 1){
            sender.sendMessage("You must provide the name of the midi you wish to get the disc of.");
            return true;
        }
        String midi = args[0];
        if((!new File(m_plugin.getDataFolder(),midi).exists()) || ! midi.endsWith(".mid")){
            sender.sendMessage("Invalid midi.");
            return true;
        }

        File discyamlfile = new File(m_plugin.getDataFolder(),"disc.yml");
        if(!discyamlfile.exists()){
            Material[] discs = new Material[]{
                    Material.MUSIC_DISC_5,
                    Material.MUSIC_DISC_11,
                    Material.MUSIC_DISC_13,
                    Material.MUSIC_DISC_CAT,
                    Material.MUSIC_DISC_CHIRP,
                    Material.MUSIC_DISC_FAR,
                    Material.MUSIC_DISC_BLOCKS,
                    Material.MUSIC_DISC_MALL,
                    Material.MUSIC_DISC_MELLOHI,
                    Material.MUSIC_DISC_OTHERSIDE,
                    Material.MUSIC_DISC_PIGSTEP,
                    Material.MUSIC_DISC_STAL,
                    Material.MUSIC_DISC_STRAD,
                    Material.MUSIC_DISC_WAIT,
                    Material.MUSIC_DISC_WARD,
                    Material.MUSIC_DISC_CREATOR,
                    Material.MUSIC_DISC_CREATOR_MUSIC_BOX,
                    Material.MUSIC_DISC_PRECIPICE,
                    Material.MUSIC_DISC_RELIC
            };
            FileConfiguration discyml = YamlConfiguration.loadConfiguration(discyamlfile);
            for(File f : m_plugin.getDataFolder().listFiles()) {
                if(f.getName().endsWith(".mid")){
                    String songName = f.getName().substring(0, f.getName().length() - 4);
                    discyml.set(songName + ".material", discs[ThreadLocalRandom.current().nextInt(discs.length)].name());
                    discyml.set(songName + ".displayname", "MIDI Disc");
                    discyml.set(songName + ".rarity", ItemRarity.COMMON.name());
                    discyml.set(songName + ".lore", Collections.singletonList(songName.replace('_', ' ')));
                }
            }
            try {
                discyml.save(discyamlfile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        FileConfiguration discyml = YamlConfiguration.loadConfiguration(discyamlfile);

        Material discMaterial = Material.MUSIC_DISC_BLOCKS;
        String displayName;
        ItemRarity rarity;
        List<String> lore;
        String songName = args[0].substring(0,args[0].length() - 4);

        if(discyml.contains(songName)){
            discMaterial = Material.matchMaterial(discyml.getString(songName + ".material", discMaterial.name()));
            if (discMaterial == null) {
                discMaterial = Material.MUSIC_DISC_BLOCKS;
                sender.sendMessage("[WARNING] Material not working");
            }
            displayName = discyml.getString(songName + ".displayname");
            lore = discyml.getStringList(songName + ".lore");
            rarity = ItemRarity.valueOf(discyml.getString(songName + ".rarity", "COMMON"));
        }
        else {
            displayName = "MIDI Disc";
            lore = Collections.emptyList();
            rarity = ItemRarity.COMMON;
        }
        ItemStack disc = new ItemStack(discMaterial);
        disc.editMeta((meta) -> {
            meta.displayName(MiniMessage.miniMessage().deserialize(displayName));
            meta.setRarity(rarity);
            meta.setLore(lore);
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(discKey, PersistentDataType.STRING, args[0]);
        });
        player.getInventory().addItem(disc);
        sender.sendMessage("Giving Midi Music disc \"" + songName + "\".");
        return super.onCommand(sender, cmnd, name, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender cs, Command cmnd, String name, String[] args) {
        List<String> midi = new LinkedList<>();
        for(File f : m_plugin.getDataFolder().listFiles()){
            if(f.getName().endsWith(".mid")){
                midi.add(f.getName());
            }
        }
        return midi;
    }
}
