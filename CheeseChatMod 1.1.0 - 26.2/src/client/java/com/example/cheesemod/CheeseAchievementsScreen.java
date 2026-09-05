package com.example.cheesemod;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CheeseAchievementsScreen extends Screen {

    private final Screen parent;

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
            int nameColor = unlocked ? 0xFFFFD700 : 0xFF808080;
            int descColor = unlocked ? 0xFFE0C060 : 0xFF555555;

            int count = CheeseAchievements.getCount(achievement);
            String nameLine = (unlocked ? "★ [X] " : "[ ] ") + name + (unlocked ? " (x" + count + ")" : "");

            int nameWidth = this.font.width(nameLine);
            int descWidth = this.font.width(desc);

            graphics.text(this.font, nameLine, this.width / 2 - nameWidth / 2, y, nameColor, unlocked);
            graphics.text(this.font, desc, this.width / 2 - descWidth / 2, y + 12, descColor, false);
            y += 34;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}