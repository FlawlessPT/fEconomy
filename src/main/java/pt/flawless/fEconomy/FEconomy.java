package pt.flawless.fEconomy;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import pt.flawless.fEconomy.commands.MoneyCommand;
import pt.flawless.fEconomy.listeners.PlayerAuthListener;
import pt.flawless.fEconomy.services.EconomyRepository;
import pt.flawless.fEconomy.services.EconomyService;
import pt.flawless.fapi.database.DatabaseService;
import pt.flawless.fapi.economy.EconomyApi;
import pt.flawless.fapi.database.Databases;
import pt.flawless.fapi.enums.EMessageTypeEnum;
import pt.flawless.fapi.logs.FConsoleLogger;

import java.sql.SQLException;
import java.util.List;

public class FEconomy extends JavaPlugin {
    private final FConsoleLogger consoleLogger = new FConsoleLogger(this.getName());
    private boolean enabledOk = false;

    @Override
    public void onEnable() {
        try {
            DatabaseService database = Databases.get();
            database.migrate(this, List.of("db/feconomy/V001__create_balances.sql"));
            EconomyService service = new EconomyService(new EconomyRepository(database));

            getCommand("money").setExecutor(new MoneyCommand(service, this));
            getServer().getPluginManager().registerEvents(new PlayerAuthListener(service, this), this);
            getServer().getServicesManager().register(EconomyApi.class, service, this, ServicePriority.Normal);
        } catch (SQLException | RuntimeException e) {
            // inclui DatabaseUnavailableException (RuntimeException)
            consoleLogger.sendMessage("não foi possível iniciar a economia: " + e.getMessage(), EMessageTypeEnum.ERROR);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        enabledOk = true;
        consoleLogger.sendEnablePluginMessage();
    }

    @Override
    public void onDisable() {
        if (enabledOk) {
            consoleLogger.sendDisablePluginMessage();
        }
    }
}
