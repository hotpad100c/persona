package ml.mypals.persona.screen;

import ml.mypals.persona.PersonaClient;
import ml.mypals.persona.characterData.CharacterData;
import ml.mypals.persona.items.rosterData.PlayerRosterData;
import ml.mypals.persona.items.rosterData.RosterEntry;
import ml.mypals.persona.network.packets.roster.RosterDeltaSyncC2SPayload;
import ml.mypals.persona.roster.BookMarkManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import static ml.mypals.persona.Persona.MOD_ID;
import static ml.mypals.persona.fakePlayer.FakePlayerFactory.*;
import static ml.mypals.persona.screen.RosterRecordScreen.DEFAULT_NARRATION;

import java.util.concurrent.ThreadLocalRandom;

public class RosterViewScreen extends Screen {

    private final PlayerRosterData roster;
    private int leftPos;
    private int topPos;

    private static final Identifier BOOK_LOCATION =
            Identifier.fromNamespaceAndPath(MOD_ID,"textures/gui/roster_page.png");

    private static final Identifier BOOKMARK_1 =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/bookmark_1.png");

    private static final Identifier BOOKMARK_2 =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/bookmark_2.png");

    private static final Identifier BOOKMARK_3 =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/bookmark_3.png");

    private static final Identifier BOOKMARK_4 =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/bookmark_4.png");

    private static final Identifier BOOKMARK_5 =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/bookmark_5.png");

    private static final Identifier BOOKMARK_6 =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/bookmark_6.png");

    private static final Identifier BOOKMARK_7 =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/bookmark_7.png");

    private static final Identifier BOOKMARK_8 =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/bookmark_8.png");

    private static final Identifier CHARACTER_ADD =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/character_add.png");

    private static final Identifier CHARACTER_BG =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/character_bg.png");

    private static final Identifier CHARACTER_BG_HIGHLIGHT =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/character_bg_h.png");

    private static final Identifier PLAYER_NAME_BOX =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/player_name_box.png");

    private static final Identifier SEARCH =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/search.png");


    private static final Identifier BOOKMARK =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/bookmark.png");


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

    private static final int BOOKMARK_WIDTH = 27;
    private static final int BOOKMARK_HEIGHT = 11;
    private static final int BOOKMARK_SPACING = 1;
    private static final int STAR_BUTTON_SIZE = 12;

    private int mouseX;
    private int mouseY;
    private int currentPage = 0;
    private int totalPages = 1;

    private final Map<Integer,FakeMouse> fakeMice = new HashMap<>();
    private int hoveredSlot = -1;
    private int hoveredStarButton = -1;

    private RosterPageButton flipLeft;
    private RosterPageButton flipRight;
    private String selectedGroup = "all";
    private String searchText = "";
    private EditBox searchBox;
    private Button editBookmark;
    private List<String> availableGroups = new ArrayList<>();
    private List<GroupBookmark> groupBookmarks = new ArrayList<>();

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

        searchBox = new EditBox(this.font,leftPos + 25, topPos - 14, 80, 18,Component.literal(""));
        searchBox.setTextColor(0xFFaa8979);
        searchBox.setBordered(false);
        searchBox.setFocused(true);
        searchBox.setResponder(text -> {
            searchText = text;
            this.currentPage = 0;
            this.hoveredSlot = -1;
            int entryCount = getFilteredEntries().size();
            totalPages = Math.max(1, (int) Math.ceil((double) entryCount / (SLOTS_PER_PAGE * 2)));

        });

        editBookmark = new net.minecraft.client.gui.components.Button.Plain(
                leftPos + 195,
                topPos-33,
                28, 41,
                Component.literal(""),
                b-> {
                    Minecraft.getInstance().setScreen(new BookmarkManagerScreen(this.roster));
                },
                DEFAULT_NARRATION) {
            @Override
            protected void renderContents(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
                guiGraphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        BOOKMARK, this.getX(), this.getY()-(this.isActive() && this.isHovered? 2:0),
                        0, 0, this.getWidth(), this.getHeight(),
                        this.getWidth(), this.getHeight());
            }

        };

        this.addRenderableWidget(editBookmark);
        this.addRenderableWidget(searchBox);

        collectGroups();

        createGroupBookmarks();

        int entryCount = getFilteredEntries().size();
        totalPages = Math.max(1, (int) Math.ceil((double) entryCount / (SLOTS_PER_PAGE * 2)));

        fakeMice.clear();

        if (totalPages > 1) {
            int btnY = topPos + PAGE_HEIGHT + 10;
            int btnWidth = 60;

            flipLeft = this.addRenderableWidget(new RosterPageButton(leftPos + 20, btnY, false, b -> {
                if (currentPage > 0) {
                    currentPage--;
                    hoveredSlot = -1;
                }
            }, true));

            flipRight = this.addRenderableWidget(new RosterPageButton(leftPos + 20 + totalWidth - btnWidth, btnY, true, b -> {
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

        BookMarkManager bookMarkManager = PersonaClient.getBookMarkManager();
        if(bookMarkManager == null) return;
        bookMarkManager.getActiveMarks().stream()
                .distinct()
                .filter(g -> g != null && !g.isEmpty())
                .forEach(availableGroups::add);
    }

    private void createGroupBookmarks() {
        groupBookmarks.clear();
        int bookmarkX = leftPos - BOOKMARK_WIDTH + 8;
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

        if (searchText != null && !searchText.trim().isEmpty()) {
            String searchLower = searchText.toLowerCase().trim();
            entries = entries.stream()
                    .filter(e -> {
                        String nickname = e.getNickname();
                        String group = e.getGroup();
                        return (nickname != null && nickname.toLowerCase().contains(searchLower)) ||
                                (group != null && group.toLowerCase().contains(searchLower));
                    })
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

        renderBookPages(gui);
        renderGroupBookmarks(gui, mouseX, mouseY);
        String pageText = (currentPage + 1) + " / " + totalPages;
        gui.drawCenteredString(font, pageText, this.width / 2 +3, topPos + PAGE_HEIGHT + 10, 0xFFAAAAAA);

        updateHoveredSlot(mouseX, mouseY);
        renderPlayerGrid(gui, partialTick);

        if (flipLeft != null) flipLeft.visible = currentPage > 0 && totalPages > 1;
        if (flipRight != null) flipRight.visible = currentPage + 1 < totalPages && totalPages > 1;

        this.mouseX = mouseX;
        this.mouseY = mouseY;

        gui.blit(RenderPipelines.GUI_TEXTURED,SEARCH,leftPos + 25, topPos - 18, 0, 0,100,18,100,18);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void renderGroupBookmarks(GuiGraphics gui, int mouseX, int mouseY) {
        for (GroupBookmark bookmark : groupBookmarks) {
            boolean isSelected = bookmark.group.equals(selectedGroup);
            boolean isHovered = mouseX >= bookmark.x && mouseX < bookmark.x + BOOKMARK_WIDTH &&
                    mouseY >= bookmark.y && mouseY < bookmark.y + BOOKMARK_HEIGHT;

            int index = Math.floorMod(bookmark.group.hashCode(), 8);

            Identifier texture = switch (index) {
                case 0 -> BOOKMARK_1;
                case 1 -> BOOKMARK_2;
                case 2 -> BOOKMARK_3;
                case 3 -> BOOKMARK_4;
                case 4 -> BOOKMARK_5;
                case 5 -> BOOKMARK_6;
                case 6 -> BOOKMARK_7;
                default -> BOOKMARK_8;
            };


            int delta = (isHovered || isSelected?4:0);
            int posX = bookmark.x - delta;

            gui.blit(RenderPipelines.GUI_TEXTURED,texture,posX, bookmark.y, 0, 0,BOOKMARK_WIDTH+delta,BOOKMARK_HEIGHT,BOOKMARK_WIDTH+delta,BOOKMARK_HEIGHT);


            String displayText = bookmark.group.equals("all") ? "All" : bookmark.group;
            String shorten = displayText;

            if (font.width(displayText) > SLOT_SIZE - 10) {
                shorten = font.plainSubstrByWidth(shorten, BOOKMARK_WIDTH - 14) + "...";
                if(isHovered)  gui.setTooltipForNextFrame(Component.literal(displayText), mouseX, mouseY);
            }

            gui.drawString(font, shorten,
                    posX+4,
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
                                  int sX, int sY, int slotIndex, boolean isHovered) {

        int slotX = sX - (isHovered?2:0);
        int slotY = sY - (isHovered?2:0);

        gui.blit(RenderPipelines.GUI_TEXTURED,isHovered?CHARACTER_BG_HIGHLIGHT:CHARACTER_BG,slotX, slotY, 0, 0,SLOT_SIZE,SLOT_SIZE,SLOT_SIZE,SLOT_SIZE);

        gui.blit(RenderPipelines.GUI_TEXTURED,PLAYER_NAME_BOX,sX, sY+SLOT_SIZE+1, 0, 0,SLOT_SIZE,9,SLOT_SIZE,9);

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
            if (font.width(displayName) > SLOT_SIZE) {
                shorten = font.plainSubstrByWidth(shorten, SLOT_SIZE - 4) + "...";
            }

            int nameColor = isHovered ? 0xFFFFFFFF : 0xFFaa8979;

            gui.pose().pushMatrix();
            int y = sY + 5 + SLOT_SIZE - (SLOT_SIZE - SLOT_INNER_SIZE) / 2;
            int x = sX + SLOT_SIZE/2;
            gui.pose().translate(x,y);
            gui.pose().scale(0.75f);
            gui.pose().translate(-x,-y);
            gui.drawString(font, shorten, x - font.width(shorten) / 2, y, nameColor,isHovered);
            gui.pose().popMatrix();
            renderStarButton(gui, entry, slotX, slotY, slotIndex);

            if (isHovered) gui.setTooltipForNextFrame(Component.literal(displayName), mouseX, mouseY);
        } else {
            gui.drawString(font, "?",
                    sX + SLOT_SIZE / 2,
                    sY + SLOT_SIZE / 2 - 4,
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

                Optional<CharacterData> characterDataOptional = PersonaClient.getCharacterManager().getCurrentCharacter();
                characterDataOptional.ifPresent(characterData1 -> {
                    ClientPlayNetworking.send(
                            new RosterDeltaSyncC2SPayload(List.of(entry),List.of(),characterData1.getCharacterId())
                    );
                });
                PersonaClient.getRosterDataManager().saveToCache();

                init();
                return true;
            }
        }
        if(hoveredSlot>=0){
            List<RosterEntry> entries = getFilteredEntries();
            int startIndex = currentPage * SLOTS_PER_PAGE * 2;
            int globalIndex = startIndex + hoveredSlot;
            if (globalIndex < entries.size()) {
                RosterEntry entry = entries.get(globalIndex);
                Minecraft.getInstance().setScreen(new RosterRecordScreen(entry));

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