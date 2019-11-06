package fr.mineral.Commands;

import fr.mineral.Utils.Save.FileToGame;
import fr.mineral.Utils.Setup;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetupCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("setup")) {
            if(sender.isOp()) {
                if(Setup.instance == null) {
                    Setup s = new Setup();
                    Setup.displayInfos((Player) sender);
                }
            }
        }
        return false;
    }
}
