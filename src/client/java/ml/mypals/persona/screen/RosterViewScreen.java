package ml.mypals.persona.screen;

import ml.mypals.persona.items.rosterData.PlayerRosterData;
import ml.mypals.persona.items.rosterData.RosterEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import static ml.mypals.persona.Persona.MOD_ID;
import static ml.mypals.persona.fakePlayer.FakePlayerFactory.*;

import java.util.concurrent.ThreadLocalRandom;

public class RosterViewScreen extends Screen {

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

    private static final int BOOKMARK_WIDTH = 40;
    private static final int BOOKMARK_HEIGHT = 12;
    private static final int BOOKMARK_SPACING = 1;
    private static final int STAR_BUTTON_SIZE = 12;

    private int mouseX;
    private int mouseY;
    private int currentPage = 0;
    private int totalPages = 1;

    private final Map<Integer,FakeMouse> fakeMice = new HashMap<>();
    private int hoveredSlot = -1;
    private int hoveredStarButton = -1;

    private PageButton flipLeft;
    private PageButton flipRight;

    private String selectedGroup = "all";
    private List<String> availableGroups = new ArrayList<>();
    private List<GroupBookmark> groupBookmarks = new ArrayList<>();

    public RosterViewScreen(PlayerRosterData roster) {
        super(Component.literal(""));
        this.roster = roster;
    }

    @Override
    protected void init() {
        super.init();
        //TODO Sync changes with the server.
        int totalWidth = PAGE_WIDTH * 2 + PAGE_GAP;
        leftPos = (this.width - totalWidth) / 2;
        topPos = (this.height - PAGE_HEIGHT) / 2;

        collectGroups();

        createGroupBookmarks();

        int entryCount = getFilteredEntries().size();
        totalPages = Math.max(1, (int) Math.ceil((double) entryCount / (SLOTS_PER_PAGE * 2)));

        fakeMice.clear();

        if (totalPages > 1) {
            int btnY = topPos + PAGE_HEIGHT - 10;
            int btnWidth = 60;

            flipLeft = this.addRenderableWidget(new PageButton(leftPos + 20, btnY, false, b -> {
                if (currentPage > 0) {
                    currentPage--;
                    hoveredSlot = -1;
                }
            }, true));

            flipRight = this.addRenderableWidget(new PageButton(leftPos + 20 + totalWidth - btnWidth, btnY, true, b -> {
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    hoveredSlot = -1;
                }
            }, true));
        }
    }

    private void collectGroups() {
        availableGroups.clear();
        availableGroups.add("all");

        roster.getEntries().stream()
                .map(RosterEntry::getGroup)
                .distinct()
                .filter(g -> g != null && !g.isEmpty())
                .forEach(availableGroups::add);
    }

    private void createGroupBookmarks() {
        groupBookmarks.clear();
        int bookmarkX = leftPos - BOOKMARK_WIDTH+5;
        int startY = topPos + 20;
        int availableHeight = PAGE_HEIGHT - 40;

        int groupCount = availableGroups.size();
        int totalHeightNeeded = groupCount * BOOKMARK_HEIGHT + (groupCount - 1) * BOOKMARK_SPACING;

        int actualSpacing = BOOKMARK_SPACING;
        if (totalHeightNeeded > availableHeight) {
            actualSpacing = (availableHeight - groupCount * BOOKMARK_HEIGHT) / Math.max(1, groupCount - 1);
        }

        for (int i = 0; i < availableGroups.size(); i++) {
            String group = availableGroups.get(i);
            int y = startY + i * (BOOKMARK_HEIGHT + actualSpacing);
            groupBookmarks.add(new GroupBookmark(group, bookmarkX, y));
        }
    }

    private List<RosterEntry> getFilteredEntries() {
        List<RosterEntry> entries = new ArrayList<>(roster.getEntries());

        entries.sort((a, b) -> Boolean.compare(b.isStarred(), a.isStarred()));
        if (!"all".equals(selectedGroup)) {
            entries = entries.stream()
                    .filter(e -> selectedGroup.equals(e.getGroup()))
                    .collect(Collectors.toList());
        }

        return entries;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderBackground(guiGraphics, i, j, f);
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {

        renderGroupBookmarks(gui, mouseX, mouseY);
        renderBookPages(gui);
        String pageText = (currentPage + 1) + " / " + totalPages;
        gui.drawCenteredString(font, pageText, this.width / 2 +3, topPos + PAGE_HEIGHT + 10, 0xFFAAAAAA);

        updateHoveredSlot(mouseX, mouseY);
        renderPlayerGrid(gui, partialTick);

        if (flipLeft != null) flipLeft.visible = currentPage > 0 && totalPages > 1;
        if (flipRight != null) flipRight.visible = currentPage + 1 < totalPages && totalPages > 1;

        this.mouseX = mouseX;
        this.mouseY = mouseY;
        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void renderGroupBookmarks(GuiGraphics gui, int mouseX, int mouseY) {
        for (GroupBookmark bookmark : groupBookmarks) {
            boolean isSelected = bookmark.group.equals(selectedGroup);
            boolean isHovered = mouseX >= bookmark.x && mouseX < bookmark.x + BOOKMARK_WIDTH &&
                    mouseY >= bookmark.y && mouseY < bookmark.y + BOOKMARK_HEIGHT;

            int hash = bookmark.hashCode();
            float hue = (hash & 0xFFFF) / 65535f;
            float saturation = 0.5f;
            float brightness = isHovered ? 1.0f : 0.8f;
            int rgb = Color.HSBtoRGB(hue, saturation, brightness);
            int borderColor = isHovered || isSelected ?0x88FFFFFF : 0x66FFFFFF;

            int posX = bookmark.x - (isHovered || isSelected?4:0);

            gui.fill(posX, bookmark.y, bookmark.x + BOOKMARK_WIDTH, bookmark.y + BOOKMARK_HEIGHT, rgb);
            gui.fill(posX, bookmark.y + BOOKMARK_HEIGHT - 1, bookmark.x + BOOKMARK_WIDTH, bookmark.y + BOOKMARK_HEIGHT, borderColor);

            String displayText = bookmark.group.equals("all") ? "All" : bookmark.group;
            String shorten = displayText;

            if (font.width(displayText) > SLOT_SIZE - 10) {
                shorten = font.plainSubstrByWidth(shorten, BOOKMARK_WIDTH - 14) + "...";
                if(isHovered)  gui.setTooltipForNextFrame(Component.literal(displayText), mouseX, mouseY);
            }

            gui.drawString(font, shorten,
                    posX+2,
                    bookmark.y + (BOOKMARK_HEIGHT - font.lineHeight) / 2+1,
                    -2039584);
        }
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
        hoveredStarButton = -1;

        List<RosterEntry> entries = getFilteredEntries();
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
                int slotY = topPos - 10 + GRID_TOP_OFFSET + row * (SLOT_SIZE + SLOT_SPACING + 10);

                int starBtnX = slotX + SLOT_SIZE - STAR_BUTTON_SIZE;
                int starBtnY = slotY + STAR_BUTTON_SIZE/2 - 4;

                if (mouseX >= starBtnX && mouseX < starBtnX + STAR_BUTTON_SIZE &&
                        mouseY >= starBtnY && mouseY < starBtnY + STAR_BUTTON_SIZE) {
                    hoveredStarButton = side * SLOTS_PER_PAGE + i;
                    hoveredSlot = side * SLOTS_PER_PAGE + i;
                    return;
                }

                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                        mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    hoveredSlot = side * SLOTS_PER_PAGE + i;
                    break;
                }
            }
        }
    }

    private void renderPlayerGrid(GuiGraphics gui, float partialTick) {
        List<RosterEntry> entries = getFilteredEntries();
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
                int slotY = topPos - 10 + GRID_TOP_OFFSET + row * (SLOT_SIZE + SLOT_SPACING + 10);

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
            if(mouse == null){
            fakeMice.put(slotIndex, new FakeMouse(slotX, slotY));
            }
            mouse = fakeMice.get(slotIndex);

            if (isHovered) {
                mouse.update(partialTick, width, slotY,mouseX,mouseY);
            } else {
                mouse.resetToCenter(slotX,slotY);
            }

            int modelX = slotX + (SLOT_SIZE - SLOT_INNER_SIZE) / 2;
            int textArea = SLOT_SIZE - SLOT_INNER_SIZE;
            int modelY = slotY + (SLOT_SIZE - SLOT_INNER_SIZE) / 2 + textArea / 4;

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
            String shorten = displayName;
            if (font.width(displayName) > SLOT_SIZE - 4) {
                shorten = font.plainSubstrByWidth(shorten, SLOT_SIZE - 8) + "...";
            }

            int nameColor = isHovered ? 0xFFFFFFFF : -2039584;

            gui.drawString(font, shorten,
                    slotX,
                    slotY + 4 + SLOT_SIZE - (SLOT_SIZE - SLOT_INNER_SIZE) / 2,
                    nameColor);

            renderStarButton(gui, entry, slotX, slotY, slotIndex);

            if (isHovered) gui.setTooltipForNextFrame(Component.literal(displayName), mouseX, mouseY);
        } else {
            gui.drawString(font, "?",
                    slotX + SLOT_SIZE / 2,
                    slotY + SLOT_SIZE / 2 - 4,
                    -2039584);
        }
    }

    private void renderStarButton(GuiGraphics gui, RosterEntry entry, int slotX, int slotY, int slotIndex) {
        int starBtnX = slotX + SLOT_SIZE - STAR_BUTTON_SIZE - 2;
        int starBtnY = slotY + STAR_BUTTON_SIZE/2 - 4;

        boolean isHovered = (slotIndex == hoveredStarButton);
        boolean isStarred = entry.isStarred();

        int bgColor = isHovered ? 0xFFFFD700 : (isStarred ? 0xFFFFA500 : 0x88444444);

        String star = isStarred ? "★" : "☆";
        gui.drawCenteredString(font, star,
                starBtnX + STAR_BUTTON_SIZE / 2,
                starBtnY ,
                bgColor);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent mouseButtonEvent, boolean button) {

        for (GroupBookmark bookmark : groupBookmarks.reversed()) {
            if (mouseX >= bookmark.x && mouseX < bookmark.x + BOOKMARK_WIDTH &&
                    mouseY >= bookmark.y && mouseY < bookmark.y + BOOKMARK_HEIGHT) {
                selectedGroup = bookmark.group;
                currentPage = 0;
                hoveredSlot = -1;
                init();
                return true;
            }
        }

        if (hoveredStarButton >= 0) {
            List<RosterEntry> entries = getFilteredEntries();
            int startIndex = currentPage * SLOTS_PER_PAGE * 2;
            int globalIndex = startIndex + hoveredStarButton;

            if (globalIndex < entries.size()) {
                RosterEntry entry = entries.get(globalIndex);
                entry.setStarred(!entry.isStarred());
                init();
                return true;
            }
        }

        return super.mouseClicked(mouseButtonEvent, button);
    }

    private record GroupBookmark(String group, int x, int y) {
    }

    private static class FakeMouse {
        float fakeX = 0;
        float fakeY = 0;
        float targetX = 0;
        float targetY = 0;
        long lastChange = 0;
        float defaultX = 0;
        float defaultY = 0;

        public FakeMouse(int defaultX, int defaultY) {
            this.defaultX = defaultX;
            this.defaultY = defaultY;
            fakeX = defaultX;
            fakeY = defaultY;
        }

        void update(float partialTick, int screenWidth, int gridY, int mouseX, int mouseY) {
            long now = System.currentTimeMillis();

            if (now - lastChange > 1500 + (int) (Math.random() * 1000)) {
                lastChange = now;
                if (Math.random() < 0.3) {
                    targetX = defaultX;
                    targetY = defaultY;
                } else {
                    targetX = ThreadLocalRandom.current().nextInt(0,screenWidth);
                            targetY = ThreadLocalRandom.current().nextInt((int) ((float) gridY -5), (int) ((float) gridY +5));
                }
            }

            float speed = 0.05f * Math.min(partialTick * 20, 2);
            fakeX += (targetX - fakeX) * speed;
            fakeY += (targetY - fakeY) * speed;
        }

        void resetToCenter(int defaultX, int defaultY) {
            fakeX += (defaultX - fakeX) * 0.15f;
            fakeY += (defaultY - fakeY) * 0.15f;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}