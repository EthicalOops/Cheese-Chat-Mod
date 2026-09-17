package com.example.cheesemod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Cheese Mod
 *
 * Watches incoming chat messages. Whenever someone types "!cheese" in a
 * Hypixel chat channel the player has enabled (Guild/Party/Co-op, toggled
 * via /cheese), this automatically sends a random response message.
 */
public class CheeseModClient implements ClientModInitializer {

    // Edit this list to add/remove/change what the mod can paste into chat.
    // One is picked at random each time !cheese is triggered.
    private static final List<String> RESPONSE_MESSAGES = List.of(
            "Hrm I think Cheese",
            "Very Cheesy",
            "O.o thats a very cheesy panini",
            "Cheese is a dairy product derived from milk that is produced in a wide range of flavors, textures, and forms by coagulation of the milk protein casein.",
            "Cheesy Michael",
            "Thats not the Jungle Cheese!"
    );

    private static final int[] TIER_DENOMINATORS = {5000, 2500, 1000, 500, 200, 100};

    private static final CheeseAchievements.Achievement[] TIER_ACHIEVEMENTS = {
        CheeseAchievements.Achievement.DIVINE_CHEESE,
        CheeseAchievements.Achievement.LEGENDARY_CHEESE,
        CheeseAchievements.Achievement.EPIC_CHEESE,
        CheeseAchievements.Achievement.RARE_CHEESE,
        CheeseAchievements.Achievement.UNCOMMON_CHEESE,
        CheeseAchievements.Achievement.COMMON_CHEESE
    };

    private static final String[] TIER_MESSAGES = {
        "Divine Cheese! You found the rarest cheese of all!",
        "RNGesus Incarnate CHEESE! WTF! You found the second rarest cheese of all!",
        "Pray RNGesus Cheese! That's a rare find.",
        "Extraordinary Cheese! Not bad at all.",
        "Very Rare Cheese! A little something extra.",
        "Rare Cheese! You found it."
    };

    // The trigger word/phrase to look for (case-insensitive).
    private static final String TRIGGER = "!cheese";

    // Minimum time (ms) between auto-sent responses, so the mod can't spam
    // chat or trigger itself in a loop if someone spams !cheese.
    private static final long COOLDOWN_MS = 1000;
    private volatile long lastTriggerTime = 0;

    //tracking field for last send message
    private volatile String lastSentMessage = null;

    private volatile CheeseAchievements.Achievement pendingAchievement = null;

    // Used to delay the reply so it doesn't look instant/bot-like.
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "cheese-mod-delay");
                t.setDaemon(true);
                return t;
            });

    // Reply delay range, in milliseconds.
    private static final long MIN_DELAY_MS = 400;
    private static final long MAX_DELAY_MS = 600;

    @Override
    public void onInitializeClient() {
        CheeseConfig.load();
        CheeseAchievements.load();
        CheeseUpdateChecker.checkForUpdates();
        // GAME fires for any message sent by the server - this is what Hypixel
        // uses for its chat (regular messages, party, guild, etc.), since it
        // doesn't send vanilla signed player chat.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return; // ignore action bar messages
            handleMessage(message.getString());
        });

        // /cheese opens the settings GUI to toggle which channels count.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("cheese")
                        .executes(context -> {
                            Minecraft client = Minecraft.getInstance();
                            client.execute(() -> client.setScreen(new CheeseSettingsScreen()));
                            return 1;
                        })));
    }

    private void handleMessage(String content) {
        if (content == null) return;

        String clean = stripFormatting(content);
        if (!clean.toLowerCase().contains(TRIGGER)) return;

        // Only guild/party/co-op chat count, and only if that channel is
        // enabled in settings - it should never fire off public/all chat.
        ChatChannel channel = detectChannel(clean);
        if (channel == null || !channel.isEnabled()) return;

        long now = System.currentTimeMillis();
        if (now - lastTriggerTime < COOLDOWN_MS) {
            return; // still cooling down, avoid spam/loops
        }
        lastTriggerTime = now;

        String response = pickRandomResponse();
        long delay = ThreadLocalRandom.current().nextLong(MIN_DELAY_MS, MAX_DELAY_MS + 1);
        scheduler.schedule(() -> sendChatMessage(channel, response), delay, TimeUnit.MILLISECONDS);
        }

    // Hypixel messages come through with legacy "§x" color/formatting codes
    // still embedded (e.g. "§bCo-op > §b[MVP§f+§b] Name: !cheese"), so we
    // need to strip those before matching on the channel prefix or trigger.
    private static final java.util.regex.Pattern FORMATTING_CODE =
            java.util.regex.Pattern.compile("(?i)\u00A7[0-9A-FK-OR]");

    private String stripFormatting(String content) {
        return FORMATTING_CODE.matcher(content).replaceAll("");
    }

    // Matches "Guild >", "Guild>", "Party >", "Co-op>", etc. at the start of
    // the (already formatting-stripped) message.
    private static final java.util.regex.Pattern CHANNEL_PREFIX =
            java.util.regex.Pattern.compile("^(Guild|Party|Co-op)\\s*>",
                    java.util.regex.Pattern.CASE_INSENSITIVE);

    private ChatChannel detectChannel(String cleanContent) {
        java.util.regex.Matcher matcher = CHANNEL_PREFIX.matcher(cleanContent.trim());
        if (!matcher.find()) return null;

        String label = matcher.group(1);
        for (ChatChannel channel : ChatChannel.values()) {
            if (channel.label.equalsIgnoreCase(label)) {
                return channel;
            }
        }
        return null;
    }
    
    private String pickRandomResponse() {
        for (int i = 0; i < TIER_DENOMINATORS.length; i++) {
            if (ThreadLocalRandom.current().nextInt(1, TIER_DENOMINATORS[i] + 1) == 1) {
                playSecretJingle();
                pendingAchievement = TIER_ACHIEVEMENTS[i];
                return TIER_MESSAGES[i];
            }
        }

        List<String> pool = RESPONSE_MESSAGES;
        if (lastSentMessage != null && RESPONSE_MESSAGES.size() > 1) {
            pool = RESPONSE_MESSAGES.stream()
                    .filter(msg -> !msg.equals(lastSentMessage))
                    .toList();
        }
        int index = ThreadLocalRandom.current().nextInt(pool.size());
        String selected = pool.get(index);
        lastSentMessage = selected;
        return selected;
    }

    private void notifyAchievementUnlocked(CheeseAchievements.Achievement achievement, int count) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        String color = getAchievementColor(achievement);
        String message = count == 1
                ? "§6§lAchievement Get! §r" + color + "§l" + achievement.title
                : color + "§l" + achievement.title + " §r§e#" + count;
        client.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
    }

    private String getAchievementColor(CheeseAchievements.Achievement achievement) {
        return switch (achievement) {
            case COMMON_CHEESE -> "§9";      // blue
            case UNCOMMON_CHEESE -> "§1";    // dark blue
            case RARE_CHEESE -> "§5";        // purple
            case EPIC_CHEESE -> "§d";        // hot pink (light purple in MC's palette)
            case LEGENDARY_CHEESE -> "§c";   // red
            case DIVINE_CHEESE -> "§b";      // cyan
        };
    }

    private void playSecretJingle() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f));
    }

    private void sendChatMessage(ChatChannel channel, String text) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.player.connection == null) {
            return;
        }

        String command = channel.commandPrefix + " " + text;
        // Runs on the next client tick to make sure we're not mid-event-processing.
        client.execute(() -> {
            client.player.connection.sendCommand(command);

            if (pendingAchievement != null) {
                CheeseAchievements.Achievement achievement = pendingAchievement;
                pendingAchievement = null;
                int count = CheeseAchievements.increment(achievement);
                notifyAchievementUnlocked(achievement, count);
            }
        });
    }
}
