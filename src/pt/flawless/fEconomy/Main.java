package pt.flawless.fEconomy;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import pt.flawless.fEconomy.commands.MoneyCommand;
import pt.flawless.fEconomy.database.DatabaseConnector;
import pt.flawless.fEconomy.listeners.PlayerAuthListener;
import pt.flawless.fapi.logs.FConsoleLogger;

import java.sql.SQLException;

public class Main extends JavaPlugin {
    private static Plugin plugin;

    @Override
    public void onEnable() {
        plugin = this;

        try {
            DatabaseConnector.init();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        registerCommands();
        registerListeners();

        FConsoleLogger.sendEnablePlugin(plugin.getName());
    }

    @Override
    public void onDisable() {
        FConsoleLogger.sendDisablePlugin(plugin.getName());
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
