package pt.flawless.fEconomy.services;

import org.bukkit.Bukkit;
import pt.flawless.fEconomy.database.DatabaseConnector;
import pt.flawless.fapi.database.LocalDatabase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class EconomyService {
    private static LocalDatabase database = DatabaseConnector.database;

    public static void createPlayerMoney(UUID uuid, String username) {
        String sql = "INSERT INTO user_money(uuid, username) VALUES (?, ?);";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            // TODO: Create EconomyServiceException
            throw new RuntimeException(e);
        }
    }

    public static double getPlayerMoney(UUID uuid) {
        try {
            PreparedStatement stmt = database.getConnection().prepareStatement("SELECT balance FROM user_money WHERE uuid = ?;");
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                double balance = rs.getDouble("balance");

                return balance;
            }
        } catch (SQLException e) {
            // TODO: Create EconomyServiceException
            Bukkit.getConsoleSender().sendMessage(e.getMessage());
            throw new RuntimeException(e);
        }

        return 0;
    }

    public static double setPlayerMoney(UUID uuid, double newBalance) {
        try {
            PreparedStatement stmt = database.getConnection().prepareStatement("UPDATE user_money SET balance = ? WHERE uuid = ?;");
            stmt.setDouble(1, newBalance);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();

            return newBalance;
        } catch (SQLException e) {
            // TODO: Create EconomyServiceException
            throw new RuntimeException(e);
        }
    }

    public static double addPlayerMoney(UUID uuid, double amount) {
        try {
            // Update balance
            PreparedStatement stmt = database.getConnection().prepareStatement("UPDATE user_money SET balance = balance + ? WHERE uuid = ?;");
            stmt.setDouble(1, amount);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();

//            // Fetch new balance
//            PreparedStatement selectStmt = database.getConnection().prepareStatement("SELECT balance FROM user_money WHERE uuid = ?;");
//            selectStmt.setString(1, uuid.toString());
//
//            ResultSet rs = stmt.executeQuery();
//            double newBalance = 0;
//            if (rs.next()) {
//                newBalance = rs.getDouble("balance");
//            }

            return amount;
        } catch (SQLException e) {
            // TODO: Create EconomyServiceException
            throw new RuntimeException(e);
        }
    }
}
