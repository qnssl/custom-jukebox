package ru.friends.customjukebox.client.audio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import ru.friends.customjukebox.CustomJukebox;
import ru.friends.customjukebox.data.Track;
import ru.friends.customjukebox.data.TrackLibrary;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Client-only. Owns the currently-playing SoundInstance for every jukebox
 * position we know about, so we can stop the old one before starting a
 * new one (or when a StopSyncPayload arrives).
 *
 * KNOWN LIMITATION: Minecraft's sound engine has no simple public API to
 * start a sound already-in-progress at an arbitrary offset ("seek").
 * When a player joins mid-song, we currently just start the track from
 * the beginning instead of jumping to where the other players are. If you
 * want true seek-sync for latecomers, look at wrapping a custom
 * AudioStream and manipulating the OpenAL source position directly - it's
 * doable but is a separate, more advanced piece of work.
 */
public final class ClientJukeboxAudio {
    private static final Map<BlockPos, SoundInstance> PLAYING = new HashMap<>();

    private ClientJukeboxAudio() {}

    public static void play(BlockPos pos, String trackId, long elapsedMillis) {
        stop(pos);

        // Defensive cleanup: Minecraft's sound engine has a limited pool of
        // concurrent STREAMING channels (our tracks are long OGGs, so they
        // stream rather than fully preload). If a previous play/stop raced
        // with the async decode of an older request, that old streaming
        // source can be orphaned - still holding a channel, but no longer
        // tracked in our PLAYING map, so our normal stop(pos) above never
        // reaches it. Over a long test session those orphans pile up until
        // no channels are left and NEW tracks silently fail to start.
        // Force-stopping the whole RECORDS category (which is what
        // PositionedSoundInstance.record() uses) before every new play
        // sweeps up any such orphans regardless of how they got stranded.
        MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.RECORDS);
        PLAYING.clear();

        Optional<Track> trackOpt = TrackLibrary.findTrack(trackId);
        if (trackOpt.isEmpty()) {
            CustomJukebox.LOGGER.warn("[CustomJukebox] Unknown trackId '{}' - is your resources/textures pack out of date?", trackId);
            return;
        }
        Track track = trackOpt.get();

        SoundEvent event = SoundEvent.of(track.soundId());
        PositionedSoundInstance instance = PositionedSoundInstance.record(event, net.minecraft.util.math.Vec3d.ofCenter(pos));

        MinecraftClient.getInstance().getSoundManager().play(instance);
        PLAYING.put(pos.toImmutable(), instance);

        if (elapsedMillis > 1500) {
            CustomJukebox.LOGGER.info("[CustomJukebox] Joined a track already {} ms in progress - playing from the start (see README limitation).", elapsedMillis);
        }
    }

    public static void stop(BlockPos pos) {
        SoundInstance existing = PLAYING.remove(pos.toImmutable());
        if (existing != null) {
            MinecraftClient.getInstance().getSoundManager().stop(existing);
        }
    }

    public static void stopAll() {
        PLAYING.values().forEach(MinecraftClient.getInstance().getSoundManager()::stop);
        PLAYING.clear();
    }
}
