package pt.flawless.fEconomy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pt.flawless.fEconomy.services.EconomyService;

public class MoneyCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;

            if (args.length == 0) {
                double currentUserMoney = EconomyService.getPlayerMoney(player.getUniqueId());

                player.sendMessage("§eSaldo atual: §7%balance%€".replace("%balance%", String.valueOf(currentUserMoney)));
            }
        }

        return false;
    }
}
