package com.example.addon.hud;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class HudExample extends HudElement {
    public static final HudElementInfo<HudExample> INFO = new HudElementInfo<>(AddonTemplate.HUD_GROUP, "spawner-tick-display", "Displays the spawner's tick countdown.", HudExample::new);

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private int lastKnownSpawnDelay = -1; // Stores the last known spawnDelay

    public HudExample() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        // Call tick manually to update the spawnDelay before rendering
        tick();

        String text = lastKnownSpawnDelay >= 0 ? "Spawner Tick: " + lastKnownSpawnDelay : "Spawner Tick: N/A";

        setSize(renderer.textWidth(text, true), renderer.textHeight(true));

        // Render background
        renderer.quad(x, y, getWidth(), getHeight(), Color.DARK_GRAY);

        // Render text
        renderer.text(text, x, y, Color.WHITE, true);
    }

    // Remove the @Override annotation
    public void tick() {
        if (mc.world != null && mc.player != null) {
            BlockPos playerPos = mc.player.getBlockPos();
            Optional<MobSpawnerBlockEntity> closestSpawner = findClosestSpawner(playerPos, 32);

            closestSpawner.ifPresent(spawner -> {
                lastKnownSpawnDelay = spawner.getLogic().spawnDelay;
            });
        }
    }


    /**
     * Finds the closest spawner within a given range.
     */
    private Optional<MobSpawnerBlockEntity> findClosestSpawner(BlockPos center, int range) {
        MobSpawnerBlockEntity closestSpawner = null;
        double closestDistance = Double.MAX_VALUE;

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos spawnerPos = center.add(x, y, z);
                    if (mc.world.getBlockEntity(spawnerPos) instanceof MobSpawnerBlockEntity spawner) {
                        double distance = spawnerPos.getSquaredDistance(center);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestSpawner = spawner;
                        }
                    }
                }
            }
        }

        return Optional.ofNullable(closestSpawner);
    }
}
