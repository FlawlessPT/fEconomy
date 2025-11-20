package pt.flawless.fEconomy.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import pt.flawless.fEconomy.services.EconomyService;
import pt.flawless.fapi.enums.FAuthType;
import pt.flawless.fapi.events.PlayerAuthEvent.PlayerAuthEvent;
import pt.flawless.fapi.logs.FConsoleLogger;

public class PlayerAuthListener implements Listener {
    @EventHandler
    public void onPlayerAuth(PlayerAuthEvent e) {
        Player player = e.getPlayer();

        if (e.getAuthType() == FAuthType.REGISTRATION) {
            EconomyService.createPlayerMoney(player.getUniqueId(), player.getName());
            FConsoleLogger.sendConsoleMessage("§eAccount created for " + player.getName());
        }
    }
}
