package fr.mineral.Events;

import fr.mapbuilder.MapBuilder;
import fr.mineral.Core.Game.BlockManager;
import fr.mineral.Core.Game.Game;
import fr.mineral.Settings.GameSettings;
import fr.mineral.Translation.Lang;
import fr.mineral.Utils.BlockSaver;
import fr.mineral.Utils.ErrorReporting.Error;
import fr.mineral.Utils.Radius;
import fr.mineral.mineralcontest;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaced implements Listener {
    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent event) {

        if(MapBuilder.getInstance().isBuilderModeEnabled) return;

        World worldEvent = event.getPlayer().getWorld();
        Game game = mineralcontest.getPlayerGame(event.getPlayer());

        if (MapBuilder.getInstance().isBuilderModeEnabled) return;

        if (mineralcontest.isAMineralContestWorld(worldEvent)) {
            // SI on est dans le HUB, on ne peut pas poser de bloc
            if (worldEvent.equals(mineralcontest.plugin.pluginWorld)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(mineralcontest.prefixErreur + Lang.cant_interact_block_hub.toString());
                return;
            }

            // Si on est pas dans une game, on arrête là, ne devrait pas arriver
            if (game == null) {
                Bukkit.getLogger().severe("[MineralContest] Block got placed, but not inside a game ...");
                return;
            }

            if (game.isGameStarted() && !game.isGamePaused()) {
                try {

                    GameSettings settings = game.groupe.getParametresPartie();
                    if (Radius.isBlockInRadius(event.getBlock().getLocation(), game.getArene().getCoffre().getPosition(), settings.getCVAR("protected_zone_area_radius").getValeurNumerique())) {
                        // We are in the radius of the arena
                        if (settings.getCVAR("mp_enable_block_adding").getValeurNumerique() == 1) {
                            BlockManager blockManager = BlockManager.getInstance();

                            if (!blockManager.isBlockAllowedToBeAdded(event.getBlock())) {
                                event.setCancelled(true);
                                event.getPlayer().sendMessage(mineralcontest.prefixErreur + Lang.block_not_allowed_to_be_placed.toString());
                            }
                            blockManager.addBlock(event.getBlock());
                        } else {
                            // We block the block adding
                            event.setCancelled(true);
                            event.getPlayer().sendMessage(mineralcontest.prefixErreur + Lang.cant_interact_block_hub.toString());
                        }
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Error.Report(e, game);
                }
            } else {
                // We block the block adding
                event.setCancelled(true);
                event.getPlayer().sendMessage(mineralcontest.prefixErreur + Lang.cant_interact_block_hub.toString());
            }

            game.addAChest(event.getBlock());
            // Save the block
            game.addBlock(event.getBlock(), BlockSaver.Type.PLACED);
        }

    }

}

