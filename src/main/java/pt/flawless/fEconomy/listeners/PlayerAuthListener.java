package pt.flawless.fEconomy.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import pt.flawless.fEconomy.services.EconomyService;
import pt.flawless.fapi.enums.EMessageTypeEnum;
import pt.flawless.fapi.events.PlayerAuthEvent.PlayerAuthEvent;
import pt.flawless.fapi.logs.FConsoleLogger;

public class PlayerAuthListener implements Listener {
    private final EconomyService service;
    private final Plugin plugin;

    public PlayerAuthListener(EconomyService service, Plugin plugin) {
        this.service = service;
        this.plugin = plugin;
    }

    /** Garante a conta em qualquer autenticação (registo ou login); assíncrono, não bloqueia a thread principal. */
    @EventHandler
    public void onPlayerAuth(PlayerAuthEvent e) {
        Player player = e.getPlayer();
        String name = player.getName();

        service.ensureAccount(player.getUniqueId(), name).whenComplete((ignored, error) -> {
            if (error != null) {
                FConsoleLogger.sendConsoleMessage("não foi possível garantir a conta de " + name + ": "
                        + error.getMessage(), EMessageTypeEnum.ERROR);
            }
        });
    }
}
