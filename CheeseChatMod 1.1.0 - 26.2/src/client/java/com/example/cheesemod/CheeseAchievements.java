package com.example.cheesemod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CheeseAchievements {

    public enum Achievement {
        SECRET_CHEESE("Secret Cheese", "Trigger the 1-in-1000 secret cheese message.", true);

        public final String title;
        public final String description;
        public final boolean hiddenUntilUnlocked;

        Achievement(String title, String description, boolean hiddenUntilUnlocked) {
            this.title = title;
            this.description = description;
            this.hiddenUntilUnlocked = hiddenUntilUnlocked;
        }
    }

    private static final Path SAVE_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("cheesemod-achievements.txt");

    private static final Map<Achievement, Integer> counts = new EnumMap<>(Achievement.class);

    public static void load() {
        counts.clear();
        if (!Files.exists(SAVE_FILE)) return;
        try {
            for (String line : Files.readAllLines(SAVE_FILE)) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(":", 2);
                if (parts.length != 2) continue;
                try {
                    Achievement achievement = Achievement.valueOf(parts[0]);
                    int count = Integer.parseInt(parts[1]);
                    counts.put(achievement, count);
                } catch (IllegalArgumentException ignored) {
                    // old/unknown entry or bad count, skip it
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        try {
            String content = counts.entrySet().stream()
                    .map(e -> e.getKey().name() + ":" + e.getValue())
                    .collect(Collectors.joining("\n"));
            Files.writeString(SAVE_FILE, content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getCount(Achievement achievement) {
        return counts.getOrDefault(achievement, 0);
    }

    public static boolean isUnlocked(Achievement achievement) {
        return getCount(achievement) > 0;
    }

    /** Increments the count and saves. Returns the new count. */
    public static int increment(Achievement achievement) {
        int newCount = getCount(achievement) + 1;
        counts.put(achievement, newCount);
        save();
        return newCount;
    }
}