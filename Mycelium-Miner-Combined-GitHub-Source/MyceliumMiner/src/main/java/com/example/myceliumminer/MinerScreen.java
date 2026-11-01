package com.example.myceliumminer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class MinerScreen extends Screen {
    private enum Page { HUB, MYCELIUM, STONE, SETTINGS, HELP }
    private Page page = Page.HUB;
    private EditBox x, y, z;
    private String status = "Choose a page.";
    public MinerScreen() { super(Component.literal("Mycelium Miner HUB")); }

    private void page(Page next) { page = next; clearWidgets(); init(); }
    private void button(String title, int left, int top, int width, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(title), b -> action.run()).bounds(left, top, width, 20).build());
    }
    @Override protected void init() {
        int cx = width / 2;
        if (page == Page.HUB) {
            button("Mycelium route", cx - 100, 55, 200, () -> page(Page.MYCELIUM));
            button("Stone generator", cx - 100, 82, 200, () -> page(Page.STONE));
            button("Settings", cx - 100, 109, 200, () -> page(Page.SETTINGS));
            button("Controls / Help", cx - 100, 136, 200, () -> page(Page.HELP));
            button("Stop all macros", cx - 100, 163, 200, () -> { MyceliumMinerClient.stop(Minecraft.getInstance()); status = "Stopped."; });
        } else if (page == Page.MYCELIUM || page == Page.STONE) {
            x = field(cx - 104, 61, "X"); y = field(cx - 31, 61, "Y"); z = field(cx + 42, 61, "Z");
            if (page == Page.MYCELIUM) {
                button("Add route point", cx - 155, 91, 150, () -> add(0));
                button("Add jump point", cx + 5, 91, 150, () -> add(1));
                button("Add mining target", cx - 155, 117, 150, () -> add(2));
                button("Record route ON/OFF", cx + 5, 117, 150, () -> { MyceliumMinerClient.recording = !MyceliumMinerClient.recording; status = "Recording: " + MyceliumMinerClient.recording; });
                button("Clear route", cx - 155, 143, 150, () -> { MyceliumMinerClient.ROUTE.clear(); MyceliumMinerClient.resetRouteProgress(); status = "Route cleared"; });
                button("Clear jumps + targets", cx + 5, 143, 150, () -> { MyceliumMinerClient.JUMP_POINTS.clear(); MyceliumMinerClient.MINE_POINTS.clear(); status = "Cleared jumps and targets"; });
                button("START MYCELIUM", cx - 155, 169, 150, () -> { MyceliumMinerClient.start(MyceliumMinerClient.Mode.MYCELIUM, Minecraft.getInstance()); onClose(); });
                button("STOP", cx + 5, 169, 150, () -> { MyceliumMinerClient.stop(Minecraft.getInstance()); status = "Stopped"; });
            } else {
                button("Add selected stone block", cx - 155, 91, 310, () -> add(3));
                button("Remove last block", cx - 155, 117, 150, () -> { if (!MyceliumMinerClient.STONE_BLOCKS.isEmpty()) MyceliumMinerClient.STONE_BLOCKS.remove(MyceliumMinerClient.STONE_BLOCKS.size()-1); status = "Removed last block"; });
                button("Clear stone selection", cx + 5, 117, 150, () -> { MyceliumMinerClient.STONE_BLOCKS.clear(); status = "Selection cleared"; });
                button("START STONE", cx - 155, 143, 150, () -> { MyceliumMinerClient.start(MyceliumMinerClient.Mode.STONE, Minecraft.getInstance()); onClose(); });
                button("STOP", cx + 5, 143, 150, () -> { MyceliumMinerClient.stop(Minecraft.getInstance()); status = "Stopped"; });
            }
        } else if (page == Page.SETTINGS) {
            button("Slower turns", cx - 100, 75, 95, () -> MyceliumMinerClient.turnSpeed = Math.max(1, MyceliumMinerClient.turnSpeed - 1));
            button("Faster turns", cx + 5, 75, 95, () -> MyceliumMinerClient.turnSpeed = Math.min(15, MyceliumMinerClient.turnSpeed + 1));
        }
        if (page != Page.HUB) button("Back to HUB", cx - 105, height - 53, 100, () -> page(Page.HUB));
        button("Done", cx + 5, height - 53, 100, this::onClose);
    }
    private EditBox field(int left, int top, String hint) {
        EditBox f = new EditBox(font, left, top, 62, 20, Component.literal(hint));
        f.setHint(Component.literal(hint)); f.setMaxLength(10); addRenderableWidget(f); return f;
    }
    private void add(int kind) {
        try {
            int a = Integer.parseInt(x.getValue().trim()), b = Integer.parseInt(y.getValue().trim()), c = Integer.parseInt(z.getValue().trim());
            BlockPos pos = new BlockPos(a, b, c);
            if (kind == 0) MyceliumMinerClient.ROUTE.add(pos.getCenter());
            else if (kind == 1) MyceliumMinerClient.JUMP_POINTS.add(pos);
            else if (kind == 2) MyceliumMinerClient.MINE_POINTS.add(pos);
            else if (!MyceliumMinerClient.STONE_BLOCKS.contains(pos)) MyceliumMinerClient.STONE_BLOCKS.add(pos);
            status = "Added: " + a + ", " + b + ", " + c;
        } catch (NumberFormatException e) { status = "Enter whole-number X, Y and Z"; }
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int cx = width / 2;
        g.drawCenteredString(font, "MYCELIUM MINER | " + page, cx, 20, 0xFFFFFF);
        if (page == Page.MYCELIUM || page == Page.STONE) g.drawCenteredString(font, "Enter block X / Y / Z", cx, 43, 0xAAAAAA);
        if (page == Page.MYCELIUM) {
            g.drawCenteredString(font, "Route " + MyceliumMinerClient.ROUTE.size() + " | Jumps " + MyceliumMinerClient.JUMP_POINTS.size() + " | Targets " + MyceliumMinerClient.MINE_POINTS.size(), cx, 201, 0xFFFFFF);
        } else if (page == Page.STONE) {
            g.drawCenteredString(font, "Selected blocks: " + MyceliumMinerClient.STONE_BLOCKS.size(), cx, 181, 0xFFFFFF);
            g.drawCenteredString(font, "Select each block of your concave generator", cx, 198, 0xAAAAAA);
        } else if (page == Page.SETTINGS) {
            g.drawCenteredString(font, "Turning speed: " + MyceliumMinerClient.turnSpeed + " degrees/tick", cx, 113, 0xFFFFFF);
        } else if (page == Page.HELP) {
            g.drawCenteredString(font, "O = HUB  |  F6 = toggle mycelium  |  F7 = record", cx, 65, 0xFFFFFF);
            g.drawCenteredString(font, "Stone: add the coordinates of every block to mine", cx, 85, 0xFFFFFF);
            g.drawCenteredString(font, "Stand within 4 blocks; targets must be visible", cx, 105, 0xFFFFFF);
            g.drawCenteredString(font, "Macros run only with menus closed", cx, 125, 0xFFFFFF);
        }
        g.drawCenteredString(font, "Mode: " + MyceliumMinerClient.mode + " | " + status, cx, height - 75, 0xFFFF55);
        g.drawCenteredString(font, "Single-player prototype. Test in a backup world.", cx, height - 16, 0xFFAA55);
    }
    @Override public boolean isPauseScreen() { return false; }
}
