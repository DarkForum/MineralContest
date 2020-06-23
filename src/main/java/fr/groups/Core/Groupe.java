/**
 * TODO:
 * - Votemap
 * - Démarrer partie
 * - Creer teams (max3)
 */
package fr.groups.Core;

import fr.groups.Utils.Etats;
import fr.mineral.Core.Game.Game;
import fr.mineral.Core.House;
import fr.mineral.Core.Player.BaseItem.PlayerBaseItem;
import fr.mineral.Core.Referee.Referee;
import fr.mineral.Settings.GameSettings;
import fr.mineral.Teams.Equipe;
import fr.mineral.Translation.Lang;
import fr.mineral.mineralcontest;
import javafx.util.Pair;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.*;

public class Groupe {
    private int tailleIdentifiant = 10;
    private String identifiant;
    private LinkedList<Equipe> equipes;
    private LinkedList<Player> admins;
    private LinkedList<Player> joueurs;
    private LinkedList<Player> joueursInvites;
    private World gameWorld;
    private MapVote mapVote;

    private Game partie;
    private String nom;
    private WorldLoader worldLoader;

    private Etats etat;

    private boolean groupLocked = false;
    private String mapName = "";

    private GameSettings parametresPartie;

    private PlayerBaseItem playerBaseItem;

    private HashMap<UUID, Pair<Equipe, Location>> disconnectedPlayers;


    public Groupe() {
        this.equipes = new LinkedList<>();
        this.admins = new LinkedList<>();
        this.joueurs = new LinkedList<>();
        this.joueursInvites = new LinkedList<>();

        this.disconnectedPlayers = new HashMap<>();

        parametresPartie = new GameSettings(true);

        this.partie = new Game(this);
        this.partie.init();
        partie.setGroupe(this);

        this.etat = Etats.EN_ATTENTE;
        this.worldLoader = new WorldLoader(this);
        genererIdentifiant();

        this.playerBaseItem = new PlayerBaseItem(this);
    }

    /**
     * Méthode permettant de remettre à 0 une partie
     * Cette méthode est appelée à la fin d'une partie
     */
    public void resetGame() {
        this.partie = new Game(this);
        this.partie.init();
    }

    /**
     * Retire tous les items au sol
     * Source: https://bukkit.org/threads/remove-dropped-items-on-ground.100750/
     */
    public void removeAllDroppedItem() {
        List<Entity> entList = gameWorld.getEntities();//get all entities in the world

        for (Entity current : entList) {//loop through the list
            if (current instanceof Item) {//make sure we aren't deleting mobs/players
                current.remove();//remove it
            }
        }
    }

    public PlayerBaseItem getPlayerBaseItem() {
        return playerBaseItem;
    }

    public GameSettings getParametresPartie() {
        return parametresPartie;
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public LinkedList<Player> getPlayers() {
        return joueurs;
    }

    public boolean isPlayerIngroupe(Player p) {
        return (this.joueurs.contains(p));
    }

    public String getIdentifiant() {
        return identifiant;
    }

    public World getMonde() {
        return gameWorld;
    }

    public House getPlayerHouse(Player p) {
        for (House maison : getGame().getHouses())
            if (maison.getTeam().isPlayerInTeam(p)) return maison;
        return null;

    }

    public Equipe getPlayerTeam(Player p) {
        for (House maison : getGame().getHouses())
            if (maison.getTeam().isPlayerInTeam(p)) return maison.getTeam();
        return null;

    }

    public void genererIdentifiant() {
        String[] alphabet = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        StringBuilder id_generer = new StringBuilder();
        Random random = new Random();
        int numero_aleatoire = 0;
        for (int i = 0; i < tailleIdentifiant; ++i) {
            numero_aleatoire = random.nextInt(alphabet.length);
            id_generer.append(alphabet[numero_aleatoire]);
        }

        this.identifiant = id_generer.toString();

    }

    /**
     * @param nomMonde - Nom du monde à charger
     * @return true si chargement réussi, false sinon
     */
    public boolean chargerMonde(String nomMonde) {

        try {

            sendToEveryone(mineralcontest.prefixGroupe + "Chargement de la map \"" + nomMonde + "\" en cours ...");
            this.gameWorld = worldLoader.chargerMonde(nomMonde, getIdentifiant());
            this.gameWorld.setAutoSave(false);
        } catch (Exception e) {
            sendToadmin(mineralcontest.prefixErreur + " Impossible de charger le monde. Erreur: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        if (gameWorld == null) {
            sendToadmin(mineralcontest.prefixErreur + " Impossible de charger le monde.");
            return false;
        }


        Location worldSpawnLocation = gameWorld.getSpawnLocation();

        try {
            if (worldSpawnLocation.getX() == WorldLoader.defaultX && worldSpawnLocation.getY() == WorldLoader.defaultY && worldSpawnLocation.getZ() == WorldLoader.defaultZ)
                worldSpawnLocation = partie.getArene().getCoffre().getPosition();
        } catch (Exception e) {
            worldSpawnLocation = gameWorld.getSpawnLocation();
        }



        for (Player joueur : joueurs) {
            joueur.getInventory().clear();

            // Si le joueur est un arbitre, on lui donne le livre
            if (getGame().isReferee(joueur)) joueur.getInventory().setItemInMainHand(Referee.getRefereeItem());

                // Sinon, on lui donne le livre de selection d'équipe!
            else joueur.getInventory().setItemInMainHand(Game.getTeamSelectionItem());
            joueur.teleport(worldSpawnLocation);
            joueur.sendMessage(mineralcontest.prefixPrive + Lang.set_yourself_as_ready_to_start_game.toString());
        }

        setMapName(nomMonde);


        if (this.mapVote != null) this.mapVote.clearVotes();
        return true;
    }

    /**
     * Décharge un monde
     *
     * @return
     */
    public boolean dechargerMonde() {
        if (gameWorld == null) return false;

        for (Player joueur : joueurs) {
            joueur.teleport(mineralcontest.plugin.defaultSpawn);
        }


        mineralcontest.plugin.getServer().unloadWorld(gameWorld, false);

        worldLoader.supprimerMonde(gameWorld);
        return true;
    }

    public boolean isGroupLocked() {
        return groupLocked;
    }

    public void setGroupLocked(boolean groupLocked) {
        sendToadmin(mineralcontest.prefixPrive + ((groupLocked) ? Lang.group_is_now_locked.toString() : Lang.group_is_now_unlocked.toString()));
        this.groupLocked = groupLocked;
    }

    public MapVote getMapVote() {
        return this.mapVote;
    }

    public void initVoteMap() {
        this.mapVote = new MapVote();

        if(mapVote.getMaps().isEmpty()) {
            mapVote.disableVote();
            setEtat(Etats.EN_ATTENTE);
            setGroupLocked(false);
            sendToadmin(mineralcontest.prefixErreur + Lang.error_no_maps_downloaded_to_start_game.toString());
            sendToadmin(mineralcontest.prefixErreur + Lang.error_no_maps_downloaded_to_start_game.toString());
        } else {
            setEtat(Etats.VOTE_EN_COURS);
            setGroupLocked(true);
            sendToEveryone(mineralcontest.prefixGroupe + Lang.vote_started.toString());
        }
    }

    public void enableVote() {
        if (this.mapVote != null) mapVote.voteEnabled = true;
    }

    public Etats getEtatPartie() {
        return this.etat;
    }

    public void setEtat(Etats etat) {
        this.etat = etat;
    }

    public boolean isPlayerInvited(Player p) {
        return this.joueursInvites.contains(p);
    }

    public void removeAdmin(Player joueur) {
        sendToadmin(mineralcontest.prefixPrive + Lang.translate(Lang.player_is_no_longer_a_group_admin.toString(), joueur));
        this.admins.remove(joueur);

    }


    public void inviterJoueur(Player p) {
        if (joueursInvites.contains(p)) {
            sendToadmin("ERREUR DEJA INVITE");
            return;
        }

        if (joueurs.contains(p)) {
            sendToadmin(mineralcontest.prefixErreur + Lang.translate(Lang.error_player_already_in_this_group.toString(), p));
            return;
        }

        if (mineralcontest.getPlayerGroupe(p) != null) {
            sendToadmin(mineralcontest.prefixErreur + Lang.translate(Lang.error_player_already_have_a_group.toString(), p));
            return;
        }

        p.sendMessage(mineralcontest.prefixPrive + Lang.translate(Lang.you_got_invited_to_a_group.toString(), this));
        sendToadmin(mineralcontest.prefixPrive + Lang.translate(Lang.player_successfully_invited_to_group.toString(), p));
        this.joueursInvites.add(p);
    }

    public Game getGame() {
        return partie;
    }

    public String getNom() {
        return this.nom;
    }

    public String setNom(String nom) {
        this.nom = nom;
        return this.nom;
    }

    public boolean containsPlayer(Player p) {
        return joueurs.contains(p);
    }

    public boolean isGroupeCreateur(Player p) {
        return this.admins.getFirst().equals(p);
    }

    public boolean isAdmin(Player p) {
        return this.admins.contains(p);
    }

    public void kickPlayer(Player p) {
        this.joueurs.remove(p);
        this.admins.remove(p);
        this.joueursInvites.remove(p);
        p.sendMessage(mineralcontest.prefixPrive + Lang.translate(Lang.you_were_kicked_from_a_group.toString(), this));
        sendToEveryone(mineralcontest.prefixPrive + Lang.translate(Lang.player_got_kicked_from_group.toString(), p));
    }

    /**
     * Envoie un message aux admins
     * @param message: Message à envoyer
     */
    public void sendToadmin(String message) {
        for (Player player : admins)
            player.sendMessage(message);
    }

    public void sendToEveryone(String message) {
        for (Player p : joueurs) {
            p.sendMessage(message);
        }
    }

    /**
     * Permet de créer une équipe
     * @param nom - Nom de l'équipe
     * @param couleur - Couleur de l'équipe
     */
    public void addEquipe(String nom, ChatColor couleur) {

    }

    /**
     * Permet de supprimer une équipe
     * @param nom
     */
    public void removeEquipe(String nom) {

    }


    public void addJoueur(Player p) {
        if (this.joueurs.contains(p)) return;

        this.joueursInvites.remove(p);
        this.joueurs.add(p);
        p.sendMessage(mineralcontest.prefixPrive + Lang.translate(Lang.successfully_joined_a_group.toString(), this));
        sendToEveryone(mineralcontest.prefixGroupe + Lang.translate(Lang.player_joined_our_group.toString(), p));

        for(Player joueur : joueurs) {
            if(!partie.isPlayerReady(joueur))
                joueur.sendMessage(mineralcontest.prefixPrive + Lang.set_yourself_as_ready_to_start_votemap.toString());
        }
    }

    public void addAdmin(Player p) {
        if (!this.joueurs.contains(p)) addJoueur(p);
        if (!this.admins.contains(p)) this.admins.add(p);
        sendToadmin(mineralcontest.prefixPrive + Lang.translate(Lang.player_is_now_group_admin.toString(), p));
    }

    public int getPlayerCount() {
        return this.joueurs.size();
    }

    public void retirerJoueur(Player joueur) {
        if (isGroupeCreateur(joueur) && mineralcontest.communityVersion) {

            sendToEveryone(mineralcontest.prefixPrive + Lang.group_got_deleted.toString());
            this.joueurs.clear();
            this.admins.clear();
            this.joueursInvites.clear();
            mineralcontest.supprimerGroupe(this);
            return;
        }

        this.joueurs.remove(joueur);
        this.admins.remove(joueur);
        this.joueursInvites.remove(joueur);
        joueur.sendMessage(mineralcontest.prefixPrive + Lang.translate(Lang.you_left_the_group.toString(), this));
    }


    public LinkedList<Player> getAdmins() {
        return admins;
    }

    /**
     * Sauvegarde les membres du groupe ayant été déconnecté, avec leur position
     *
     * @param p
     */
    public void addDisconnectedPlayer(Player p) {
        Pair<Equipe, Location> playerInfo = new Pair<>(getPlayerTeam(p), p.getLocation());
        retirerJoueur(p);
        if (!havePlayerDisconnected(p)) disconnectedPlayers.put(p.getUniqueId(), playerInfo);
    }

    /**
     * Reconnecte un joueur, on le re TP dans sa position initiale, et dans son équipe
     *
     * @param p
     */
    public void playerHaveReconnected(Player p) {
        if (havePlayerDisconnected(p)) {
            Pair<Equipe, Location> playerInfo = disconnectedPlayers.get(p.getUniqueId());

            Equipe playerTeam = playerInfo.getKey();
            Location playerLocation = playerInfo.getValue();
            p.setFlying(false);
            // Si la team n'est pas nulle, on le remet dans son équipe
            if (playerTeam != null) {
                try {
                    playerTeam.addPlayerToTeam(p, true, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (p.isOp()) {
                    partie.addReferee(p);
                    return;
                }
            }

            // On téléport le joueur à sa dernière position
            p.teleport(playerLocation);

            // On le supprime de la liste des déco
            disconnectedPlayers.remove(p.getUniqueId());

            // SI il n'y a plus personne dans la liste, on relance la partie!
            if (disconnectedPlayers.isEmpty()) partie.resumeGame();
        }
    }

    /**
     * Retourne VRAI si le joueur s'était déjà déconnecté
     *
     * @param p
     */
    public boolean havePlayerDisconnected(Player p) {
        return disconnectedPlayers.containsKey(p.getUniqueId());
    }


}

