package fr.mineral.Events;

import fr.mineral.Scoreboard.ScoreboardUtil;
import fr.mineral.mineralcontest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class PlayerWorldChange implements Listener {
    @EventHandler
    public void onPlayerchangeWorld(PlayerChangedWorldEvent event) {


        if (mineralcontest.isAMineralContestWorld(event.getFrom())) {
            ScoreboardUtil.unrankedSidebarDisplay(event.getPlayer(), "");
        }

        // miltivers support kick

        /*if(event.getPlayer().getWorld().equals(mineralcontest.plugin.pluginWorld) && mineralcontest.getPlayerGame(joueur).isGameStarted()) {
            World oldWorld = event.getPlayer().getWorld();
            event.getPlayer().teleport(new Location(event.getFrom(), 0, 70, 0));
        }*/
    }
}
