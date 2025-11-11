package xyz.kaijiieow.statusup.core;

import xyz.kaijiieow.statusup.StatusUp;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class FileLogger {

    private final Logger logger;
    private FileHandler fileHandler;

    public FileLogger(StatusUp plugin) {
        this.logger = Logger.getLogger(plugin.getName() + "FileLogger");
        this.logger.setUseParentHandlers(false);

        try {
            fileHandler = new FileHandler(plugin.getDataFolder() + "/statusup.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
            
            logInfo("File logging started.");
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize file logger!", e);
        }
    }

    public void logInfo(String message) {
        logger.log(Level.INFO, message);
    }

    public void logWarning(String message) {
        logger.log(Level.WARNING, message);
    }

    public void logError(String message, Throwable t) {
        logger.log(Level.SEVERE, message, t);
    }

    public void close() {
        if (fileHandler != null) {
            fileHandler.close();
        }
    }
}