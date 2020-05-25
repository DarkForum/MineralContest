/**
 * TODO: - Charger les differents spawns de la map depuis le dossier du monde ;)
 */
package fr.groups;

import fr.groups.Commands.Admin.AjouterAdmin;
import fr.groups.Commands.Admin.RetirerAdmin;
import fr.groups.Commands.Groupe.*;
import fr.groups.Commands.Vote.StartVote;
import fr.groups.Commands.Vote.Vote;
import fr.mineral.Utils.ErrorReporting.Error;
import fr.mineral.Utils.Player.PlayerUtils;
import fr.mineral.mineralcontest;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.SimplePluginManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

public class GroupeExtension {

    private static GroupeExtension instance;
    private CommandMap bukkitCommandMap;
    public static boolean enabled = true;

    private GroupeExtension() {
        if (!enabled) return;
        instance = this;
        Bukkit.getLogger().info("Loading GroupeExtension ...");
        try {
            getPluginCommandMap();
        } catch (Exception e) {
            e.printStackTrace();
            Error.Report(e);
        }

        registerCommands();
        supprimerMapsExistantes();
    }

    /**
     * Supprime toute les maps crées auparavant
     */
    private void supprimerMapsExistantes() {
        File dossierServer = new File(System.getProperty("user.dir"));
        File[] fichiers = dossierServer.listFiles((dir, name) -> name.toLowerCase().startsWith("mc_"));

        for (File fichier : fichiers) {
            try {
                Bukkit.getServer().unloadWorld(fichier.getName(), false);
                FileUtils.deleteDirectory(fichier);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Error.Report(ioe);
            }
        }

        for (World world : Bukkit.getWorlds()) {
            if (world.getName().contains("mc_")) {
                world.setAutoSave(false);
                for (Player p : world.getPlayers()) {
                    if (mineralcontest.plugin.defaultSpawn == null) {
                        p.setHealth(0);
                    } else {
                        p.teleport(mineralcontest.plugin.defaultSpawn);
                    }
                }

                Bukkit.unloadWorld(world, false);
                Bukkit.getLogger().info("Successfully unloaded world " + world.getName());

            }
        }



    }

    public static GroupeExtension getInstance() {
        if (instance == null) return new GroupeExtension();
        return instance;
    }

    private void registerCommands() {
        if (!enabled) return;
        this.bukkitCommandMap.register("", new CreerGroupe());
        this.bukkitCommandMap.register("", new StartVote());
        this.bukkitCommandMap.register("", new InviterGroupe());
        this.bukkitCommandMap.register("", new JoinGroupe());
        this.bukkitCommandMap.register("", new KickPlayerFromGroup());
        this.bukkitCommandMap.register("", new QuitterGroupe());
        this.bukkitCommandMap.register("", new AjouterAdmin());
        this.bukkitCommandMap.register("", new RetirerAdmin());

        this.bukkitCommandMap.register("", new Vote());


    }

    private void getPluginCommandMap() throws NoSuchFieldException, IllegalAccessException {
        Field cmdMapField = SimplePluginManager.class.getDeclaredField("commandMap");
        cmdMapField.setAccessible(true);
        this.bukkitCommandMap = (CommandMap) cmdMapField.get(Bukkit.getPluginManager());
    }
}
