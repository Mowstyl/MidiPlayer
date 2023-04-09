package org.primesoft.midiplayer.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.primesoft.midiplayer.MidiPlayerMain;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GiveDiscCommand extends BaseCommand{

    private MidiPlayerMain m_plugin;

    public GiveDiscCommand(MidiPlayerMain main){
        this.m_plugin = main;
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
                    Material.MUSIC_DISC_WARD
            };
            FileConfiguration discyml = YamlConfiguration.loadConfiguration(discyamlfile);
            for(File f : m_plugin.getDataFolder().listFiles()){
                if(f.getName().endsWith(".mid")){
                    discyml.set(f.getName().substring(0,f.getName().length()-4)+".material",discs[ThreadLocalRandom.current().nextInt(discs.length)].name());
                    discyml.set(f.getName().substring(0,f.getName().length()-4)+".displayname",f.getName().substring(0,f.getName().length()-4));
                    discyml.set(f.getName().substring(0,f.getName().length()-4)+".lore", Arrays.asList("-Midi"));
                }
            }
            try {
                discyml.save(discyamlfile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        FileConfiguration discyml = YamlConfiguration.loadConfiguration(discyamlfile);
        Material defaultDisc = Material.MUSIC_DISC_BLOCKS;
        String displayname = "Midi Disc";
        List<String> lore = new LinkedList<>();

        String songname = args[0].substring(0,args[0].length()-4);

        if(discyml.contains(songname)){
            defaultDisc = Material.matchMaterial(discyml.getString(songname+".material"));
            displayname = discyml.getString(songname+".displayname");
            lore = discyml.getStringList(songname+".lore");
        }

        lore.add(ChatColor.BLACK+"Filename:"+args[0]);

        ItemStack disc = new ItemStack(defaultDisc);
        ItemMeta im = disc.getItemMeta();
        im.setDisplayName(displayname);
        im.setLore(lore);
        disc.setItemMeta(im);
        player.getInventory().addItem(disc);
        sender.sendMessage("Giving Midi Music disc \""+songname+"\".");
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
