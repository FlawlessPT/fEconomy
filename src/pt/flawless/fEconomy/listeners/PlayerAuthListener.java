package pt.flawless.fEconomy.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import pt.flawless.fEconomy.services.EconomyService;
import pt.flawless.fapi.enums.FAuthType;
import pt.flawless.fapi.events.PlayerAuthEvent.PlayerAuthEvent;

public class PlayerAuthListener implements Listener {
    @EventHandler
    public void onPlayerAuth(PlayerAuthEvent e) {
        Player player = e.getPlayer();

        if (e.getAuthType() == FAuthType.REGISTRATION) {
            EconomyService.createPlayerMoney(player.getUniqueId(), player.getName());
            player.sendMessage("§eAccount created");

        } else {
            double currentBalance = EconomyService.getPlayerMoney(player.getUniqueId());
            double newBalance = EconomyService.addPlayerMoney(player.getUniqueId(), 100);
            player.sendMessage("§e100€ adicionados à conta. Novo saldo: §7%new_balance%".replace("%new_balance%", String.valueOf(currentBalance + newBalance)));
        }
    }
}
