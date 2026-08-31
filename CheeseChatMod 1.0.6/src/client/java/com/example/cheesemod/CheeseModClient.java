package com.example.cheesemod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
            "Cheesy Michael"
    );

    // The trigger word/phrase to look for (case-insensitive).
    private static final String TRIGGER = "!cheese";

    // Minimum time (ms) between auto-sent responses, so the mod can't spam
    // chat or trigger itself in a loop if someone spams !cheese.
    private static final long COOLDOWN_MS = 3000;

    private long lastTriggerTime = 0;

    @Override
    public void onInitializeClient() {
        CheeseConfig.load();

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
        sendChatMessage(channel, pickRandomResponse());
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
        int index = ThreadLocalRandom.current().nextInt(RESPONSE_MESSAGES.size());
        return RESPONSE_MESSAGES.get(index);
    }

    private void sendChatMessage(ChatChannel channel, String text) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.player.connection == null) {
            return;
        }
        String command = channel.commandPrefix + " " + text;
        // Runs on the next client tick to make sure we're not mid-event-processing.
        client.execute(() -> client.player.connection.sendCommand(command));
    }
}
