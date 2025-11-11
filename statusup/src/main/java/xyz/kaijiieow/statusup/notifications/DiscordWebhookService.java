package xyz.kaijiieow.statusup.notifications;

import xyz.kaijiieow.statusup.StatusUp;
import xyz.kaijiieow.statusup.core.FileLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhookService {

    private final FileLogger logger;
    private final HttpClient client;
    private final String webhookUrl;
    private final boolean enabled;

    private final String webhookUsername;
    private final String webhookAvatarUrl;

    public DiscordWebhookService(StatusUp plugin, FileLogger logger, FileConfiguration settings) {
        this.logger = logger;
        this.enabled = settings.getBoolean("discord.enabled", false);
        this.webhookUrl = settings.getString("discord.webhook_url", "");

        this.webhookUsername = settings.getString("discord.username", "StatusUp Notifier");
        this.webhookAvatarUrl = settings.getString("discord.avatar_url", "");

        if (this.enabled && (this.webhookUrl.isEmpty() || this.webhookUrl.equals("YOUR_DISCORD_WEBHOOK_URL_HERE"))) {
            logger.logWarning("Discord webhook is enabled, but the URL is missing in settings.yml!");
        }
        
        this.client = HttpClient.newHttpClient();
    }

    public void sendUpgradeNotification(Player player, String fromDisplay, String toDisplay, String upgradeType) {
        if (!enabled || this.webhookUrl.isEmpty() || this.webhookUrl.equals("YOUR_DISCORD_WEBHOOK_URL_HERE")) {
            return;
        }

        String json = buildFullPayload(player, fromDisplay, toDisplay, upgradeType);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        CompletableFuture.runAsync(() -> {
            try {
                client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                logger.logError("Failed to send Discord webhook!", e);
            }
        });
    }

    private String buildFullPayload(Player player, String from, String to, String type) {
        String title, color, emoji;
        if (type.equals("RANKUP")) {
            title = "Player Ranked Up!";
            color = "3066993";
            emoji = ":arrow_double_up:";
        } else {
            title = "Player Starred Up!";
            color = "15844367";
            emoji = ":star:";
        }

        String playerName = player.getName();
        String playerHeadUrl = "https://crafatar.com/avatars/" + player.getUniqueId() + "?overlay";

        String embedObject = "{"
            + "    \"title\": \"" + escapeJson(emoji + " " + title) + "\","
            + "    \"color\": " + color + ","
            + "    \"description\": \"**" + escapeJson(playerName) + "** has upgraded!\","
            + "    \"fields\": ["
            + "      {\"name\": \"From\", \"value\": \"" + escapeJson(from) + "\", \"inline\": true},"
            + "      {\"name\": \"To\", \"value\": \"" + escapeJson(to) + "\", \"inline\": true}"
            + "    ],"
            + "    \"thumbnail\": {\"url\": \"" + escapeJson(playerHeadUrl) + "\"},"
            + "    \"timestamp\": \"" + Instant.now().toString() + "\""
            + "  }";

        return "{"
            + "\"username\": \"" + escapeJson(this.webhookUsername) + "\","
            + "\"avatar_url\": \"" + escapeJson(this.webhookAvatarUrl) + "\","
            + "\"embeds\": [" + embedObject + "]"
            + "}";
    }

    private String escapeJson(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}