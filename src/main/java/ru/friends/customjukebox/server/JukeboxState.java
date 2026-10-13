package ru.friends.customjukebox.server;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track, per world, of which jukeboxes currently "have a track
 * playing" and since when. Purely in-memory - it resets on server
 * restart. That's an accepted, documented limitation (see README);
 * persisting it via a PersistentState is a natural future improvement.
 */
public final class JukeboxState {
    public record PlayingInfo(String trackId, long startMillis) {}

    private static final Map<World, Map<BlockPos, PlayingInfo>> STATE = new ConcurrentHashMap<>();

    private JukeboxState() {}

    public static void set(World world, BlockPos pos, String trackId, long startMillis) {
        STATE.computeIfAbsent(world, w -> new ConcurrentHashMap<>())
                .put(pos.toImmutable(), new PlayingInfo(trackId, startMillis));
    }

    public static void clear(World world, BlockPos pos) {
        Map<BlockPos, PlayingInfo> worldState = STATE.get(world);
        if (worldState != null) {
            worldState.remove(pos.toImmutable());
        }
    }

    public static Map<BlockPos, PlayingInfo> getAll(World world) {
        return Collections.unmodifiableMap(
                STATE.getOrDefault(world, Collections.emptyMap()));
    }
}
