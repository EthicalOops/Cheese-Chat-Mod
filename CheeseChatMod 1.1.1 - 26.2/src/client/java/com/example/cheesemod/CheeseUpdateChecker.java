package com.example.cheesemod;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks GitHub for a newer release of the mod, matching only releases
 * tagged for the Minecraft version currently running (so a 26.2 release
 * doesn't get shown to 1.21.11 users, or vice versa).
 *
 * Expects GitHub tags in the form "v<mod_version>+<mc_version>",
 * e.g. "v1.1.0+26.2" or "v1.1.0+1.21.11".
 *
 * The network check runs immediately on client start (doesn't need a
 * world), but the chat message itself is held back until the player is
 * confirmed connected to Hypixel — never shown at the title screen, in
 * singleplayer, or on other servers.
 *
 * For the 26.2 branch (Mojang mappings). Call CheeseUpdateChecker.checkForUpdates()
 * once from onInitializeClient().
 */
public class CheeseUpdateChecker {

    // CHANGE THIS to your actual GitHub repo, e.g. "JamesXYZ/CheeseChatMod"
    private static final String REPO = "yourusername/cheesemod";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases";

    private static final Pattern TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern HTML_URL_PATTERN = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");

    private static volatile String pendingCurrent;
    private static volatile String pendingLatest;
    private static volatile String pendingUrl;
    private static volatile boolean hasPendingMessage = false;
    private static volatile boolean alreadyShown = false;

    public static void checkForUpdates() {
        ClientTickEvents.END_CLIENT_TICK.register(CheeseUpdateChecker::onClientTick);

        CompletableFuture.runAsync(() -> {
            try {
                String currentVersion = FabricLoader.getInstance()
                        .getModContainer("cheesemod")
                        .map(c -> c.getMetadata().getVersion().getFriendlyString())
                        .orElse("unknown");

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .timeout(Duration.ofSeconds(5))
                        .header("Accept", "application/vnd.github+json")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) return;

                String body = response.body();
                String mcVersion = getCurrentMinecraftVersion();

                List<String> tags = new ArrayList<>();
                Matcher tagMatcher = TAG_PATTERN.matcher(body);
                while (tagMatcher.find()) tags.add(tagMatcher.group(1));

                List<String> urls = new ArrayList<>();
                Matcher urlMatcher = HTML_URL_PATTERN.matcher(body);
                while (urlMatcher.find()) urls.add(urlMatcher.group(1));

                String bestVersion = null;
                String bestUrl = null;

                for (int i = 0; i < tags.size(); i++) {
                    String tag = tags.get(i);
                    if (!tag.contains(mcVersion)) continue; // skip releases for other MC versions

                    String versionPart = tag.replaceFirst("^v", "").split("\\+")[0];
                    if (bestVersion == null || isNewer(versionPart, bestVersion)) {
                        bestVersion = versionPart;
                        bestUrl = (i < urls.size()) ? urls.get(i) : null;
                    }
                }

                if (bestVersion != null && isNewer(bestVersion, currentVersion)) {
                    pendingCurrent = currentVersion;
                    pendingLatest = bestVersion;
                    pendingUrl = bestUrl;
                    hasPendingMessage = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static String getCurrentMinecraftVersion() {
        return FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("");
    }

    private static void onClientTick(Minecraft client) {
        if (alreadyShown || !hasPendingMessage) return;
        if (client.player == null) return;
        if (!isOnHypixel(client)) return;

        client.player.sendSystemMessage(Component.literal(
                "§6[Cheese Chat] §eYour mod is outdated! §7Current: §c" + pendingCurrent
                        + " §7Latest: §a" + pendingLatest
        ));
        if (pendingUrl != null) {
            Component link = Component.literal("§n§7Click here to view the release")
                .withStyle(style -> style
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(URI.create(pendingUrl)))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(
                                Component.literal(pendingUrl))));
        client.player.sendSystemMessage(link);
    }
        alreadyShown = true;
    }

    private static boolean isOnHypixel(Minecraft client) {
        ServerData server = client.getCurrentServer();
        if (server == null) return false; // singleplayer, LAN, or not yet connected
        String address = server.ip;
        return address != null && address.toLowerCase().contains("hypixel.net");
    }

    /** Basic dotted-number version compare, ignoring anything after a "+" build tag. */
    private static boolean isNewer(String latest, String current) {
        String[] latestParts = latest.split("\\+")[0].split("\\.");
        String[] currentParts = current.split("\\+")[0].split("\\.");
        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int l = i < latestParts.length ? parseIntSafe(latestParts[i]) : 0;
            int c = i < currentParts.length ? parseIntSafe(currentParts[i]) : 0;
            if (l != c) return l > c;
        }
        return false;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
