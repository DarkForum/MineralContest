package fr.mineral.Core.Arena.Zones;

import fr.mineral.Core.House;
import fr.mineral.Settings.GameSettingsCvar;
import fr.mineral.Teams.Equipe;
import fr.mineral.Translation.Lang;
import fr.mineral.Utils.ErrorReporting.Error;
import fr.mineral.Utils.Player.CouplePlayer;
import fr.mineral.Utils.Player.PlayerBaseItem;
import fr.mineral.Utils.Player.PlayerUtils;
import fr.mineral.mineralcontest;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedList;

/*
    Classe représentant la deathzone
 */
public class DeathZone {
    /*
        Un "CouplePlayer" est une classe ayant les attributs suivants:
            - Player joueur
            - int valeur
     */
    LinkedList<CouplePlayer> joueurs;

    // Temps en seconde
    private int timeInDeathzone = 0;
    private Location spawnLocation;

    public DeathZone() {
        this.joueurs = new LinkedList<CouplePlayer>();
        timeInDeathzone = (int) GameSettingsCvar.getValueFromCVARName("death_time");
    }
    public LinkedList<CouplePlayer> getPlayers() { return this.joueurs; }

    public void setSpawnLocation(Location pos) {
        mineralcontest.plugin.getLogger().info(mineralcontest.prefixGlobal + Lang.translate(Lang.deathzone_spawn_location_added.toString()));
        this.spawnLocation = pos;
    }

    public Location getSpawnLocation() throws Exception {
        if(spawnLocation == null) {
            throw new Exception(Lang.translate(Lang.deathzone_spawn_location_undefined.toString()));
        }

        return this.spawnLocation;
    }

    // Cette fonction réduit le temps des joueurs d'une seconde
    // Elle sera appelée dans le runnable bukkit qui gère le temps
    public void reducePlayerTimer() throws Exception {

        // SI on a des joueurs dans la deathZone
        if(joueurs.size() != 0) {
            for(CouplePlayer joueur : this.joueurs) {

                // Si le joueur a fini sa peine
                if(joueur.getValeur() <= 0)
                    libererJoueur(joueur);

                // ON réduit son temps de 1

                if(joueur.getValeur() >= 1) joueur.getJoueur().sendTitle(ChatColor.RED + Lang.translate(Lang.deathzone_you_are_dead.toString()), Lang.translate(Lang.deathzone_respawn_in.toString(), joueur.getJoueur()), 0, 20, 0);
                joueur.setValeur(joueur.getValeur()-1);
                joueur.getJoueur().setFireTicks(0);

            }
        }
    }

    public int getPlayerDeathTime(Player joueur) {
        if(isPlayerDead(joueur))
            for(CouplePlayer cp : getPlayers())
                if(cp.getJoueur().equals(joueur))
                    return cp.getValeur();

        return 0;
    }

    public void add(Player joueur) throws Exception {
        timeInDeathzone = (int) GameSettingsCvar.getValueFromCVARName("death_time");

        if(mineralcontest.plugin.getGame().isReferee(joueur) && mineralcontest.plugin.getGame().isGameStarted()) {
            joueur.setGameMode(GameMode.SURVIVAL);
            joueur.setFireTicks(0);
            joueur.setHealth(20f);
            PlayerUtils.teleportPlayer(joueur, mineralcontest.plugin.getGame().getArene().getCoffre().getPosition());
            return;
        }

        this.joueurs.add(new CouplePlayer(joueur, timeInDeathzone));
        joueur.setGameMode(GameMode.ADVENTURE);
        joueur.getInventory().clear();
        joueur.sendMessage(mineralcontest.prefixPrive + Lang.translate(Lang.deathzone_respawn_in.toString(), joueur));
        //PlayerUtils.teleportPlayer(this.spawnLocation);
        try {
            PlayerUtils.teleportPlayer(joueur, mineralcontest.plugin.getGame().getPlayerHouse(joueur).getHouseLocation());

        }catch(Exception e) {
            e.printStackTrace();
            Error.Report(e);
        }

        joueur.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20*(timeInDeathzone*3), 1));
        joueur.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20*(timeInDeathzone*3), 1));

    }

    public boolean isPlayerDead(Player joueur) {
        for(CouplePlayer cp : getPlayers()) {
            if(cp.getJoueur().equals(joueur))
                return true;
        }

        return false;
    }

    private void libererJoueur(CouplePlayer DeathZonePlayer) throws Exception {

        // SI le joueur n'a plus de temps à passer ici
        if(DeathZonePlayer.getValeur() <= 0) {
            Player joueur = DeathZonePlayer.getJoueur();
            joueur.setGameMode(GameMode.SURVIVAL);
            joueur.setFireTicks(0);
            joueur.setHealth(20f);

            Equipe team = mineralcontest.plugin.getGame().getPlayerTeam(joueur);
            House teamHouse = mineralcontest.plugin.getGame().getPlayerHouse(joueur);
            if(team == null) {
                // On le téléporte vers l'arene
                // On le téléporte vers l'arene
                mineralcontest.broadcastMessage(mineralcontest.prefixGlobal + "Le joueur " + joueur.getDisplayName() + " a été TP au centre de l'arène car il n'a pas d'équipe et vient de réapparaitre suite à une mort");
                mineralcontest.broadcastMessage(mineralcontest.prefixGlobal + "Le joueur " + joueur.getDisplayName() + " a également été mis spectateur. Vous devez changer son gamemode");
                mineralcontest.plugin.getGame().teleportToLobby(joueur);
                joueur.setGameMode(GameMode.SPECTATOR);

            } else {
                // ON le TP vers son spawn equipe
                PlayerUtils.teleportPlayer(joueur, teamHouse.getHouseLocation());
                joueur.removePotionEffect(PotionEffectType.INVISIBILITY);
                joueur.removePotionEffect(PotionEffectType.BLINDNESS);


            }

            // On rend le stuff du joueur
            //PlayerUtils.givePlayerBaseItems(joueur);
            try {
                PlayerBaseItem.givePlayerItems(joueur, PlayerBaseItem.everyRespawnName);
            }catch (Exception e) {
                mineralcontest.broadcastMessage(mineralcontest.prefixErreur + e.getMessage());
                e.printStackTrace();
                Error.Report(e);
            }
            DeathZonePlayer.getJoueur().sendTitle(ChatColor.GREEN + Lang.translate(Lang.deathzone_respawned.toString()), "", 1, 2*20, 1);

            // ON le supprime de la liste
            this.joueurs.remove(DeathZonePlayer);



        }
    }
}
