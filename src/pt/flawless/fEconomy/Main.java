package pt.flawless.fEconomy;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import pt.flawless.fapi.logs.FConsoleLogger;

public class Main extends JavaPlugin {
    private static Plugin plugin;

    @Override
    public void onEnable() {
        FConsoleLogger.sendEnablePlugin(plugin.getName());
    }

    @Override
    public void onDisable() {
        FConsoleLogger.sendDisablePlugin(plugin.getName());
    }

    public static Plugin getMainPlugin() {
        return plugin;
    }
}
