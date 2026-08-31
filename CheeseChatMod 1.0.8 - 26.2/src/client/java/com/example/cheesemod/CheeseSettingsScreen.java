package com.example.cheesemod;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Settings screen opened by typing /cheese. Lets the player toggle which
 * Hypixel chat channels the !cheese trigger should respond in.
 */
public class CheeseSettingsScreen extends Screen {

    public CheeseSettingsScreen() {
        super(Component.literal("Cheese Mod Settings"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;

        ChatChannel[] channels = ChatChannel.values();
        for (int i = 0; i < channels.length; i++) {
            ChatChannel channel = channels[i];
            int y = startY + (i * 24);
            Button button = Button.builder(label(channel), btn -> {
                channel.setEnabled(!channel.isEnabled());
                CheeseConfig.save();
                btn.setMessage(label(channel));
            }).bounds(centerX - 100, y, 200, 20).build();
            this.addRenderableWidget(button);
        }

        Button doneButton = Button.builder(Component.literal("Done"), btn -> this.onClose())
                .bounds(centerX - 100, startY + (channels.length * 24) + 12, 200, 20).build();
        this.addRenderableWidget(doneButton);
    }

    private Component label(ChatChannel channel) {
        return Component.literal(channel.label + " Chat: " + (channel.isEnabled() ? "ON" : "OFF"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(this.font, this.title.getString(), this.width / 2 - this.font.width(this.title) / 2,
                this.height / 2 - 60, 0xFFFFFF, true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
