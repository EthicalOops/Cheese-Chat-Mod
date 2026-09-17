package com.example.cheesemod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Persisted settings for Cheese Mod: which chat channels !cheese should
 * respond in. Saved to config/cheesemod.properties so it survives restarts.
 */
public class CheeseConfig {

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("cheesemod.properties");

    // All off by default - the user opts in to each channel via /cheese.
    public static boolean guildEnabled = true;
    public static boolean partyEnabled = true;
    public static boolean coopEnabled = true;

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            props.load(in);
            guildEnabled = Boolean.parseBoolean(props.getProperty("guildEnabled", "false"));
            partyEnabled = Boolean.parseBoolean(props.getProperty("partyEnabled", "false"));
            coopEnabled = Boolean.parseBoolean(props.getProperty("coopEnabled", "false"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        Properties props = new Properties();
        props.setProperty("guildEnabled", String.valueOf(guildEnabled));
        props.setProperty("partyEnabled", String.valueOf(partyEnabled));
        props.setProperty("coopEnabled", String.valueOf(coopEnabled));
        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            props.store(out, "Cheese Mod settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
