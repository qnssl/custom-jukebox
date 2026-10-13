package ru.friends.customjukebox.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import ru.friends.customjukebox.data.TrackLibrary;
import ru.friends.customjukebox.network.PlayRequestPayload;
import ru.friends.customjukebox.network.StopRequestPayload;

/**
 * Opened when a player right-clicks a Jukebox with an empty hand.
 * Dark-themed vanilla-style screen: search box on top, scrollable grouped
 * track list below, small "Stop" button.
 */
public class JukeboxScreen extends Screen {
    private final BlockPos jukeboxPos;

    private TextFieldWidget searchBox;
    private TrackListWidget list;

    public JukeboxScreen(BlockPos jukeboxPos) {
        super(Text.translatable("customjukebox.screen.title"));
        this.jukeboxPos = jukeboxPos;
    }

    @Override
    protected void init() {
        int margin = 20;
        int contentWidth = Math.min(360, this.width - margin * 2);
        int left = (this.width - contentWidth) / 2;

        searchBox = new TextFieldWidget(this.textRenderer, left, 24, contentWidth - 70, 20,
                Text.translatable("customjukebox.screen.search"));
        searchBox.setPlaceholder(Text.translatable("customjukebox.screen.search"));
        searchBox.setChangedListener(this::onSearchChanged);
        addSelectableChild(searchBox);
        setInitialFocus(searchBox);

        ButtonWidget stopButton = ButtonWidget.builder(Text.literal("■ Stop"), b -> {
                    ClientPlayNetworking.send(new StopRequestPayload(jukeboxPos));
                    ru.friends.customjukebox.client.audio.ClientJukeboxAudio.stop(jukeboxPos);
                })
                .dimensions(left + contentWidth - 64, 24, 64, 20)
                .build();
        addDrawableChild(stopButton);

        int listTop = 52;
        int listHeight = this.height - listTop - 12;
        list = new TrackListWidget(this.client, contentWidth, listHeight, listTop,
                jukeboxPos, this::onTrackChosen);
        list.setX(left);
        list.setAlbums(TrackLibrary.getAlbums());
        addSelectableChild(list);
    }

    private void onSearchChanged(String query) {
        list.setAlbums(TrackLibrary.search(query));
    }

    private void onTrackChosen(BlockPos pos, String trackId) {
        ClientPlayNetworking.send(new PlayRequestPayload(pos, trackId));
        // We don't play locally here - we wait for the server's PlaySyncPayload
        // broadcast, so that the person who clicked and everyone else start
        // in the same way, from the same code path.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark background, like a semi-opaque vanilla dark overlay
        context.fill(0, 0, this.width, this.height, 0xE0101014);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFFFF);
        searchBox.render(context, mouseX, mouseY, delta);
        list.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
