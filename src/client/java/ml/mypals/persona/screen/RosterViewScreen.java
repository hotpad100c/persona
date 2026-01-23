package ml.mypals.persona.screen;

import ml.mypals.persona.items.rosterData.PlayerRosterData;
import ml.mypals.persona.items.rosterData.RosterEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

import static ml.mypals.persona.Persona.MOD_ID;
import static ml.mypals.persona.fakePlayer.FakePlayerFactory.*;

import net.minecraft.client.gui.components.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class RosterViewScreen extends Screen {

    private static final Logger log = LoggerFactory.getLogger(RosterViewScreen.class);
    private final PlayerRosterData roster;
    private int leftPos;
    private int topPos;

    private static final Identifier BOOK_LOCATION =
            Identifier.fromNamespaceAndPath(MOD_ID,"textures/gui/roster_page.png");

    private static final int PAGE_WIDTH = 140;
    private static final int PAGE_HEIGHT = 163;
    private static final int PAGE_GAP = -6;
    private static final int GRID_LEFT_OFFSET = 10;
    private static final int GRID_TOP_OFFSET = 30;
    private static final int SLOT_SIZE = 35;
    private static final int SLOT_SPACING = 4;
    private static final int SLOT_INNER_SIZE = 30;
    private static final int SLOTS_PER_ROW = 3;
    private static final int SLOTS_PER_COL = 3;
    private static final int SLOTS_PER_PAGE = SLOTS_PER_ROW * SLOTS_PER_COL;

    private int currentPage = 0;
    private int totalPages = 1;

    private final List<FakeMouse> fakeMice = new ArrayList<>();

    private int hoveredSlot = -1;

    private PageButton flipLeft;
    private PageButton flipRight;

    public RosterViewScreen(PlayerRosterData roster) {
        super(Component.literal(""));
        this.roster = roster;
    }

    @Override
    protected void init() {
        super.init();

        int totalWidth = PAGE_WIDTH * 2 + PAGE_GAP;
        leftPos = (this.width - totalWidth) / 2;
        topPos = (this.height - PAGE_HEIGHT) / 2;

        int entryCount = roster.getEntries().size();
        totalPages = Math.max(1, (int) Math.ceil((double) entryCount / (SLOTS_PER_PAGE * 2)));

        fakeMice.clear();
        for (int i = 0; i < SLOTS_PER_PAGE * 2; i++) {
            fakeMice.add(new FakeMouse(width, height));
        }

        if (totalPages > 1) {
            int btnY = topPos + PAGE_HEIGHT - 10;
            int btnWidth = 60;

            flipLeft = this.addRenderableWidget(new PageButton(leftPos + 20, btnY, false,b -> {
                if (currentPage > 0) {
                    currentPage--;
                    hoveredSlot = -1;
                }
            }, true));


            flipRight = this.addRenderableWidget(new PageButton(leftPos + 20 + totalWidth - btnWidth, btnY, true,b -> {
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    hoveredSlot = -1;
                }
            }, true));

        }
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderBackground(guiGraphics, i, j, f);
        renderBookPages(guiGraphics);

    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        gui.drawCenteredString(font, this.title, this.width / 2, topPos - 20, 0xFFFFFF);
        String pageText = (currentPage + 1) + " / " + totalPages;
        gui.drawCenteredString(font, pageText, this.width / 2, topPos + PAGE_HEIGHT + 5, 0xAAAAAA);
        updateHoveredSlot(mouseX, mouseY);
        renderPlayerGrid(gui, partialTick);

        flipLeft.visible = currentPage > 0 && totalPages > 1;
        flipRight.visible = currentPage+1 < totalPages && totalPages > 1;

        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void renderBookPages(GuiGraphics gui) {

        gui.blit(
                RenderPipelines.GUI_TEXTURED,
                BOOK_LOCATION,
                leftPos, topPos,
                0, 0,
                280, 179,
                280, 179
        );
    }

    private void updateHoveredSlot(int mouseX, int mouseY) {
        hoveredSlot = -1;

        List<RosterEntry> entries = roster.getEntries();
        int startIndex = currentPage * SLOTS_PER_PAGE * 2;

        for (int side = 0; side < 2; side++) {
            int baseX = leftPos + (side == 0 ? 0 : PAGE_WIDTH + PAGE_GAP);
            int pageStart = startIndex + side * SLOTS_PER_PAGE;

            for (int i = 0; i < SLOTS_PER_PAGE; i++) {
                int globalIndex = pageStart + i;
                if (globalIndex >= entries.size()) break;

                int row = i / SLOTS_PER_ROW;
                int col = i % SLOTS_PER_ROW;

                int slotX = baseX + GRID_LEFT_OFFSET + col * (SLOT_SIZE + SLOT_SPACING);
                int slotY = topPos + GRID_TOP_OFFSET + row * (SLOT_SIZE + SLOT_SPACING);

                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                        mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    hoveredSlot = side * SLOTS_PER_PAGE + i;
                    break;
                }
            }
        }
    }

    private void renderPlayerGrid(GuiGraphics gui, float partialTick) {
        List<RosterEntry> entries = roster.getEntries();
        int startIndex = currentPage * SLOTS_PER_PAGE * 2;

        for (int side = 0; side < 2; side++) {
            int textureWidth = PAGE_WIDTH * 2 + PAGE_GAP;
            int baseX = leftPos
                    + side * PAGE_WIDTH
                    + side * PAGE_GAP
                    + (286 - textureWidth) / 2;

            int pageStart = startIndex + side * SLOTS_PER_PAGE;

            for (int i = 0; i < SLOTS_PER_PAGE; i++) {
                int globalIndex = pageStart + i;
                if (globalIndex >= entries.size()) break;

                RosterEntry entry = entries.get(globalIndex);
                int row = i / SLOTS_PER_ROW;
                int col = i % SLOTS_PER_ROW;

                int slotX = baseX + GRID_LEFT_OFFSET + col * (SLOT_SIZE + SLOT_SPACING);
                int slotY = topPos + GRID_TOP_OFFSET + row * (SLOT_SIZE + SLOT_SPACING);

                int slotIndex = side * SLOTS_PER_PAGE + i;
                boolean isHovered = (slotIndex == hoveredSlot);

                renderPlayerSlot(gui, partialTick, entry, slotX, slotY, slotIndex, isHovered);
            }
        }
    }

    private void renderPlayerSlot(GuiGraphics gui, float partialTick, RosterEntry entry,
                                  int slotX, int slotY, int slotIndex, boolean isHovered) {
        int hash = entry.getCharacterId().hashCode();
        float hue = (hash & 0xFFFF) / 65535f;
        float saturation = 0.45f;
        float brightness = isHovered ? 1.0f : 0.85f;
        int rgb = Color.HSBtoRGB(hue, saturation, brightness) & 0x00FFFFFF;
        int alpha = isHovered ? 0xAA : 0x66;
        int bgColor = (alpha << 24) | rgb;
        int borderColor = isHovered ? 0xFF000000 : 0x88000000;

        gui.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, bgColor);
        gui.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + 1, borderColor);
        gui.fill(slotX, slotY, slotX + 1, slotY + SLOT_SIZE, borderColor);
        gui.fill(slotX + SLOT_SIZE - 1, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, borderColor);
        gui.fill(slotX, slotY + SLOT_SIZE - 1, slotX + SLOT_SIZE, slotY + SLOT_SIZE, borderColor);

        AbstractClientPlayer player = getOrGenerateFakePlayer(
                minecraft.level,
                entry.getNickname(),
                entry.getCharacterSkin()
        );

        if (player != null) {
            FakeMouse mouse = fakeMice.get(slotIndex);

            if (isHovered) {
                mouse.update(partialTick, width, slotY);
            } else {
                mouse.resetToCenter();
            }

            int modelX = slotX + (SLOT_SIZE - SLOT_INNER_SIZE) / 2;
            int textArea = SLOT_SIZE - SLOT_INNER_SIZE;
            int modelY = slotY
                    + (SLOT_SIZE - SLOT_INNER_SIZE) / 2
                    + textArea / 4;


            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    gui,
                    modelX, modelY,
                    modelX + SLOT_INNER_SIZE, modelY + SLOT_INNER_SIZE,
                    20,
                    0.7f,
                    mouse.fakeX,
                    mouse.fakeY,
                    player
            );

            String displayName = entry.getNickname().isEmpty()
                    ? player.getName().getString()
                    : entry.getNickname();

            if (font.width(displayName) > SLOT_SIZE - 4) {
                displayName = font.plainSubstrByWidth(displayName, SLOT_SIZE - 8) + "...";
            }

            int nameColor = isHovered ? 0x000000 : 0x333333;
            gui.drawString(font, displayName,
                    slotX + SLOT_SIZE / 2,
                    slotY + SLOT_SIZE - (SLOT_SIZE - SLOT_INNER_SIZE) / 2,
                    nameColor);
        } else {
            gui.drawCenteredString(font, "?",
                    slotX + SLOT_SIZE / 2,
                    slotY + SLOT_SIZE / 2 - 4,
                    0x888888);
        }
    }

    private static class FakeMouse {
        float fakeX = 0;
        float fakeY = 0;
        float targetX = 0;
        float targetY = 0;
        long lastChange = 0;
        float defaultX = 0;
        float defaultY = 0;
        public FakeMouse(int screenWidth, int screenHeight){
            defaultX = ThreadLocalRandom.current().nextInt(0,screenWidth);
            defaultY = ThreadLocalRandom.current().nextInt(0,screenHeight/2);
            fakeX = defaultX;
            fakeY = defaultY;
        }
        void update(float partialTick, int screenWidth, int gridY) {
            long now = System.currentTimeMillis();

            if (now - lastChange > 1500 + (int)(Math.random() * 1000)) {
                lastChange = now;
                float cx = screenWidth / 2f;
                float gridHeight = SLOTS_PER_COL * SLOT_SIZE
                        + (SLOTS_PER_COL - 1) * SLOT_SPACING;

                float cy = gridY + gridHeight / 2f;

                if (Math.random() < 0.3) {
                    targetX = cx;
                    targetY = cy;
                } else {
                    double angle = Math.random() * Math.PI * 2;
                    double r = 0.2 + Math.random() * 0.25;
                    targetX = (float)(cx + Math.cos(angle) * screenWidth * r);
                    targetY = (float)(cy + Math.sin(angle) * gridY * r);
                }
            }

            float speed = 0.05f * Math.min(partialTick * 20, 2);
            fakeX += (targetX - fakeX) * speed;
            fakeY += (targetY - fakeY) * speed;
        }

        void resetToCenter() {

            fakeX += (defaultX - fakeX) * 0.15f;
            fakeY += (defaultY - fakeY) * 0.15f;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}