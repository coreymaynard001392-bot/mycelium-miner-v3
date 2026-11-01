package com.example.myceliumminer;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

public class MyceliumMinerClient implements ClientModInitializer {
    public enum Mode { OFF, MYCELIUM, STONE }
    public static final List<Vec3> ROUTE = new ArrayList<>();
    public static final List<BlockPos> JUMP_POINTS = new ArrayList<>();
    public static final List<BlockPos> MINE_POINTS = new ArrayList<>();
    // Add only the blocks you want mined. An arbitrary concave outline is supported.
    public static final List<BlockPos> STONE_BLOCKS = new ArrayList<>();
    public static Mode mode = Mode.OFF;
    public static boolean recording = false;
    public static float turnSpeed = 4.0f;
    private static int routeIndex, mineIndex, stoneIndex, recordCooldown, jumpCooldown;
    private static Vec3 lastRecorded;
    private static BlockPos lastJumpPoint;
    private static KeyMapping openKey, toggleKey, recordKey;
    private static boolean ownedForward, ownedJump, ownedAttack;

    @Override public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("myceliumminer", "controls"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.myceliumminer.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, category));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.myceliumminer.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F6, category));
        recordKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.myceliumminer.record", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, category));
        ClientTickEvents.END_CLIENT_TICK.register(MyceliumMinerClient::tick);
    }

    public static void stop(Minecraft client) { mode = Mode.OFF; release(client); }
    public static void start(Mode next, Minecraft client) {
        release(client);
        mode = next;
        routeIndex = mineIndex = stoneIndex = 0;
        jumpCooldown = 0;
        lastJumpPoint = null;
    }
    public static void resetRouteProgress() { routeIndex = mineIndex = 0; jumpCooldown = 0; lastJumpPoint = null; }

    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) { mode = Mode.OFF; release(mc); return; }
        while (openKey.consumeClick()) { stop(mc); mc.setScreen(new MinerScreen()); }
        while (toggleKey.consumeClick()) {
            if (mode == Mode.OFF) start(Mode.MYCELIUM, mc); else stop(mc);
            mc.player.displayClientMessage(Component.literal("Macro: " + mode), true);
        }
        while (recordKey.consumeClick()) {
            recording = !recording;
            lastRecorded = null;
            mc.player.displayClientMessage(Component.literal(recording ? "Recording route" : "Route recording stopped"), true);
        }
        if (recording && mc.screen == null && recordCooldown-- <= 0) {
            recordCooldown = 10;
            Vec3 here = mc.player.position();
            if (lastRecorded == null || here.distanceTo(lastRecorded) > 1.25) {
                ROUTE.add(here); lastRecorded = here;
            }
        }
        if (mc.screen != null || mode == Mode.OFF) { release(mc); return; }
        if (mode == Mode.STONE) {
            releaseForwardAndJump(mc);
            mineList(mc, STONE_BLOCKS, true);
            return;
        }
        if (ROUTE.isEmpty()) { stop(mc); mc.player.displayClientMessage(Component.literal("Record or add route points first"), true); return; }
        // Mining takes priority over walking, preventing the two aiming controllers fighting.
        if (mineList(mc, MINE_POINTS, false)) { releaseForwardAndJump(mc); return; }
        releaseAttack(mc);
        Vec3 here = mc.player.position();
        Vec3 dest = ROUTE.get(routeIndex % ROUTE.size());
        double dx = dest.x - here.x, dz = dest.z - here.z;
        double distance = Math.hypot(dx, dz);
        if (distance < 0.55) { routeIndex = (routeIndex + 1) % ROUTE.size(); setForward(mc, false); }
        else {
            float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90);
            turn(mc, yaw, mc.player.getXRot());
            setForward(mc, Math.abs(Mth.wrapDegrees(yaw - mc.player.getYRot())) < 25);
        }
        if (jumpCooldown > 0) jumpCooldown--;
        setJump(mc, false);
        if (lastJumpPoint != null && here.distanceTo(Vec3.atCenterOf(lastJumpPoint)) > 2.0) lastJumpPoint = null;
        if (jumpCooldown == 0 && lastJumpPoint == null) {
            for (BlockPos p : JUMP_POINTS) {
                if (here.distanceTo(Vec3.atCenterOf(p)) < 1.3) {
                    setJump(mc, true); jumpCooldown = 8; lastJumpPoint = p; break;
                }
            }
        }
    }

    // Returns true when the route macro should stop walking to mine a nearby target.
    private static boolean mineList(Minecraft mc, List<BlockPos> blocks, boolean generator) {
        if (blocks.isEmpty()) { releaseAttack(mc); return false; }
        int index = generator ? stoneIndex : mineIndex;
        if (index >= blocks.size()) index = 0;
        // In generator mode, continue cycling, including positions that regenerate.
        // In route mode, skip air targets and continue walking.
        for (int tries = 0; tries < blocks.size(); tries++) {
            BlockPos p = blocks.get(index);
            if (!mc.level.getBlockState(p).isAir()) break;
            index = (index + 1) % blocks.size();
        }
        if (generator) stoneIndex = index; else mineIndex = index;
        BlockPos target = blocks.get(index);
        if (mc.level.getBlockState(target).isAir()) { releaseAttack(mc); return generator; }
        Vec3 aim = Vec3.atCenterOf(target).subtract(mc.player.getEyePosition());
        if (aim.length() > 4.4) { releaseAttack(mc); return generator; }
        float yaw = (float)(Math.toDegrees(Math.atan2(aim.z, aim.x)) - 90);
        float pitch = (float)-Math.toDegrees(Math.atan2(aim.y, Math.hypot(aim.x, aim.z)));
        turn(mc, yaw, pitch);
        boolean aligned = Math.abs(Mth.wrapDegrees(yaw - mc.player.getYRot())) < 3.0
                && Math.abs(pitch - mc.player.getXRot()) < 3.0;
        boolean onBlock = mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(target);
        setAttack(mc, aligned && onBlock);
        // After the target is mined, the next tick skips air blocks.
        return true;
    }

    private static void turn(Minecraft mc, float yaw, float pitch) {
        float speed = Mth.clamp(turnSpeed, 1f, 15f);
        float deltaYaw = Mth.wrapDegrees(yaw - mc.player.getYRot());
        mc.player.setYRot(mc.player.getYRot() + Mth.clamp(deltaYaw, -speed, speed));
        mc.player.setYHeadRot(mc.player.getYRot());
        mc.player.setXRot(Mth.clamp(mc.player.getXRot() + Mth.clamp(pitch - mc.player.getXRot(), -speed, speed), -90, 90));
    }

    // Only override keys while the macro runs. When stopping, restore current physical input.
    private static void setForward(Minecraft mc, boolean value) { mc.options.keyUp.setDown(value); ownedForward = true; }
    private static void setJump(Minecraft mc, boolean value) { mc.options.keyJump.setDown(value); ownedJump = true; }
    private static void setAttack(Minecraft mc, boolean value) { mc.options.keyAttack.setDown(value); ownedAttack = true; }
    private static void releaseForwardAndJump(Minecraft mc) {
        if (mc.options == null) return;
        if (ownedForward) { restore(mc, mc.options.keyUp); ownedForward = false; }
        if (ownedJump) { restore(mc, mc.options.keyJump); ownedJump = false; }
    }
    private static void releaseAttack(Minecraft mc) {
        if (mc.options != null && ownedAttack) { restore(mc, mc.options.keyAttack); ownedAttack = false; }
    }
    private static void release(Minecraft mc) { releaseForwardAndJump(mc); releaseAttack(mc); }
    private static void restore(Minecraft mc, KeyMapping key) {
        // Re-synchronise from Minecraft's bound key, not hard-coded W/Space/mouse.
        if (mc.getWindow() != null && key.getKey().getType() == InputConstants.Type.KEYSYM) {
            key.setDown(InputConstants.isKeyDown(mc.getWindow(), key.getKey().getValue()));
        } else { key.setDown(false); }
    }
}
