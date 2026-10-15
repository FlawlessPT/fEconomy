package pt.flawless.fEconomy.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pt.flawless.fEconomy.money.Money;
import pt.flawless.fEconomy.services.EconomyService;
import pt.flawless.fapi.enums.EMessageTypeEnum;
import pt.flawless.fapi.logs.FConsoleLogger;

public class MoneyCommand implements CommandExecutor {
    private final EconomyService service;
    private final Plugin plugin;

    public MoneyCommand(EconomyService service, Plugin plugin) {
        this.service = service;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cApenas jogadores podem usar este comando.");
                return true;
            }
            if (args.length != 0) {
                return false;
            }

            Player player = (Player) sender;
            service.getBalance(player.getUniqueId()).whenComplete((balance, error) -> {
                try {
                    Bukkit.getScheduler().runTask(plugin, () -> reply(player, balance, error));
                } catch (RuntimeException e) {
                    // por exemplo o plugin foi desativado entretanto
                    FConsoleLogger.sendConsoleMessage("não foi possível responder a /money de "
                            + player.getName() + ": " + e.getMessage(), EMessageTypeEnum.ERROR);
                }
            });
        } catch (RuntimeException e) {
            sender.sendMessage("§cNão foi possível obter o saldo.");
            FConsoleLogger.sendConsoleMessage("falha em /money: " + e.getMessage(), EMessageTypeEnum.ERROR);
        }
        return true;
    }

    /** Corre na thread principal. */
    private void reply(Player player, Long balance, Throwable error) {
        if (error != null) {
            FConsoleLogger.sendConsoleMessage("não foi possível obter o saldo de " + player.getName()
                    + ": " + rootMessage(error), EMessageTypeEnum.ERROR);
        }
        if (!player.isOnline()) {
            return;
        }
        if (error != null) {
            player.sendMessage("§cNão foi possível obter o saldo.");
        } else {
            player.sendMessage("§eSaldo atual: §7" + Money.format(balance));
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable t = error;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : "");
    }
}
