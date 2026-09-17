package com.example.cheesemod;

import com.example.cheesemod.CheeseAchievements;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CheeseAchievementsScreen extends Screen {

    private final Screen parent;

    private int getAchievementColor(CheeseAchievements.Achievement achievement) {
    return switch (achievement) {
        case COMMON_CHEESE -> 0xFF5555FF;      // blue
        case UNCOMMON_CHEESE -> 0xFF0000AA;    // dark blue
        case RARE_CHEESE -> 0xFFAA00AA;        // purple
        case EPIC_CHEESE -> 0xFFFF55FF;        // hot pink / light purple
        case LEGENDARY_CHEESE -> 0xFFFF5555;   // red
        case DIVINE_CHEESE -> 0xFF00FFFF;      // cyan
        };
    }

    public CheeseAchievementsScreen(Screen parent) {
        super(Component.literal("Cheese Achievements"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int backWidth = 100;
        this.addRenderableWidget(
                Button.builder(Component.literal("Back"), button -> this.minecraft.gui.setScreen(parent))
                        .bounds((this.width - backWidth) / 2, this.height - 30, backWidth, 20)
                        .build()
        );
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.text(this.font, this.title.getString(),
                this.width / 2 - this.font.width(this.title) / 2, 20, 0xFFFFFFFF, true);

        int y = 50;
        for (CheeseAchievements.Achievement achievement : CheeseAchievements.Achievement.values()) {
            boolean unlocked = CheeseAchievements.isUnlocked(achievement);

            String name = (unlocked || !achievement.hiddenUntilUnlocked) ? achievement.title : "???";
            String desc = unlocked ? achievement.description
                    : (achievement.hiddenUntilUnlocked ? "Hidden achievement" : achievement.description);
            int nameColor = unlocked ? getAchievementColor(achievement) : 0xFF808080;

            int count = CheeseAchievements.getCount(achievement);
            String nameLine = (unlocked ? "★ [X] " : "[ ] ") + name + (unlocked ? " (x" + count + ")" : "");

            int nameWidth = this.font.width(nameLine);
            int descWidth = this.font.width(desc);

            graphics.text(this.font, nameLine, this.width / 2 - nameWidth / 2, y, nameColor, unlocked);
            graphics.text(this.font, desc, this.width / 2 - descWidth / 2, y + 12, nameColor, false);
            y += 34;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}