package fr.mineral.Commands.CVAR;

import fr.mineral.Core.Game.Game;
import fr.mineral.Settings.GameSettingsCvarOLD;
import fr.mineral.Translation.Lang;
import fr.mineral.mineralcontest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class mp_randomize_team implements CommandExecutor {
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

            if (partie.isGameStarted()) {
                sender.sendMessage(mineralcontest.prefixErreur + "La partie est déjà en cours, la modification de valeur n'est pas permis.");
                return true;
            }

            // début mp_randomizer_team
            if(command.getName().equalsIgnoreCase("mp_randomize_team")) {
                if(args.length == 1) {
                    if(args[0].equalsIgnoreCase("0") || args[0].equalsIgnoreCase("1")) {
                    /*mineralcontest.mp_randomize_team = Integer.parseInt(args[0]);
                    try {
                        mineralcontest.plugin.setConfigValue("config.cvar.mp_randomize_team", mineralcontest.mp_randomize_team);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/

                        GameSettingsCvarOLD.mp_randomize_team.setValue(args[0]);

                        switch(Integer.parseInt(args[0])) {
                            case 1:
                                mineralcontest.broadcastMessage(mineralcontest.prefixGlobal + "Les équipes seront désormais aléatoire.", partie.groupe);
                                break;

                            case 0:
                                mineralcontest.broadcastMessage(mineralcontest.prefixGlobal + "Les équipes seront désormais de manière manuelle.", partie.groupe);
                                mineralcontest.broadcastMessage(mineralcontest.prefixGlobal + "Veuillez utiliser /join <rouge | bleu | jaune>", partie.groupe);
                                break;
                        }

                        return false;
                    }else{
                        sender.sendMessage("Usage: /mp_randomize_team [1 | 0]");
                        return true;
                    }
                } else if(args.length == 0) {
                    sender.sendMessage("[mp_randomize_team] Valeur actuelle: " + GameSettingsCvarOLD.mp_randomize_team.getValue());
                    return true;
                }else {
                    sender.sendMessage("Usage: /mp_randomize_team [1 | 0]");
                    return true;
                }
            }
        }



        // FIN mp_randomize_team

        return false;
    }
}
