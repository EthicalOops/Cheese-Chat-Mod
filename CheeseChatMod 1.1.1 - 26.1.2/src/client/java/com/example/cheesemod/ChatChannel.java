package com.example.cheesemod;

/**
 * A Hypixel chat channel cheesemod can watch and reply in.
 *
 * To add a new channel later: add one line here with its display prefix
 * (as it appears in chat, e.g. "Guild") and the command used to send a
 * message into it (without the leading slash) - everything else (the GUI
 * toggle, detection, and sending) is wired up generically from this list.
 */
public enum ChatChannel {

    GUILD("Guild", "gc"),
    PARTY("Party", "pc"),
    COOP("Co-op", "cc");

    public final String label;
    public final String commandPrefix;

    ChatChannel(String label, String commandPrefix) {
        this.label = label;
        this.commandPrefix = commandPrefix;
    }

    public boolean isEnabled() {
        return switch (this) {
            case GUILD -> CheeseConfig.guildEnabled;
            case PARTY -> CheeseConfig.partyEnabled;
            case COOP -> CheeseConfig.coopEnabled;
        };
    }

    public void setEnabled(boolean enabled) {
        switch (this) {
            case GUILD -> CheeseConfig.guildEnabled = enabled;
            case PARTY -> CheeseConfig.partyEnabled = enabled;
            case COOP -> CheeseConfig.coopEnabled = enabled;
        }
    }
}
