package ru.friends.customjukebox.data;

import java.util.List;

/**
 * A group of tracks shown together under one header in the GUI.
 * IMPORTANT: two albums can have the exact same artist+title (e.g. several
 * "Без альбома" groups for the same artist) and still be separate entries -
 * order in the list is preserved from tracks.json and each group always
 * gets its own header + divider in the UI, on purpose.
 */
public record Album(String artist, String title, List<Track> tracks) {
}
