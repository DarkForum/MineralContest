package fr.mineral;

import fr.file_manager.FileList;
import fr.file_manager.RessourceFilesManager;
import fr.groups.Core.Groupe;
import fr.groups.GroupeExtension;
import fr.groups.Utils.Etats;
import fr.mineral.Commands.*;
import fr.mineral.Core.Game.Game;
import fr.mineral.Core.Game.JoinTeam.JoinTeamInventoryEvent;
import fr.mineral.Core.Player.BaseItem.Commands.SetDefaultItems;
import fr.mineral.Core.Player.BaseItem.Events.InventoryClick;
import fr.mineral.Settings.GameSettings;
import fr.mapbuilder.MapBuilder;
import fr.mineral.Core.Referee.RefereeEvent;
import fr.mineral.Events.*;
import fr.mineral.Translation.Lang;
import fr.mineral.Utils.Log.GameLogger;
import fr.mineral.Utils.Log.Log;
import fr.mineral.Utils.Metric.SendInformation;
import fr.mineral.Utils.Player.PlayerUtils;
import fr.mineral.Utils.UrlFetcher.Urls;
import fr.mineral.Utils.VersionChecker.Version;
import fr.world_downloader.WorldDownloader;
import jdk.internal.jline.internal.Nullable;
import org.bukkit.*;
import org.bukkit.command.CommandMap;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Logger;

public final class mineralcontest extends JavaPlugin {

    public static boolean debug = false;

    /**
     * Community version is a version ran by server that wants to have multiple games running at the same time.
     * If set to true, then players will have to create a groupe, invite players, start the vote.
     * If set to false, one group will automatically be created, OP players will be set as admin, and every players joining the server
     * will automatically be added to the group.
     */
    public static boolean communityVersion = false;

    public static String prefix = "[MineralContest]";
    public static String prefixErreur;
    public static String prefixGlobal;
    public static String prefixPrive;
    public static String prefixAdmin;
    public static String prefixTeamChat;
    public static String prefixGroupe;
    public static Logger log = Bukkit.getLogger();
    public static mineralcontest plugin;
    public World pluginWorld;

    public Location defaultSpawn;
    public MapBuilder mapBuilderInstance;

    /**
     * Array of all the messages we can fetch from synchroneyes's plugin website.
     */
    private ArrayList<String> messagesFromWebsite;
    public GroupeExtension groupeExtension;
    public LinkedList<Groupe> groupes;
    public WorldDownloader worldDownloader;

    // Constructeur, on initialise les variables
    public mineralcontest() {
        mineralcontest.plugin = this;
    }


    /**
     * Affiche dans le chat les messages récupéré depuis le site web, lié à cette version du plugin
     */
    public static void afficherMessageVersion() {
        // Pour chaque message
        for (String message : plugin.messagesFromWebsite) {
            broadcastMessage(mineralcontest.prefixGlobal + message);
        }
    }

    public static void afficherMessageVersionToPlayer(Player joueur) {
        for (String message : plugin.messagesFromWebsite) {
            joueur.sendMessage(mineralcontest.prefixPrive + message);
        }
    }


    public static Object getPluginConfigValue(String configName) {
        File fichierConfigurationPlugin = new File(plugin.getDataFolder(), FileList.Config_default_plugin.toString());
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(fichierConfigurationPlugin);
        return configuration.get(configName);
    }

    public static void supprimerGroupe(Groupe g) {
        plugin.groupes.remove(g);
    }

    public static Groupe getPlayerGroupe(Player p) {
        mineralcontest instance = plugin;
        for (Groupe groupe : instance.groupes) {
            if (groupe.containsPlayer(p)) return groupe;
        }
        return null;
    }

    public Groupe getNonCommunityGroup() {
        return groupes.getFirst();
    }

    public void creerNouveauGroupe(Groupe nouveauGroupe) {

        if (!communityVersion) return;

        GameLogger.addLog(new Log("group_create", "Creating a new group with name " + nouveauGroupe.getNom(), "mineralcontest: creerNouveauGroupe"));
        for (Groupe groupe : groupes) {
            if (groupe.getNom().equals(nouveauGroupe.getNom())) {
                groupe.sendToadmin(prefixErreur + Lang.error_group_with_this_name_already_exists.toString());
                GameLogger.addLog(new Log("error_group_create", Lang.error_group_with_this_name_already_exists.getDefault(), "mineralcontest: creerNouveauGroupe"));
                return;
            }
        }

        while (!isGroupeIdentifiantUnique(nouveauGroupe)) {
            nouveauGroupe.genererIdentifiant();
        }


        this.groupes.add(nouveauGroupe);
        nouveauGroupe.sendToEveryone(prefixPrive + Lang.success_group_successfully_created.toString());
        GameLogger.addLog(new Log("group_created", "created a new group with name " + nouveauGroupe.getNom() + " and ID: " + nouveauGroupe.getIdentifiant(), "mineralcontest: creerNouveauGroupe"));

    }

    private boolean isGroupeIdentifiantUnique(Groupe groupe) {
        for (Groupe g : groupes)
            if (g.getIdentifiant().equalsIgnoreCase(groupe.getIdentifiant())) return false;
        return true;
    }

    public void setDefaultSpawn(Location defaultSpawn) {
        this.defaultSpawn = defaultSpawn;
    }


    public void initCommunityVersion() {
        if (!communityVersion) {
            Groupe defaut = new Groupe();
            defaut.setEtat(Etats.EN_ATTENTE);
            defaut.setNom("MineralContest");
            groupes.add(defaut);

            if (pluginWorld == null) pluginWorld = PlayerUtils.getPluginWorld();
            if (pluginWorld == null) return;
            for (Player joueur : mineralcontest.plugin.pluginWorld.getPlayers())
                if (joueur.isOp()) getNonCommunityGroup().addAdmin(joueur);
                else getNonCommunityGroup().addJoueur(joueur);
        }
    }

    @Override
    public void onEnable() {

        // On copie les fichiers par défaut
        RessourceFilesManager.createDefaultFiles();


        // On charge le fichier de langue
        Lang.loadLang(getPluginConfigValue("language").toString());


        // Initialisation des variables du plugin
        this.groupeExtension = GroupeExtension.getInstance();
        this.groupes = new LinkedList<>();
        this.mapBuilderInstance = MapBuilder.getInstance();
        this.messagesFromWebsite = new ArrayList<>();
        this.worldDownloader = WorldDownloader.getInstance();


        pluginWorld = PlayerUtils.getPluginWorld();
        defaultSpawn = (pluginWorld != null) ? pluginWorld.getSpawnLocation() : null;
        if (pluginWorld != null) pluginWorld.setDifficulty(Difficulty.PEACEFUL);


        registerCommands();
        registerEvents();


        if (!debug)
            if (pluginWorld != null)
                for (Player online : pluginWorld.getPlayers())
                    PlayerUtils.teleportPlayer(online, defaultSpawn);

        PlayerUtils.runScoreboardManager();
        GameLogger.addLog(new Log("server_event", "OnEnable", "plugin_startup"));

        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerUtils.drawPlayersHUD();
            }
        }.runTaskTimer(this, 0, 20);
        initCommunityVersion();


        /*
            Verification de mise à jour et téléchargement automatique
            Seulement si la config l'autorise
         */
        if((boolean) getPluginConfigValue("enable_auto_update")) {

            // On lance la procédure de vérification de version une fois que le plugin est totalement chargé
            getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {


                Version.isCheckingStarted = true;
                Thread operationsThreade = new Thread(() -> {
                    // On récupère toutes les URL du plugin
                    Urls.FetchAllUrls();
                    worldDownloader.initMapLists();

                    Version.fetchAllMessages(messagesFromWebsite);

                    afficherMessageVersion();

                    Version.Check(true);

                });

                operationsThreade.start();

                // On lance un timer qui vérifie, à chaque seconde, si le téléchargement est terminé
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // La procédure à commencer
                        if(Version.isCheckingStarted) {

                            // Si le plugin a été mis à jour, on reload le plugin
                            if(Version.hasUpdated) {
                                Bukkit.reload();
                            }

                        } else {
                            // On arrête le timer
                            this.cancel();
                        }
                    }
                }.runTaskTimer(this, 20, 20);

            });



        }


    }

    @Override
    public void onDisable() {

        for (Groupe groupe : groupes) {
            Game game = groupe.getGame();
            SendInformation.sendGameData(SendInformation.ended, game);
            // todo GETGAME
            if (pluginWorld != null && !debug) {
                for (Player player : pluginWorld.getPlayers()) {
                    game.teleportToLobby(player);
                    PlayerUtils.clearPlayer(player);
                }
            }
        }


        //getGame().resetMap();
        Bukkit.getScheduler().cancelTasks(this);
        GameLogger.addLog(new Log("server_event", "OnDisable", "plugin_shutdown"));

    }

    private void registerEvents() {

        Bukkit.getServer().getPluginManager().registerEvents(new BlockDestroyed(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new BlockPlaced(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new EntityInteract(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerInteract(), this);


        Bukkit.getServer().getPluginManager().registerEvents(new BucketEvent(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new ChestEvent(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new EntityDamage(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new EntityTarget(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new EntitySpawn(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new EntityDeathEvent(), this);


        Bukkit.getServer().getPluginManager().registerEvents(new ExplosionEvent(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerDisconnect(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerJoin(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerMove(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerSpawn(), this);

        Bukkit.getServer().getPluginManager().registerEvents(new SafeZoneEvent(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerChat(), this);


        Bukkit.getServer().getPluginManager().registerEvents(new RefereeEvent(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerPick(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerWorldChange(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new WorldLoaded(), this);



        // PlayerBaseItem
        Bukkit.getServer().getPluginManager().registerEvents(new InventoryClick(), this);

        // JoinMenu
        Bukkit.getServer().getPluginManager().registerEvents(new JoinTeamInventoryEvent(), this);



    }

    private void registerCommands() {
        // Register les commands
        getCommand("start").setExecutor(new StartGameCommand());
        getCommand("pause").setExecutor(new PauseGameCommand());
        getCommand("stopGame").setExecutor(new StopGameCommand());
        getCommand("arene").setExecutor(new AreneTeleportCommand());
        getCommand("arena").setExecutor(new AreneTeleportCommand());
        getCommand("join").setExecutor(new JoinCommand());
        getCommand("referee").setExecutor(new RefereeCommand());
        getCommand("arbitre").setExecutor(new RefereeCommand());
        getCommand("ready").setExecutor(new ReadyCommand());
        getCommand("t").setExecutor(new TeamChat());
        getCommand("team").setExecutor(new TeamChat());



        getCommand("switch").setExecutor(new SwitchCommand());
        getCommand("resume").setExecutor(new ResumeGameCommand());

        Field cmdMapField = null;
        try {
            cmdMapField = SimplePluginManager.class.getDeclaredField("commandMap");
            cmdMapField.setAccessible(true);
            CommandMap bukkitCommandMap = (CommandMap) cmdMapField.get(Bukkit.getPluginManager());

            bukkitCommandMap.register("", new MCCvarCommand());
            bukkitCommandMap.register("", new SetDefaultItems());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }



        getCommand("spawnchest").setExecutor(new SpawnChestCommand());
        getCommand("allow").setExecutor(new AllowCommand());
        getCommand("leaveteam").setExecutor(new LeaveTeamCommand());

        /*getCommand("setup").setExecutor(new SetupCommand());
        getCommand("saveworld").setExecutor(new SaveWorldCommand());
        getCommand("valider").setExecutor(new ValiderCommand());*/

    }

    // Called when the game start
    public void setWorldBorder() throws Exception {
        if(pluginWorld == null) return;
        int playZoneRadius = GameSettings.getValeurParDefaut("mp_set_playzone_radius").getValeurNumerique();
        int marge = 100; // Use to prevent server.properties spawn protection

        WorldBorder world = pluginWorld.getWorldBorder();

        // TODO getGame
        //Location arenaCenterLocation = getGame().getArene().getCoffre().getPosition().clone();

        //arenaCenterLocation.setX(arenaCenterLocation.getX()-marge);
        //world.setCenter(arenaCenterLocation);
        //world.setSize(playZoneRadius + marge);
    }

    public void setDefaultWorldBorder() {

        if(mapBuilderInstance.isBuilderModeEnabled) return;

        World game_world = mineralcontest.plugin.pluginWorld;
        int size = 30000000;

        if(game_world != null) {
            game_world.getWorldBorder().setCenter(mineralcontest.plugin.defaultSpawn);
            game_world.getWorldBorder().setSize(size);
        }
    }


    public static void checkIfMapIsCorrect() {
        if(mineralcontest.plugin.pluginWorld == null) {
            mineralcontest.plugin.pluginWorld = PlayerUtils.getPluginWorld();
        }
        if(mineralcontest.debug) return;
        if (mineralcontest.plugin.pluginWorld == null) {
            ConsoleCommandSender console = mineralcontest.plugin.getServer().getConsoleSender();
            log.severe("NULL");
            log.severe(mineralcontest.prefixErreur + Lang.bad_map_loaded.toString());
            log.severe(mineralcontest.prefixErreur + Lang.github_link.toString());
            log.severe(mineralcontest.prefixErreur + Lang.plugin_shutdown.toString());
            Bukkit.getPluginManager().disablePlugin(mineralcontest.plugin);

        }
    }

    public static void broadcastMessage(String message, Groupe groupe) {
        groupe.sendToEveryone(message);
        Bukkit.getConsoleSender().sendMessage(message);
        GameLogger.addLog(new Log("broadcast-group", message, "server"));
    }

    public static void broadcastMessage(String message) {
        for (Player joueur : Bukkit.getOnlinePlayers())
            if (isInAMineralContestWorld(joueur))
                joueur.sendMessage(message);

        Bukkit.getConsoleSender().sendMessage(message);
        GameLogger.addLog(new Log("broadcast-plugin", message, "server"));
    }

    public static void broadcastMessageToAdmins(String message) {
        for(Player player : mineralcontest.plugin.pluginWorld.getPlayers())
            if(player.isOp()) player.sendMessage(message);
        Bukkit.getLogger().info(message);
        GameLogger.addLog(new Log("broadcast_admin", message, "server"));
    }


    public static boolean isInAMineralContestWorld(Player p) {
        for (Groupe groupe : plugin.groupes)
            if (groupe.getMonde() != null && p.getWorld().equals(groupe.getMonde())) return true;

        return p.getWorld().equals(mineralcontest.plugin.pluginWorld);
    }

    public static boolean isInMineralContestHub(Player p) {
        return p.getWorld().equals(plugin.pluginWorld);
    }

    public static boolean isAMineralContestWorld(World w) {
        for (Groupe groupe : plugin.groupes)
            if (groupe.getMonde() == null && communityVersion) return false;
            else if (w.equals(groupe.getMonde())) return true;
        return w.equals(plugin.pluginWorld);
    }

    @Nullable
    public static Game getPlayerGame(Player p) {
        Groupe g = getPlayerGroupe(p);
        if (g != null) return g.getGame();
        return null;
    }

    @Nullable
    public static Game getWorldGame(World world) {
        for (Groupe groupe : plugin.groupes)
            if (groupe.getMonde() == null) return null;
            else if (groupe.getMonde().equals(world))
                return groupe.getGame();
        return null;
    }



}
