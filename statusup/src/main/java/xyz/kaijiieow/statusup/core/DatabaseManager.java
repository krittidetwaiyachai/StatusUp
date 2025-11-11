package xyz.kaijiieow.statusup.core;

import xyz.kaijiieow.statusup.StatusUp;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final StatusUp plugin;
    private final FileLogger logger;
    private Connection connection;
    private final String dbType;

    private String host, database, username, password;
    private int port;
    
    private File dbFile;

    public DatabaseManager(StatusUp plugin, FileLogger logger, FileConfiguration settings) {
        this.plugin = plugin;
        this.logger = logger;
        this.dbType = settings.getString("database.type", "none").toLowerCase();

        switch (this.dbType) {
            case "mysql":
                this.host = settings.getString("database.mysql.host");
                this.port = settings.getInt("database.mysql.port");
                this.database = settings.getString("database.mysql.database");
                this.username = settings.getString("database.mysql.username");
                this.password = settings.getString("database.mysql.password");
                break;
            case "sqlite":
                String filename = settings.getString("database.sqlite.filename", "logs.db");
                this.dbFile = new File(plugin.getDataFolder(), filename);
                break;
            default:
                logger.logInfo("Database logging is disabled (type: 'none').");
                return;
        }

        connect();
        createTableAsync();
    }

    private void connect() {
        if (dbType.equals("none")) return;
        
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }

            if (dbType.equals("mysql")) {
                String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false";
                this.connection = DriverManager.getConnection(jdbcUrl, username, password);
                logger.logInfo("Successfully connected to MySQL Database.");

            } else if (dbType.equals("sqlite")) {
                Class.forName("org.sqlite.JDBC"); 
                
                if (!dbFile.exists()) {
                    try {
                        dbFile.getParentFile().mkdirs();
                        dbFile.createNewFile();
                        logger.logInfo("Created new SQLite database file: " + dbFile.getName());
                    } catch (IOException e) {
                        logger.logError("Failed to create SQLite database file!", e);
                        return;
                    }
                }
                
                String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
                this.connection = DriverManager.getConnection(jdbcUrl);
                logger.logInfo("Successfully connected to SQLite Database.");
            }
            
        } catch (SQLException | ClassNotFoundException e) {
            logger.logError("Failed to connect to " + dbType + " Database!", e);
        }
    }

    private void createTableAsync() {
        if (dbType.equals("none")) return;
        
        CompletableFuture.runAsync(() -> {
            String sql;
            if (dbType.equals("mysql")) {
                sql = "CREATE TABLE IF NOT EXISTS statusup_logs ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "player_uuid VARCHAR(36) NOT NULL,"
                    + "player_name VARCHAR(16) NOT NULL,"
                    + "upgrade_type VARCHAR(10) NOT NULL,"
                    + "from_group VARCHAR(50) NOT NULL,"
                    + "to_group VARCHAR(50) NOT NULL"
                    + ");";
            } else { 
                sql = "CREATE TABLE IF NOT EXISTS statusup_logs ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "player_uuid TEXT NOT NULL,"
                    + "player_name TEXT NOT NULL,"
                    + "upgrade_type TEXT NOT NULL,"
                    + "from_group TEXT NOT NULL,"
                    + "to_group TEXT NOT NULL"
                    + ");";
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            } catch (SQLException e) {
                logger.logError("Failed to create 'statusup_logs' table!", e);
            }
        });
    }

    public void logUpgradeAsync(Player player, String fromGroup, String toGroup, String upgradeType) {
        if (dbType.equals("none")) return;
        
        CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO statusup_logs (player_uuid, player_name, upgrade_type, from_group, to_group) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, player.getUniqueId().toString());
                pstmt.setString(2, player.getName());
                pstmt.setString(3, upgradeType);
                pstmt.setString(4, fromGroup);
                pstmt.setString(5, toGroup);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.logError("Failed to log upgrade to database!", e);
                connect(); 
            }
        });
    }

    public void closeConnection() {
        if (dbType.equals("none")) return;
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.logInfo(dbType + " Database connection closed.");
            }
        } catch (SQLException e) {
            logger.logError("Error while closing database connection!", e);
        }
    }
}