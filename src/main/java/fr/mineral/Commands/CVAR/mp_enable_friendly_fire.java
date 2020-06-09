package fr.mineral.Commands.CVAR;

import fr.mineral.Core.Game.Game;
import fr.mineral.Settings.GameSettingsCvarOLD;
import fr.mineral.Translation.Lang;
import fr.mineral.mineralcontest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class mp_enable_friendly_fire implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(Lang.error_command_can_only_be_used_in_game.toString());
            return false;
        }

        Player player = (Player) sender;
        if (mineralcontest.isInAMineralContestWorld(player)) {
            Game partie = mineralcontest.getPlayerGame(player);
            if (partie == null) {
                sender.sendMessage(Lang.error_command_can_only_be_used_in_game.toString());
                return false;
            }

            if(command.getName().equalsIgnoreCase("mp_enable_friendly_fire")) {
                if(sender.isOp()) {
                    if(args.length == 1) {
                        int valeur = 1;
                        try {
                            valeur = Integer.parseInt(args[0]) % 2;
                            GameSettingsCvarOLD.mp_enable_friendly_fire.setValue(valeur);
                            if (valeur == 1)
                                mineralcontest.broadcastMessage(mineralcontest.prefixPrive + Lang.cvar_friendly_fire_enabled.toString(), partie.groupe);
                            else
                                mineralcontest.broadcastMessage(mineralcontest.prefixPrive + Lang.cvar_friendly_fire_disabled.toString(), partie.groupe);

                            return false;
                        }catch (NumberFormatException nfe){
                            nfe.printStackTrace();
                            sender.sendMessage("Usage: /mp_enable_friendly_fire <0=DISABLED, 1=ENABLED>");
                            return false;
                        }

                    } else {
                        sender.sendMessage("Usage: /mp_enable_friendly_fire <0=DISABLED, 1=ENABLED>");
                        return false;
                    }

                }
            }

            return false;
        }

        return false;
    }
}
