package ru.friends.customjukebox.data;

import net.minecraft.util.Identifier;

/**
 * One playable song.
 *
 * @param id       unique id, e.g. "disc_014" - also used as the sound
 *                 event path (assets/customjukebox/sounds/tracks/{id}.ogg)
 * @param artist   artist display name
 * @param album    album display name (e.g. "Сингл" / "Без альбома" for
 *                 tracks without a real album, see generate_tracks.py)
 * @param title    track display name
 * @param cover    texture identifier for the cover art
 * @param soundId  identifier of the SoundEvent to play (namespace:track.id)
 */
public record Track(String id, String artist, String album, String title, Identifier cover, Identifier soundId) {
}
