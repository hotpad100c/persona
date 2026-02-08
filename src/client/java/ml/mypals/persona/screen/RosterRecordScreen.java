package ml.mypals.persona.screen;

import ml.mypals.persona.PersonaClient;
import ml.mypals.persona.characterData.CharacterData;
import ml.mypals.persona.fakePlayer.FakePlayerFactory;
import ml.mypals.persona.items.rosterData.RosterEntry;
import ml.mypals.persona.network.packets.roster.AddToRosterC2SPayload;
import ml.mypals.persona.items.rosterData.AddCharacterToRosterData;
import ml.mypals.persona.network.packets.roster.RosterDeltaSyncC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static ml.mypals.persona.Persona.MOD_ID;


public class RosterRecordScreen extends Screen  {

    private static final Identifier BOOK_LOCATION =
            Identifier.fromNamespaceAndPath(MOD_ID,"textures/gui/roster_page.png");


    private static final Identifier CONFIRM =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/confirm.png");


    private static final Identifier CANCEL =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/cancel.png");

    private static final Identifier DELETE =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/delete.png");

    private static final Identifier DELETE_H =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/delete_h.png");


    private static final Identifier PLAYER_VIEW_DOWNER =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/player_view_downer.png");

    private static final Identifier PLAYER_VIEW_UPPER =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/player_view_upper.png");

    protected static final Button.CreateNarration DEFAULT_NARRATION = (supplier) -> (MutableComponent)supplier.get();

    private static final int PAGE_WIDTH = 140;
    private static final int PAGE_HEIGHT = 163;
    private static final int PAGE_GAP = -6;

    @Nullable private final AddCharacterToRosterData characterData;
    @Nullable private final RosterEntry rosterEntry;
    private AbstractClientPlayer targetPlayer;
    private EditBox nameBox;
    private MultiLineEditBox memoBox;
    private Button confirmButton;
    private Button cancelButton;
    private Button delete;
    private int leftPos;
    private int topPos;

    public RosterRecordScreen(@Nullable AddCharacterToRosterData characterData) {
        super(Component.translatable("persona.roster.add_player"));
        this.characterData = characterData;
        this.rosterEntry = null;
    }

    public RosterRecordScreen(@Nullable RosterEntry rosterEntry) {
        super(Component.literal(""));
        this.rosterEntry = rosterEntry;
        this.characterData = null;
    }

    @Override
    protected void init() {
        super.init();

        int totalWidth = PAGE_WIDTH * 2 + PAGE_GAP;
        leftPos = (this.width - totalWidth) / 2;
        topPos = (this.height - PAGE_HEIGHT) / 2;


        nameBox = new RosterEditBox(
                this.font,
                leftPos + 20,
                topPos + 137,
                110,
                21,
                Component.translatable("persona.roster.name")
        );

        nameBox.setMaxLength(32);

        this.memoBox = new RosterMultiLineEditBox(
                this.font,
                this.width / 2 + 5,
                topPos + 21,
                110,
                134,
                CommonComponents.EMPTY,
                CommonComponents.EMPTY,
                0xFFaa8979,
                false,
                0xFFaa8979,
                false,
                false
        );

        if(rosterEntry != null){
            nameBox.setValue(rosterEntry.getNickname());

            memoBox.setValue(rosterEntry.getNotes());
            memoBox.setFocused(true);
        }

        this.addRenderableWidget(nameBox);
        this.addRenderableWidget(memoBox);


        confirmButton = new Button.Plain(
                leftPos + 160,
                topPos-33,
                28, 41,
                Component.literal(""),
                b->onConfirm(),
                DEFAULT_NARRATION) {
            @Override
            protected void renderContents(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
                guiGraphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        CONFIRM, this.getX(), this.getY()-(this.isActive() && this.isHovered? 2:0),
                        0, 0, this.getWidth(), this.getHeight(),
                        this.getWidth(), this.getHeight());
            }

        };

        cancelButton = new Button.Plain(
                leftPos + 195,
                topPos-33,
                28, 41,
                Component.literal(""),
                b->onClose(),
                DEFAULT_NARRATION) {
            @Override
            protected void renderContents(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
                guiGraphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        CANCEL, this.getX(), this.getY()-(this.isActive() && this.isHovered? 2:0),
                        0, 0, this.getWidth(), this.getHeight(),
                        this.getWidth(), this.getHeight());
            }

        };

        delete = new Button.Plain(
                leftPos + 230,
                topPos-33,
                28, 41,
                Component.literal(""),
                b->deleteCharacter(),
                DEFAULT_NARRATION) {
            @Override
            protected void renderContents(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
                this.active = Minecraft.getInstance().hasShiftDown();
                if(isHovered && !active){
                    guiGraphics.setTooltipForNextFrame(Component.translatable("persona.shift_confirm"),
                            i,j);
                }
                guiGraphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        active && isHovered?DELETE_H:DELETE, this.getX(), this.getY()-(this.isActive() && this.isHovered? 2:0),
                        0, 0, this.getWidth(), this.getHeight(),
                        this.getWidth(), this.getHeight());
            }

        };

        this.addRenderableWidget(confirmButton);
        this.addRenderableWidget(cancelButton);
        if(rosterEntry != null){
            this.addRenderableWidget(delete);
        }

        if (minecraft.level != null && characterData != null) {
            for (AbstractClientPlayer p : minecraft.level.players()) {
                if (p.getStringUUID().equals(characterData.playerId())) {
                    targetPlayer = p;

                    targetPlayer.setCustomNameVisible(true);
                    targetPlayer.setCustomName(Component.literal("???"));
                    break;
                }
            }
        }else if(minecraft.level != null && rosterEntry != null){
            targetPlayer = FakePlayerFactory.getOrGenerateFakePlayer(
                    minecraft.level,
                    rosterEntry.getPlayerId(),
                    rosterEntry.getCharacterSkin()
            );
        }
    }

    private void onConfirm() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) return;
        if(characterData != null){
            String uuid = characterData.playerId();

            ClientPlayNetworking.send(
                    new AddToRosterC2SPayload(new AddCharacterToRosterData(uuid, name, memoBox.getValue().trim()))
            );

        }
        else if(rosterEntry != null){

            rosterEntry.setNotes(memoBox.getValue().trim());
            rosterEntry.setNickname(name);
            Optional<CharacterData> characterDataOptional = PersonaClient.getCharacterManager().getCurrentCharacter();
            characterDataOptional.ifPresent(characterData1 -> {
                ClientPlayNetworking.send(
                        new RosterDeltaSyncC2SPayload(List.of(rosterEntry),List.of(),characterData1.getCharacterId())
                );
            });
            PersonaClient.getRosterDataManager().saveToCache();
        }
        this.onClose();
    }

    private void deleteCharacter(){
        if(rosterEntry != null){
            Optional<CharacterData> characterDataOptional = PersonaClient.getCharacterManager().getCurrentCharacter();
            characterDataOptional.ifPresent(characterData1 -> {
                ClientPlayNetworking.send(
                        new RosterDeltaSyncC2SPayload(List.of(),List.of(rosterEntry.getCharacterId()),characterData1.getCharacterId())
                );
            });
            PersonaClient.getRosterDataManager().removeEntry(rosterEntry.getCharacterId());
            onClose();
        }
    }
    @Override
    public void onClose() {
        targetPlayer.setCustomNameVisible(false);
        PersonaClient.getRosterDataManager().getCurrentRoster().ifPresent(rosterData ->{
            if(PersonaClient.playerCategoryData.canUseRoster()){
                Minecraft.getInstance().setScreen(new RosterViewScreen(rosterData));
            }
        });
    }
    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderBackground(guiGraphics, i, j, f);
    }


    private float fakeMouseX = this.width / 2f;
    private float fakeMouseY = this.height / 2f;

    private float targetMouseX = 0;
    private float targetMouseY = 0;

    private long lastChangeTime = 0;
    private void updateFakeMouse() {
        long now = System.currentTimeMillis();

        float cx = this.width / 2f;
        float cy = this.height / 2f;

        if (now - lastChangeTime > 3600) {
            lastChangeTime = now;

            if (Math.random() < 0.3 && targetMouseX != cx && targetMouseY != cy) {
                targetMouseX = cx;
                targetMouseY = cy;
            } else {
                targetMouseX = (float)(ThreadLocalRandom.current().nextInt(0,this.width));
                targetMouseY = (float)(cy + ThreadLocalRandom.current().nextInt(-40,4));
            }
        }

        fakeMouseX += (targetMouseX - fakeMouseX) * 0.1f;
        fakeMouseY += (targetMouseY - fakeMouseY) * 0.1f;
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
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

        gui.blit(
                RenderPipelines.GUI_TEXTURED,
                BOOK_LOCATION,
                leftPos, topPos,
                0, 0,
                280, 179,
                280, 179
        );

        gui.blit(
                RenderPipelines.GUI_TEXTURED,
                PLAYER_VIEW_UPPER,
                leftPos+20, topPos+21,
                0, 0,
                110, 3,
                110, 3
        );

        if (targetPlayer != null) {

            updateFakeMouse();
            targetPlayer.setCustomName(Component.literal(nameBox.getValue()));
            int x = leftPos+20;
            int y = topPos+10;
            int w = x+110;
            int h = y+110;
            int scale = 40;
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    gui,
                    x,
                    y,
                    w,
                    h,
                    scale,
                    0.4f,
                    fakeMouseX,
                    fakeMouseY,
                    targetPlayer
            );
        }

        gui.blit(
                RenderPipelines.GUI_TEXTURED,
                PLAYER_VIEW_DOWNER,
                leftPos+20, topPos+121,
                0, 0,
                110, 9,
                110, 9
        );
        super.render(gui, mouseX, mouseY, partialTick);

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
