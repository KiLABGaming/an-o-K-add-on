package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class Ghostify extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> renderOriginal = sgGeneral.add(new BoolSetting.Builder()
        .name("render-original")
        .description("Renders your player model at the original position.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> logSpawnerStatus = sgGeneral.add(new BoolSetting.Builder()
        .name("log-spawner-status")
        .description("Logs the spawner's spawnDelay.")
        .defaultValue(true)
        .build()
    );

    private FakePlayerEntity fakePlayer;
    private final List<PlayerMoveC2SPacket> packets = new ArrayList<>();
    private boolean frozen = false;
    private BlockPos trackedSpawnerPos = null;
    private int spawnDelay = -1;

    public Ghostify() {
        super(AddonTemplate.CATEGORY, "Ghostify", "Freezes movement until the spawner's spawnDelay is 20 ticks.");
    }

    @Override
    public void onActivate() {
        ChatUtils.info("§aSpawnerBlinkFreeze activated. Freezing movement.");
        if (renderOriginal.get()) {
            fakePlayer = new FakePlayerEntity(mc.player, mc.player.getGameProfile().getName(), 20, true);
            fakePlayer.doNotPush = true;
            fakePlayer.hideWhenInsideCamera = true;
            fakePlayer.spawn();
            ChatUtils.info("§aFake player model spawned.");
        }
        frozen = true;
        trackedSpawnerPos = null;
        spawnDelay = -1;
    }

    @Override
    public void onDeactivate() {
        sendStoredPackets();
        if (fakePlayer != null) {
            fakePlayer.despawn();
            fakePlayer = null;
            ChatUtils.info("§cFake player model despawned.");
        }
        resetState();
        ChatUtils.info("§cSpawnerBlinkFreeze deactivated. Movement unfrozen.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;

        if (trackedSpawnerPos != null) {
            if (mc.world.getBlockEntity(trackedSpawnerPos) instanceof MobSpawnerBlockEntity spawner) {
                spawnDelay = spawner.getLogic().spawnDelay;

                // Log the current spawnDelay
                if (logSpawnerStatus.get()) {
                    ChatUtils.info("§6Current spawnDelay: " + spawnDelay);
                }

                // Release packets and deactivate module when spawnDelay is exactly 20
                if (spawnDelay == 20) {
                    ChatUtils.info("§aSpawnDelay is 20. Movement unlocked.");
                    sendStoredPackets();
                    this.toggle(); // Deactivate the module
                }
            }
        } else {
            // Search for spawners within 16x16 blocks and capture only if spawnDelay > 0
            BlockPos playerPos = mc.player.getBlockPos();
            for (int x = -16; x <= 16; x++) {
                for (int y = -16; y <= 16; y++) {
                    for (int z = -16; z <= 16; z++) {
                        BlockPos spawnerPos = playerPos.add(x, y, z);
                        if (mc.world.getBlockEntity(spawnerPos) instanceof MobSpawnerBlockEntity spawner) {
                            if (spawner.getLogic().spawnDelay > 0) {
                                trackedSpawnerPos = spawnerPos;
                                spawnDelay = spawner.getLogic().spawnDelay;
                                ChatUtils.info("§6Spawner detected at " + spawnerPos + " with spawnDelay: " + spawnDelay);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void sendStoredPackets() {
        synchronized (packets) {
            if (!packets.isEmpty()) {
                List<PlayerMoveC2SPacket> packetsCopy = new ArrayList<>(packets);
                packets.clear();
                packetsCopy.forEach(mc.player.networkHandler::sendPacket);
                ChatUtils.info("§aSent all stored movement packets.");
            }
            frozen = false;
        }
    }

    private void resetState() {
        trackedSpawnerPos = null;
        spawnDelay = -1;
        frozen = false;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (frozen && event.packet instanceof PlayerMoveC2SPacket) {
            synchronized (packets) {
                event.cancel();
                packets.add((PlayerMoveC2SPacket) event.packet);
                ChatUtils.info("§eMovement packet blocked and stored.");
            }
        }
    }
}
