package ru.friends.customjukebox.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import ru.friends.customjukebox.data.Album;
import ru.friends.customjukebox.data.Track;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * NOTE ON YARN MAPPINGS (1.21.1): extends AlwaysSelectedEntryListWidget
 * (not the bare EntryListWidget) because ClickableWidget requires
 * appendClickableNarrations(NarrationMessageBuilder) to be implemented
 * somewhere in the hierarchy, and AlwaysSelectedEntryListWidget already
 * provides it (delegating to the selected entry's getNarration()), the
 * same way vanilla's world-select / resource-pack lists do it.
 */
public class TrackListWidget extends AlwaysSelectedEntryListWidget<TrackListWidget.Entry> {
    private static final int ITEM_HEIGHT = 26;
    private static final int COVER_SIZE = 20;

    private final BlockPos jukeboxPos;
    private final BiConsumer<BlockPos, String> onTrackSelected;

    public TrackListWidget(MinecraftClient client, int width, int height, int top,
                           BlockPos jukeboxPos, BiConsumer<BlockPos, String> onTrackSelected) {
        super(client, width, height, top, ITEM_HEIGHT);
        this.jukeboxPos = jukeboxPos;
        this.onTrackSelected = onTrackSelected;
        setRenderHeader(false, 0);
    }

    public void setAlbums(List<Album> albums) {
        clearEntries();
        for (Album album : albums) {
            addEntry(new HeaderEntry(album.artist(), album.title()));
            for (Track track : album.tracks()) {
                addEntry(new TrackEntry(track));
            }
        }
    }

    @Override
    public int getRowWidth() {
        return this.width - 12;
    }

    @Override
    protected int getScrollbarX() {
        return this.getX() + this.width - 6;
    }

    public abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
    }

    /** Non-clickable divider showing "Artist — Album". */
    public class HeaderEntry extends Entry {
        private final String artist;
        private final String album;

        public HeaderEntry(String artist, String album) {
            this.artist = artist;
            this.album = album;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float tickDelta) {
            // thin divider line above the header, dark theme
            context.fill(x, y, x + entryWidth, y + 1, 0x33FFFFFF);

            Text label = Text.literal(artist + " — " + album).formatted(net.minecraft.util.Formatting.GRAY);
            context.drawText(MinecraftClient.getInstance().textRenderer, label, x + 2, y + 10, 0xFFB0B0B0, false);
        }

        @Override
        public Text getNarration() {
            return Text.literal(artist + " " + album);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }
    }

    /** Clickable single track row: cover art + title. */
    public class TrackEntry extends Entry {
        private final Track track;

        public TrackEntry(Track track) {
            this.track = track;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (hovered) {
                context.fill(x, y, x + entryWidth, y + entryHeight, 0x40FFFFFF);
            }

            context.drawTexture(track.cover(), x + 2, y + 2, 0, 0, COVER_SIZE, COVER_SIZE, COVER_SIZE, COVER_SIZE);

            Text title = Text.literal(track.title()).formatted(net.minecraft.util.Formatting.WHITE);
            context.drawText(MinecraftClient.getInstance().textRenderer, title,
                    x + COVER_SIZE + 8, y + (entryHeight - 8) / 2, 0xFFFFFFFF, false);
        }

        @Override
        public Text getNarration() {
            return Text.literal(track.artist() + " - " + track.title());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                onTrackSelected.accept(jukeboxPos, track.id());
                return true;
            }
            return false;
        }
    }
}