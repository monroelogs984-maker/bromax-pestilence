package com.bromax.pestilence.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Dev-only screenshot driver for checking worn curios: with -Dpestilence.preview set, once in a
 * world it parks a camera in front of the player, turns the player through four views, shoots
 * each (run/screenshots/curios_<yaw>.png) and quits.
 */
final class DevPreview {
    private static final int[] YAWS = {0, 90, 180, 270};
    private static int ticks;
    private static ArmorStand camera;

    private DevPreview() {}

    static void enableIfRequested() {
        if (System.getProperty("pestilence.preview") != null) NeoForge.EVENT_BUS.addListener(DevPreview::tick);
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ticks++;
        if (ticks < 100) return;
        if (camera == null) {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            camera = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            camera.moveTo(mc.player.getX(), mc.player.getY() + 1.15 - camera.getEyeHeight(), mc.player.getZ() + 2.2, 180f, 4f);
            camera.setYHeadRot(180f);
            mc.setCameraEntity(camera);
        }
        int shot = (ticks - 100) / 40;
        if (shot >= YAWS.length) {
            mc.stop();
            return;
        }
        float yaw = YAWS[shot];
        mc.player.setYRot(yaw);
        mc.player.setXRot(0);
        mc.player.setYHeadRot(yaw);
        mc.player.setYBodyRot(yaw);
        mc.player.yRotO = yaw;
        mc.player.yBodyRotO = yaw;
        mc.player.yHeadRotO = yaw;
        if ((ticks - 100) % 40 == 30) Screenshot.grab(mc.gameDirectory, "curios_" + (int) yaw + ".png", mc.getMainRenderTarget(), msg -> {});
    }
}
