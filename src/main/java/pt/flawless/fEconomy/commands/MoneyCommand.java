package pt.flawless.fEconomy.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pt.flawless.fEconomy.exceptions.AccountNotFoundException;
import pt.flawless.fEconomy.exceptions.InsufficientFundsException;
import pt.flawless.fEconomy.money.Money;
import pt.flawless.fEconomy.services.EconomyService;
import pt.flawless.fapi.enums.EMessageTypeEnum;
import pt.flawless.fapi.logs.FConsoleLogger;

import java.util.OptionalLong;
import java.util.concurrent.CompletionException;

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

            Player player = (Player) sender;
            if (args.length == 3 && args[0].equalsIgnoreCase("send")) {
                send(player, args[1], args[2]);
                return true;
            }
            if (args.length != 0) {
                player.sendMessage("§cUso: /money [send <jogador> <montante>]");
                return true;
            }

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
    private void send(Player player, String targetName, String amountText) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage("§cJogador não encontrado ou offline.");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cNão podes enviar dinheiro a ti próprio.");
            return;
        }
        OptionalLong cents = Money.parse(amountText);
        if (cents.isEmpty()) {
            player.sendMessage("§cMontante inválido. Usa por exemplo 12,50.");
            return;
        }

        long amount = cents.getAsLong();
        service.transfer(player.getUniqueId(), target.getUniqueId(), amount).whenComplete((balance, error) -> {
            try {
                Bukkit.getScheduler().runTask(plugin, () -> replySend(player, target, amount, error));
            } catch (RuntimeException e) {
                // por exemplo o plugin foi desativado entretanto
                FConsoleLogger.sendConsoleMessage("não foi possível responder a /money send de "
                        + player.getName() + ": " + e.getMessage(), EMessageTypeEnum.ERROR);
            }
        });
    }

    /** Corre na thread principal. */
    private void replySend(Player player, Player target, long cents, Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        if (cause != null && !(cause instanceof InsufficientFundsException)
                && !(cause instanceof AccountNotFoundException)) {
            FConsoleLogger.sendConsoleMessage("não foi possível transferir de " + player.getName() + " para "
                    + target.getName() + ": " + rootMessage(error), EMessageTypeEnum.ERROR);
        }
        if (player.isOnline()) {
            if (cause == null) {
                player.sendMessage("§aEnviaste §7" + Money.format(cents) + " §aa " + target.getName() + ".");
            } else if (cause instanceof InsufficientFundsException) {
                player.sendMessage("§cSaldo insuficiente.");
            } else if (cause instanceof AccountNotFoundException) {
                player.sendMessage("§cConta não encontrada.");
            } else {
                player.sendMessage("§cNão foi possível concluir a transferência.");
            }
        }
        if (cause == null && target.isOnline()) {
            target.sendMessage("§aRecebeste §7" + Money.format(cents) + " §ade " + player.getName() + ".");
        }
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
