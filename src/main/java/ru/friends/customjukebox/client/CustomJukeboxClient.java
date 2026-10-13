package ru.friends.customjukebox.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import ru.friends.customjukebox.CustomJukebox;
import ru.friends.customjukebox.client.audio.ClientJukeboxAudio;
import ru.friends.customjukebox.client.gui.JukeboxScreen;
import ru.friends.customjukebox.data.TrackLibrary;
import ru.friends.customjukebox.network.PlaySyncPayload;
import ru.friends.customjukebox.network.StopSyncPayload;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


public class CustomJukeboxClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        registerTrackLibraryReload();
        registerBlockClickHandler();
        registerSyncReceivers();
    }

    private void registerTrackLibraryReload() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager,
                                                    Profiler prepareProfiler, Profiler applyProfiler,
                                                    Executor prepareExecutor, Executor applyExecutor) {
                return CompletableFuture.runAsync(() -> TrackLibrary.reload(manager), prepareExecutor)
                        .thenCompose(synchronizer::whenPrepared);
            }

            @Override
            public Identifier getFabricId() {
                return Identifier.of(CustomJukebox.MOD_ID, "track_library");
            }
        });
    }

    private void registerBlockClickHandler() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!player.getStackInHand(hand).isEmpty()) return ActionResult.PASS;

            var state = world.getBlockState(hitResult.getBlockPos());
            if (!state.isOf(Blocks.JUKEBOX)) return ActionResult.PASS;

            MinecraftClient.getInstance().setScreen(new JukeboxScreen(hitResult.getBlockPos().toImmutable()));
            return ActionResult.SUCCESS;
        });
    }

    private void registerSyncReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(PlaySyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                long elapsed = System.currentTimeMillis() - payload.startMillis();
                ClientJukeboxAudio.play(payload.pos(), payload.trackId(), Math.max(elapsed, 0));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(StopSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> ClientJukeboxAudio.stop(payload.pos()));
        });
    }
}
