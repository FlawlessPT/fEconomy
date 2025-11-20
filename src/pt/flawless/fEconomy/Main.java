package pt.flawless.fEconomy;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import pt.flawless.fEconomy.commands.MoneyCommand;
import pt.flawless.fEconomy.database.DatabaseConnector;
import pt.flawless.fEconomy.listeners.PlayerAuthListener;
import pt.flawless.fapi.enums.EMessageTypeEnum;
import pt.flawless.fapi.logs.FConsoleLogger;

import java.sql.SQLException;

public class Main extends JavaPlugin {
    private static Plugin plugin;
    FConsoleLogger consoleLogger = new FConsoleLogger(this.getName());

    @Override
    public void onEnable() {
        plugin = this;

        try {
            DatabaseConnector.init();
        } catch (SQLException e) {
            consoleLogger.sendMessage("Error initializing database", EMessageTypeEnum.ERROR);
            throw new RuntimeException(e);
        }

        registerCommands();
        registerListeners();

        consoleLogger.sendEnablePluginMessage();
    }

    @Override
    public void onDisable() {
        consoleLogger.sendDisablePluginMessage();
    }

    private void registerCommands() {
        getCommand("money").setExecutor(new MoneyCommand());
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerAuthListener(), this);
    }

    public static Plugin getMainPlugin() {
        return plugin;
    }
}
