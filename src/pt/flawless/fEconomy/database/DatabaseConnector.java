package pt.flawless.fEconomy.database;

import org.bukkit.Bukkit;
import pt.flawless.fEconomy.Main;
import pt.flawless.fapi.database.LocalDatabase;
import pt.flawless.fapi.database.LocalDatabaseImpl;

import java.sql.SQLException;

public class DatabaseConnector {
    public static LocalDatabase database;

    public static void init() throws SQLException {
        String createTableScript = "CREATE TABLE IF NOT EXISTS user_money (" +
                "uuid TEXT PRIMARY KEY," +
                "username TEXT NOT NULL," +
                "balance REAL DEFAULT 0 CHECK (balance >= 0)" +
                ");";

        LocalDatabaseImpl.init(Main.getMainPlugin().getDataFolder(), () -> {
            try {
                database = new LocalDatabase(Main.getMainPlugin().getDataFolder(), "economy", createTableScript);
                database.connect();
            } catch (SQLException e) {
                Bukkit.getConsoleSender().sendMessage("SQL Exception - Error creating database!");
            }
        });
    }
}
