package ml.mypals.persona.screen;

import ml.mypals.persona.network.packets.roster.AddToRosterC2SPayload;
import ml.mypals.persona.items.rosterData.AddCharacterToRosterData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;


public class RosterRecordScreen extends Screen  {

    public static final Identifier BOOK_LOCATION = Identifier.withDefaultNamespace("textures/gui/book.png");
    private final AddCharacterToRosterData characterData;
    private AbstractClientPlayer targetPlayer;
    private EditBox nameBox;

    private int leftPos;
    private int topPos;

    public RosterRecordScreen(AddCharacterToRosterData characterData) {
        super(Component.translatable("persona.roster.add_player"));
        this.characterData = characterData;
    }

    @Override
    protected void init() {
        super.init();

        int imageWidth = 192;
        int imageHeight = 192;

        leftPos = (this.width - imageWidth) / 2 - 4;
        topPos = (this.height - imageHeight) / 2 - 30;

        nameBox = new EditBox(
                this.font,
                leftPos + 36,
                topPos + 120,
                120,
                18,
                Component.translatable("persona.roster.name")
        );
        nameBox.setMaxLength(32);
        this.addRenderableWidget(nameBox);

        Button confirmButton = Button.builder(
                Component.translatable("persona.roster.confirm"),
                b -> onConfirm()
        ).bounds(
                leftPos + 36,
                topPos + 150,
                55,
                20
        ).build();

        Button cancelButton = Button.builder(
                Component.translatable("persona.roster.cancel"),
                b -> onClose()
        ).bounds(
                leftPos + 101,
                topPos + 150,
                55,
                20
        ).build();

        this.addRenderableWidget(confirmButton);
        this.addRenderableWidget(cancelButton);

        this.setInitialFocus(nameBox);

        if (minecraft.level != null) {
            for (AbstractClientPlayer p : minecraft.level.players()) {
                if (p.getStringUUID().equals(characterData.playerId())) {
                    targetPlayer = p;

                    targetPlayer.setCustomNameVisible(true);
                    targetPlayer.setCustomName(Component.literal("???"));
                    break;
                }
            }
        }
    }

    private void onConfirm() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) return;

        String uuid = characterData.playerId();

        ClientPlayNetworking.send(
                new AddToRosterC2SPayload(new AddCharacterToRosterData(uuid, name))
        );

        this.onClose();
    }

    @Override
    public void onClose() {
        targetPlayer.setCustomNameVisible(false);
        this.minecraft.setScreen(null);
    }
    private int backgroundLeft() {
        return (this.width - 192) / 2;
    }

    private int backgroundTop() {
        return 2;
    }
    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderBackground(guiGraphics, i, j, f);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_LOCATION,
                this.backgroundLeft(), this.topPos+10,
                0.0F, 0.0F, 192, 192, 256, 256);

        if (targetPlayer != null) {

            updateFakeMouse();
            targetPlayer.setCustomName(Component.literal(nameBox.getValue()));
            int x = leftPos+30;
            int y = topPos+20;
            int w = x+135;
            int h = y+100;
            int scale = 70;
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics,
                    x,
                    y,
                    w,
                    h,
                    scale,
                    0.7f,
                    fakeMouseX,
                    fakeMouseY,
                    targetPlayer
            );
        }
    }

    private float fakeMouseX = 0;
    private float fakeMouseY = 0;

    private float targetMouseX = 0;
    private float targetMouseY = 0;

    private long lastChangeTime = 0;
    private void updateFakeMouse() {
        long now = System.currentTimeMillis();

        float cx = this.width / 2f;
        float cy = (this.height / 2f + 20) / 2f;

        if (now - lastChangeTime > 3600) {
            lastChangeTime = now;

            if (Math.random() < 0.3 && targetMouseX != cx && targetMouseY != cy) {
                targetMouseX = cx;
                targetMouseY = cy;
            } else {
                double angle = Math.random() * Math.PI * 2;
                double r = (0.6 + Math.random() * 0.4) * 0.35;

                targetMouseX = (float)(cx + Math.cos(angle) * this.width  * r);
                targetMouseY = (float)(cy + Math.sin(angle) * (this.height / 2f + 20) * r);
            }
        }

        fakeMouseX += (targetMouseX - fakeMouseX) * 0.05f;
        fakeMouseY += (targetMouseY - fakeMouseY) * 0.05f;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }
    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        gui.drawCenteredString(
                this.font,
                this.title,
                leftPos + 96,
                topPos + 18,
                0x3F3F3F
        );
        gui.drawString(
                this.font,
                Component.translatable("persona.roster.name"),
                leftPos + 36,
                topPos + 75,
                0x000000,
                false
        );

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
