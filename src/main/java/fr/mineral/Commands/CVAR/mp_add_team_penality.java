package fr.mineral.Commands.CVAR;

import fr.mineral.Translation.Lang;
import fr.mineral.mineralcontest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class mp_add_team_penality implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (sender.isOp() && command.getName().equals("mp_add_team_penality")) {
            String usage = "Usage: /mp_add_team_penality <amount> <" + Lang.red_team.toString() + " | " + Lang.yellow_team.toString() + " | " + Lang.blue_team.toString() + ">";
            if (args.length == 2) {

                if (args.length == 2) {

                    int valeur = 0;
                    try {
                        valeur = Integer.parseInt(args[1]);
                    }catch(Exception e) {
                        sender.sendMessage(usage);
                        return true;
                    }

                    switch (args[0].toLowerCase()) {
                        case "j":
                        case "jaune":
                        case "y":
                        case "yellow":
                            try {
                                mineralcontest.plugin.getGame().getTeamJaune().addPenalty(valeur);

                            } catch (Exception e) {
                                sender.sendMessage(mineralcontest.prefixErreur + e.getMessage());
                            }
                            break;

                        case "b":
                        case "bleu":
                        case "bleue":
                        case "blue":
                            try {
                                mineralcontest.plugin.getGame().getTeamBleu().addPenalty(valeur);

                            } catch (Exception e) {
                                sender.sendMessage(mineralcontest.prefixErreur + e.getMessage());
                            }
                            break;

                        case "r":
                        case "rouge":
                        case "red":
                            try {
                                mineralcontest.plugin.getGame().getTeamRouge().addPenalty(valeur);

                            } catch (Exception e) {
                                sender.sendMessage(mineralcontest.prefixErreur + e.getMessage());
                            }
                            break;

                        default:
                            sender.sendMessage(usage);
                            return true;
                    }
                } else {
                    sender.sendMessage(usage);
                }
            }
            return false;
        }
        return true;
    }
}
