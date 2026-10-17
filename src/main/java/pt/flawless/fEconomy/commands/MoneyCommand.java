package pt.flawless.fEconomy.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pt.flawless.fapi.economy.AccountNotFoundException;
import pt.flawless.fapi.economy.InsufficientFundsException;
import pt.flawless.fEconomy.money.Money;
import pt.flawless.fEconomy.services.EconomyService;
import pt.flawless.fapi.enums.EMessageTypeEnum;
import pt.flawless.fapi.logs.FConsoleLogger;

import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;

public class MoneyCommand implements CommandExecutor {
    private static final String INVALID_AMOUNT = "§cMontante inválido. Usa por exemplo 12,50.";

    private final EconomyService service;
    private final Plugin plugin;

    public MoneyCommand(EconomyService service, Plugin plugin) {
        this.service = service;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args.length > 0 && isAdminOp(args[0])) {
                admin(sender, args);
                return true;
            }
            if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
                help(sender);
                return true;
            }
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
                if (args[0].equalsIgnoreCase("send")) {
                    player.sendMessage("§cUso: /money send <jogador> <montante>");
                } else {
                    // /money help é só para staff por agora: os outros não recebem uma dica que não podem usar
                    player.sendMessage(player.hasPermission("feconomy.admin")
                            ? "§cSubcomando desconhecido. Usa /money help para ver os comandos disponíveis."
                            : "§cSubcomando desconhecido.");
                }
                return true;
            }

            later(service.getBalance(player.getUniqueId()), "/money de " + player.getName(),
                    (balance, error) -> reply(player, balance, error));
        } catch (RuntimeException e) {
            sender.sendMessage("§cNão foi possível obter o saldo.");
            FConsoleLogger.sendConsoleMessage("falha em /money: " + e.getMessage(), EMessageTypeEnum.ERROR);
        }
        return true;
    }

    private static boolean isAdminOp(String arg) {
        return arg.equalsIgnoreCase("add") || arg.equalsIgnoreCase("remove") || arg.equalsIgnoreCase("set");
    }

    /** Agenda o tratamento do resultado na thread principal. */
    private <T> void later(CompletableFuture<T> future, String what, BiConsumer<T, Throwable> handler) {
        future.whenComplete((value, error) -> {
            try {
                Bukkit.getScheduler().runTask(plugin, () -> handler.accept(value, error));
            } catch (RuntimeException e) {
                // por exemplo o plugin foi desativado entretanto
                FConsoleLogger.sendConsoleMessage("não foi possível responder a " + what + ": " + e.getMessage(),
                        EMessageTypeEnum.ERROR);
            }
        });
    }

    private static Player findOnline(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendMessage("§cJogador não encontrado ou offline.");
        }
        return target;
    }

    private static Throwable unwrap(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }

    private static boolean expected(Throwable cause) {
        return cause instanceof InsufficientFundsException || cause instanceof AccountNotFoundException;
    }

    /** Corre na thread principal. */
    private void send(Player player, String targetName, String amountText) {
        Player target = findOnline(player, targetName);
        if (target == null) {
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cNão podes enviar dinheiro a ti próprio.");
            return;
        }
        OptionalLong cents = Money.parse(amountText);
        if (cents.isEmpty()) {
            player.sendMessage(INVALID_AMOUNT);
            return;
        }

        long amount = cents.getAsLong();
        later(service.transfer(player.getUniqueId(), target.getUniqueId(), amount), "/money send de " + player.getName(),
                (balance, error) -> replySend(player, target, amount, balance, error));
    }

    /** Corre na thread principal. */
    private void replySend(Player player, Player target, long cents, Long balance, Throwable error) {
        Throwable cause = unwrap(error);
        if (cause != null && !expected(cause)) {
            FConsoleLogger.sendConsoleMessage("não foi possível transferir de " + player.getName() + " para "
                    + target.getName() + ": " + rootMessage(error), EMessageTypeEnum.ERROR);
        }
        if (player.isOnline()) {
            if (cause == null) {
                player.sendMessage("§aEnviaste §7" + Money.format(cents) + " §aa " + target.getName()
                        + ". Novo saldo: §7" + Money.format(balance));
            } else if (cause instanceof InsufficientFundsException) {
                player.sendMessage("§cSaldo insuficiente.");
            } else if (cause instanceof AccountNotFoundException) {
                player.sendMessage("§cConta não encontrada.");
            } else {
                player.sendMessage("§cNão foi possível concluir a transferência.");
            }
        }
        if (cause == null && target.isOnline()) {
            String from = player.getName();
            later(service.getBalance(target.getUniqueId()), "/money send para " + target.getName(), (targetBalance, e) -> {
                if (target.isOnline()) {
                    target.sendMessage("§aRecebeste §7" + Money.format(cents) + " §ade " + from + "."
                            + (e == null ? " Novo saldo: §7" + Money.format(targetBalance) : ""));
                }
            });
        }
    }

    /** /money help: só para staff (feconomy.admin) por agora. Corre na thread principal; pode ser a consola. */
    private void help(CommandSender sender) {
        if (!sender.hasPermission("feconomy.admin")) {
            sender.sendMessage("§cNão tens permissão.");
            return;
        }
        sender.sendMessage("§e/money §7- Mostra o teu saldo");
        sender.sendMessage("§e/money send <jogador> <montante> §7- Envia dinheiro a um jogador online");
        sender.sendMessage("§e/money add <jogador> <montante> §7- Adiciona saldo a um jogador");
        sender.sendMessage("§e/money remove <jogador> <montante> §7- Remove saldo a um jogador");
        sender.sendMessage("§e/money set <jogador> <montante> §7- Define o saldo de um jogador");
        sender.sendMessage("§e/money help §7- Mostra esta lista");
    }

    /** /money add|remove|set. Corre na thread principal; o remetente pode ser a consola. */
    private void admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("feconomy.admin")) {
            sender.sendMessage("§cNão tens permissão.");
            return;
        }
        if (args.length != 3) {
            sender.sendMessage("§cUso: /money " + args[0].toLowerCase() + " <jogador> <montante>");
            return;
        }
        String op = args[0].toLowerCase();
        Player target = findOnline(sender, args[1]);
        if (target == null) {
            return;
        }
        boolean set = op.equals("set");
        OptionalLong cents = set ? Money.parseAllowZero(args[2]) : Money.parse(args[2]);
        if (cents.isEmpty()) {
            sender.sendMessage(INVALID_AMOUNT);
            return;
        }

        long amount = cents.getAsLong();
        CompletableFuture<Long> future = switch (op) {
            case "add" -> service.deposit(target.getUniqueId(), amount);
            case "remove" -> service.withdraw(target.getUniqueId(), amount);
            default -> service.setBalance(target.getUniqueId(), amount);
        };
        later(future, "/money " + op + " de " + sender.getName(),
                (balance, error) -> replyAdmin(sender, target, op, amount, balance, error));
    }

    /** Corre na thread principal. */
    private void replyAdmin(CommandSender sender, Player target, String op, long cents, Long balance, Throwable error) {
        Throwable cause = unwrap(error);
        if (cause != null && !expected(cause)) {
            FConsoleLogger.sendConsoleMessage("não foi possível executar /money " + op + " de " + sender.getName()
                    + " em " + target.getName() + ": " + rootMessage(error), EMessageTypeEnum.ERROR);
        }
        if (!(sender instanceof Player) || ((Player) sender).isOnline()) {
            if (cause == null) {
                String now = Money.format(balance);
                String name = target.getName();
                sender.sendMessage(switch (op) {
                    case "add" -> "§aAdicionaste §7" + Money.format(cents) + " §aa " + name + ". Novo saldo: §7" + now;
                    case "remove" -> "§aRemoveste §7" + Money.format(cents) + " §aa " + name + ". Novo saldo: §7" + now;
                    default -> "§aDefiniste o saldo de " + name + " para §7" + now + "§a.";
                });
            } else if (cause instanceof InsufficientFundsException) {
                sender.sendMessage("§cO jogador não tem saldo suficiente.");
            } else if (cause instanceof AccountNotFoundException) {
                sender.sendMessage("§cConta não encontrada.");
            } else {
                sender.sendMessage("§cNão foi possível concluir a operação.");
            }
        }
        if (cause == null && target.isOnline() && !target.equals(sender)) {
            target.sendMessage("§eO teu saldo foi alterado: §7" + Money.format(balance));
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
