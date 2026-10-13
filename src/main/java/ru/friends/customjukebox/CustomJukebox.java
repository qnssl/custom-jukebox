package ru.friends.customjukebox;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.friends.customjukebox.network.PlayRequestPayload;
import ru.friends.customjukebox.network.PlaySyncPayload;
import ru.friends.customjukebox.network.StopRequestPayload;
import ru.friends.customjukebox.network.StopSyncPayload;
import ru.friends.customjukebox.server.JukeboxState;

public class CustomJukebox implements ModInitializer {
    public static final String MOD_ID = "customjukebox";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[CustomJukebox] Initializing...");

        registerPayloads();
        registerBlockInteraction();
        registerServerReceivers();
        registerPlayerJoinSync();
    }

    private void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(PlayRequestPayload.ID, PlayRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StopRequestPayload.ID, StopRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlaySyncPayload.ID, PlaySyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StopSyncPayload.ID, StopSyncPayload.CODEC);
    }

    private void registerBlockInteraction() {
        // We intercept ANY empty-hand right click on a Jukebox block.
        // IMPORTANT: this common handler is registered on BOTH the client
        // and the server, and Fabric's UseBlockCallback stops at the first
        // listener that doesn't return PASS. CustomJukeboxClient registers
        // its own handler (which opens the custom screen) AFTER this one,
        // so on the client we must return PASS here and let that later
        // handler run - otherwise the screen never opens. We only guard
        // against vanilla's "eject record" behaviour on the logical server.
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!player.getStackInHand(hand).isEmpty()) return ActionResult.PASS;

            var state = world.getBlockState(hitResult.getBlockPos());
            if (!state.isOf(Blocks.JUKEBOX)) return ActionResult.PASS;

            if (world.isClient) {
                // Let the client-side handler (which opens the GUI) run.
                return ActionResult.PASS;
            } else {
                // Logical server: nothing to do besides preventing vanilla eject.
                return ActionResult.SUCCESS;
            }
        });
    }

    private void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(PlayRequestPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                World world = player.getWorld();
                long startMillis = System.currentTimeMillis();

                JukeboxState.set(world, payload.pos(), payload.trackId(), startMillis);

                PlaySyncPayload sync = new PlaySyncPayload(payload.pos(), payload.trackId(), startMillis);
                for (ServerPlayerEntity p : PlayerLookup.world((net.minecraft.server.world.ServerWorld) world)) {
                    ServerPlayNetworking.send(p, sync);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(StopRequestPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                World world = player.getWorld();

                JukeboxState.clear(world, payload.pos());

                StopSyncPayload sync = new StopSyncPayload(payload.pos());
                for (ServerPlayerEntity p : PlayerLookup.world((net.minecraft.server.world.ServerWorld) world)) {
                    ServerPlayNetworking.send(p, sync);
                }
            });
        });
    }

    private void registerPlayerJoinSync() {
        // When a player joins, tell them about every jukebox that is
        // currently "playing" in their world, so their client at least
        // starts the track (from the beginning - see README limitation
        // about true mid-song seeking).
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerEntity player = handler.getPlayer();
            World world = player.getWorld();

            JukeboxState.getAll(world).forEach((pos, info) -> {
                ServerPlayNetworking.send(handler.getPlayer(),
                        new PlaySyncPayload(pos, info.trackId(), info.startMillis()));
            });
        });
    }
}