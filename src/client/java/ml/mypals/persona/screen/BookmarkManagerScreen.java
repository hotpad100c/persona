package ml.mypals.persona.screen;

import ml.mypals.persona.PersonaClient;
import ml.mypals.persona.items.rosterData.PlayerRosterData;
import ml.mypals.persona.items.rosterData.RosterEntry;
import ml.mypals.persona.roster.BookMarkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static ml.mypals.persona.Persona.MOD_ID;
import static ml.mypals.persona.screen.RosterRecordScreen.DEFAULT_NARRATION;

public class BookmarkManagerScreen extends Screen {

    private final PlayerRosterData roster;
    private int leftPos;
    private int topPos;

    private static final Identifier BOOK_LOCATION =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/roster_page.png");

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
    private static final Identifier DELETE =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/delete.png");
    private static final Identifier DELETE_H =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/delete_h.png");

    private static final int PAGE_WIDTH = 140;
    private static final int PAGE_HEIGHT = 163;
    private static final int PAGE_GAP = -6;

    private static final int BOOKMARK_WIDTH = 54;
    private static final int BOOKMARK_HEIGHT = 21;
    private static final int BOOKMARK_SPACING = 1;
    private static final int MAX_ACTIVE_BOOKMARKS = 10;
    private static final int ACTIVE_BOOKMARK_X_OFFSET = 8;

    private static final int AVAILABLE_COLS = 2;
    private static final int AVAILABLE_ROWS = 4;
    private static final int AVAILABLE_SLOT_SIZE = 30;
    private static final int AVAILABLE_SLOT_SPACING = 10;
    private static final int AVAILABLE_GRID_LEFT = 10;
    private static final int AVAILABLE_GRID_TOP = 25;

    private static final int EDIT_AREA_LEFT = 15;
    private static final int EDIT_AREA_TOP = 25;
    private static final int EDIT_SLOT_HEIGHT = 14;

    private List<BookmarkData> activeBookmarks;
    private List<BookmarkData> availableBookmarks;

    private BookmarkData draggedBookmark = null;
    private int dragMouseX, dragMouseY;
    private boolean isDragging = false;
    private DragSource dragSource = DragSource.NONE;

    private int hoveredActiveSlot = -1;
    private int hoveredAvailableSlot = -1;
    private int hoveredSideBookmark = -1;
    private boolean hoveredNewButton = false;
    private int mouseX, mouseY;
    private Button delete;

    private EditBox editingBox = null;
    private int editingSlot = -1;

    private int nextCustomBookmarkIndex = 1;

    private RosterPageButton flipLeft;
    private RosterPageButton flipRight;
    private int currentPage = 0;
    private int itemsPerPage = AVAILABLE_COLS * AVAILABLE_ROWS;

    public BookmarkManagerScreen(PlayerRosterData roster) {
        super(Component.literal("Bookmark Manager"));
        this.roster = roster;
        initializeData();
    }

    private void initializeData() {
        BookMarkManager bookMarkManager = PersonaClient.getBookMarkManager();

        List<String> active = bookMarkManager.getActiveMarks();
        List<String> available = bookMarkManager.getAvailableMarks();
        availableBookmarks = new ArrayList<>();
        activeBookmarks = new ArrayList<>();

        active.forEach(string -> activeBookmarks.add(new BookmarkData(string, Math.floorMod(string.hashCode(), 8))));

        if (available.isEmpty()) {
            roster.getEntries().stream()
                    .map(RosterEntry::getGroup)
                    .distinct()
                    .filter(g -> g != null && !g.isEmpty())
                    .forEach(string -> {
                        bookMarkManager.add(string);
                        BookmarkData bookmarkData = new BookmarkData(string, Math.floorMod(string.hashCode(), 8));
                        availableBookmarks.add(bookmarkData);
                    });
        } else {
            available.forEach(string -> availableBookmarks.add(new BookmarkData(string, Math.floorMod(string.hashCode(), 8))));
        }
    }

    @Override
    protected void init() {
        super.init();
        int totalWidth = PAGE_WIDTH * 2 + PAGE_GAP;
        leftPos = (this.width - totalWidth) / 2;
        topPos = (this.height - PAGE_HEIGHT) / 2;

        int btnY = topPos + PAGE_HEIGHT - 20;
        int btnWidth = 23;
        int rightPageX = leftPos + PAGE_WIDTH + PAGE_GAP;

        flipLeft = this.addRenderableWidget(new RosterPageButton(rightPageX + 10, btnY, false, b -> {
            if (currentPage > 0) {
                currentPage--;
                hoveredAvailableSlot = -1;
            }
        }, true));

        flipRight = this.addRenderableWidget(new RosterPageButton(rightPageX + PAGE_WIDTH - btnWidth - 10, btnY, true, b -> {
            int totalPages = (int) Math.ceil((double) availableBookmarks.size() / itemsPerPage);
            if (currentPage < totalPages - 1) {
                currentPage++;
                hoveredAvailableSlot = -1;
            }
        }, true));

        delete = new Button.Plain(
                leftPos + 230,
                topPos - 33,
                28, 41,
                Component.literal(""),
                b -> {},
                DEFAULT_NARRATION) {
            @Override
            protected void renderContents(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
                if (isHovered) {
                    guiGraphics.setTooltipForNextFrame(Component.translatable("persona.delete_bookmark"), i, j);
                }
                guiGraphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        isHovered ? DELETE_H : DELETE,
                        this.getX(), this.getY() - (this.isActive() && this.isHovered ? 2 : 0),
                        0, 0, this.getWidth(), this.getHeight(),
                        this.getWidth(), this.getHeight());
            }
        };
        this.addRenderableWidget(delete);
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        int totalPages = (int) Math.ceil((double) availableBookmarks.size() / itemsPerPage);
        if (flipLeft != null) flipLeft.visible = currentPage > 0 && totalPages > 1;
        if (flipRight != null) flipRight.visible = currentPage + 1 < totalPages && totalPages > 1;

        renderBookPages(gui);
        updateHoverStates(mouseX, mouseY);
        renderSideBookmarks(gui);
        renderLeftEditPage(gui);
        renderRightAvailablePage(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        if (isDragging && draggedBookmark != null) {
            renderDraggedBookmark(gui, mouseX, mouseY);
        }
        renderInstructions(gui);
    }

    private void renderBookPages(GuiGraphics gui) {
        gui.blit(RenderPipelines.GUI_TEXTURED, BOOK_LOCATION,
                leftPos, topPos, 0, 0, 280, 179, 280, 179);
    }

    private void updateHoverStates(int mouseX, int mouseY) {
        if (isDragging) {
            hoveredSideBookmark = getSideBookmarkSlot(mouseX, mouseY);
            hoveredActiveSlot = -1;
            hoveredAvailableSlot = -1;
            hoveredNewButton = false;
        } else {
            hoveredSideBookmark = getSideBookmarkSlot(mouseX, mouseY);
            hoveredActiveSlot = getActiveSlot(mouseX, mouseY);
            hoveredAvailableSlot = getAvailableSlot(mouseX, mouseY);
            hoveredNewButton = isMouseOverNewButton(mouseX, mouseY);
        }
    }

    private void renderSideBookmarks(GuiGraphics gui) {
        int scaledWidth = 27;
        int scaledHeight = 11;
        int bookmarkX = leftPos - scaledWidth + ACTIVE_BOOKMARK_X_OFFSET;
        int startY = topPos + 20;

        for (int i = 0; i < activeBookmarks.size() && i < MAX_ACTIVE_BOOKMARKS; i++) {
            BookmarkData bookmark = activeBookmarks.get(i);
            int y = startY + i * (scaledHeight + BOOKMARK_SPACING);
            boolean isHovered = hoveredSideBookmark == i;
            Identifier texture = getBookmarkTexture(bookmark.colorIndex);
            int delta = isHovered ? 4 : 0;
            int posX = bookmarkX - delta;

            gui.blit(RenderPipelines.GUI_TEXTURED, texture,
                    posX, y, 0, 0,
                    scaledWidth + delta, scaledHeight,
                    scaledWidth + delta, scaledHeight);

            String displayText = bookmark.name;
            if (font.width(displayText) > scaledWidth - 8) {
                displayText = font.plainSubstrByWidth(displayText, scaledWidth - 12) + "...";
            }

            gui.drawString(font, displayText,
                    posX + 4,
                    y + (scaledHeight - font.lineHeight) / 2 + 1,
                    0xFFFFFFFF);
        }

        if (isDragging
                && dragSource == DragSource.AVAILABLE
                && activeBookmarks.stream().noneMatch(bookmarkData -> bookmarkData.name.equals(draggedBookmark.name))
                && activeBookmarks.size() < MAX_ACTIVE_BOOKMARKS) {
            int y = startY + activeBookmarks.size() * (scaledHeight + BOOKMARK_SPACING);
            gui.fill(bookmarkX - 2, y, bookmarkX + scaledWidth, y + scaledHeight, 0x7700FF00);
        }
    }

    private void renderLeftEditPage(GuiGraphics gui) {
        int baseX = leftPos + EDIT_AREA_LEFT;
        int baseY = topPos + EDIT_AREA_TOP;

        gui.drawString(font, Component.translatable("persona.active_marks"), baseX + 10, baseY - 10, 0xFFaa8979, false);

        for (int i = 0; i < activeBookmarks.size() && i < MAX_ACTIVE_BOOKMARKS; i++) {
            BookmarkData bookmark = activeBookmarks.get(i);
            int slotY = baseY + i * EDIT_SLOT_HEIGHT;
            boolean isHovered = hoveredActiveSlot == i;
            boolean isEditing = editingSlot == i;

            int bgColor = isHovered ? 0x40FFFFFF : 0x20FFFFFF;
            gui.fill(baseX - 2, slotY, baseX + 110, slotY + EDIT_SLOT_HEIGHT, bgColor);

            Identifier texture = getBookmarkTexture(bookmark.colorIndex);
            gui.blit(RenderPipelines.GUI_TEXTURED, texture,
                    baseX - 2, slotY, 0, 0, 27, 11, 27, 11);

            if (isEditing && editingBox != null) {
                editingBox.render(gui, mouseX, mouseY, 0);
            } else {
                gui.drawString(font, bookmark.name, baseX + 30, slotY + 5, 0xFFaa8979);
            }

            String deleteBtn = "✕";
            int deleteBtnX = baseX + 100;
            boolean deleteHovered = mouseX >= deleteBtnX && mouseX < deleteBtnX + 10 &&
                    mouseY >= slotY && mouseY < slotY + 10;
            int deleteColor = deleteHovered ? 0xFFFF4444 : 0xFF888888;
            gui.drawString(font, deleteBtn, deleteBtnX, slotY, deleteColor);
        }

        if (activeBookmarks.size() < MAX_ACTIVE_BOOKMARKS) {
            int btnY = baseY + activeBookmarks.size() * EDIT_SLOT_HEIGHT;
            int btnBgColor = hoveredNewButton ? 0x4000FF00 : 0x2000FF00;
            gui.fill(baseX - 2, btnY, baseX + 110, btnY + EDIT_SLOT_HEIGHT, btnBgColor);

            String newBtnText = "+";
            int textColor = hoveredNewButton ? 0xFF00FF00 : 0xFF00AA00;
            gui.drawString(font, newBtnText, baseX + 10, btnY + 4, textColor);
        }
    }

    private void renderRightAvailablePage(GuiGraphics gui) {
        int baseX = leftPos + PAGE_WIDTH + PAGE_GAP + AVAILABLE_GRID_LEFT;
        int baseY = topPos + AVAILABLE_GRID_TOP;

        int totalPages = (int) Math.ceil((double) availableBookmarks.size() / itemsPerPage);

        gui.drawString(font, Component.translatable("persona.available_marks"), baseX, baseY - 10, 0xFFaa8979, false);

        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, availableBookmarks.size());

        int displayIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            BookmarkData bookmark = availableBookmarks.get(i);

            int row = displayIndex / AVAILABLE_COLS;
            int col = displayIndex % AVAILABLE_COLS;

            if (row >= AVAILABLE_ROWS) break;

            int slotX = baseX + col * (AVAILABLE_SLOT_SIZE + AVAILABLE_SLOT_SPACING + 20);
            int slotY = baseY + row * (AVAILABLE_SLOT_SIZE + AVAILABLE_SLOT_SPACING - 15);

            boolean isHovered = hoveredAvailableSlot == i;
            int offset = isHovered ? 2 : 0;
            Identifier texture = getBookmarkTexture(bookmark.colorIndex);
            gui.blit(RenderPipelines.GUI_TEXTURED, texture,
                    slotX + 2 - offset, slotY + 2 - offset, 0, 0,
                    BOOKMARK_WIDTH - 4, BOOKMARK_HEIGHT - 2,
                    BOOKMARK_WIDTH - 4, BOOKMARK_HEIGHT - 2);

            String displayName = bookmark.name;
            if (font.width(displayName) > AVAILABLE_SLOT_SIZE - 4) {
                displayName = font.plainSubstrByWidth(displayName, AVAILABLE_SLOT_SIZE - 8) + "...";
            }
            gui.drawString(font, displayName,
                    slotX + AVAILABLE_SLOT_SIZE / 2,
                    slotY + AVAILABLE_SLOT_SIZE / 3,
                    0xFFaa8979);
            displayIndex++;
        }
    }

    private void renderDraggedBookmark(GuiGraphics gui, int mouseX, int mouseY) {
        if (draggedBookmark == null) return;

        Identifier texture = getBookmarkTexture(draggedBookmark.colorIndex);
        gui.blit(RenderPipelines.GUI_TEXTURED, texture,
                mouseX - 25, mouseY - 10,
                0, 0, BOOKMARK_WIDTH, BOOKMARK_HEIGHT, BOOKMARK_WIDTH, BOOKMARK_HEIGHT);

        gui.setTooltipForNextFrame(Component.literal(draggedBookmark.name), mouseX, mouseY);
    }

    private void renderInstructions(GuiGraphics gui) {
        gui.drawCenteredString(font, Component.translatable("persona.bookmark_tip"),
                this.width / 2, topPos + PAGE_HEIGHT + 15,
                0xFF888888);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean button) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (hoveredActiveSlot >= 0 && hoveredActiveSlot < activeBookmarks.size()) {
            int baseX = leftPos + EDIT_AREA_LEFT;
            int baseY = topPos + EDIT_AREA_TOP;
            int slotY = baseY + hoveredActiveSlot * EDIT_SLOT_HEIGHT;
            int deleteBtnX = baseX + 100;

            if (mouseX >= deleteBtnX && mouseX < deleteBtnX + 10 &&
                    mouseY >= slotY && mouseY < slotY + 10) {
                String name = activeBookmarks.get(hoveredActiveSlot).name;
                PersonaClient.getBookMarkManager().deactivate(name);
                activeBookmarks.remove(hoveredActiveSlot);
                stopEditing();
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }

        if (hoveredNewButton) {
            createNewBookmark();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        if (hoveredActiveSlot >= 0 && !isDragging) {
            startEditing(hoveredActiveSlot);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        if (editingBox != null && editingSlot >= 0) {
            stopEditing();
        }

        if (hoveredAvailableSlot >= 0) {
            startDragging(availableBookmarks.get(hoveredAvailableSlot), DragSource.AVAILABLE);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        if (hoveredSideBookmark >= 0 && hoveredSideBookmark < activeBookmarks.size()) {
            startDragging(activeBookmarks.get(hoveredSideBookmark), DragSource.ACTIVE);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        return super.mouseClicked(event, button);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        if (isDragging) {
            handleDrop();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dragX, double dragY) {
        if (isDragging) {
            dragMouseX = (int) event.x();
            dragMouseY = (int) event.y();
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (editingBox != null && editingBox.isFocused()) {
            if (editingBox.keyPressed(event)) {
                return true;
            }
            if (event.input() == 256 || event.input() == 257) {
                stopEditing();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (editingBox != null && editingBox.isFocused()) {
            return editingBox.charTyped(event);
        }
        return super.charTyped(event);
    }

    private void startDragging(BookmarkData bookmark, DragSource source) {
        draggedBookmark = bookmark;
        dragSource = source;
        isDragging = true;
        stopEditing();
    }

    private void handleDrop() {
        if (draggedBookmark == null) {
            isDragging = false;
            return;
        }

        BookMarkManager manager = PersonaClient.getBookMarkManager();

        if (delete.isHovered()) {
            String name = draggedBookmark.name;
            manager.remove(name);
            activeBookmarks.removeIf(b -> b.name.equals(name));
            availableBookmarks.removeIf(b -> b.name.equals(name));
        }
        else if (dragSource == DragSource.AVAILABLE) {
            int scaledWidth = (int) (BOOKMARK_WIDTH * 0.5);
            int bookmarkX = leftPos - scaledWidth + ACTIVE_BOOKMARK_X_OFFSET;
            int startY = topPos + 20;
            int scaledHeight = (int) (BOOKMARK_HEIGHT * 0.5);
            int endY = startY + MAX_ACTIVE_BOOKMARKS * (scaledHeight + BOOKMARK_SPACING);

            if (mouseX >= bookmarkX - 4 && mouseX < bookmarkX + scaledWidth &&
                    mouseY >= startY && mouseY < endY &&
                    activeBookmarks.size() < MAX_ACTIVE_BOOKMARKS) {

                boolean exists = activeBookmarks.stream()
                        .anyMatch(b -> b.name.equals(draggedBookmark.name));

                if (!exists) {
                    manager.activate(draggedBookmark.name);
                    activeBookmarks.add(draggedBookmark.copy());
                }
            }
        }
        else if (dragSource == DragSource.ACTIVE && hoveredSideBookmark >= 0) {
            int fromIndex = activeBookmarks.indexOf(draggedBookmark);
            if (fromIndex >= 0 && hoveredSideBookmark != fromIndex) {
                activeBookmarks.remove(fromIndex);
                int toIndex = Math.min(hoveredSideBookmark, activeBookmarks.size());
                activeBookmarks.add(toIndex, draggedBookmark);

                syncActiveToManager();
            }
        }

        isDragging = false;
        draggedBookmark = null;
        dragSource = DragSource.NONE;
    }

    private void startEditing(int slot) {
        if (slot < 0 || slot >= activeBookmarks.size()) return;

        stopEditing();

        editingSlot = slot;
        BookmarkData bookmark = activeBookmarks.get(slot);

        int baseX = leftPos + EDIT_AREA_LEFT;
        int baseY = topPos + EDIT_AREA_TOP;
        int slotY = baseY + slot * EDIT_SLOT_HEIGHT;

        editingBox = new EditBox(font, baseX + 30, slotY + 5, 70, 10, Component.literal(""));
        editingBox.setBordered(false);
        editingBox.setEditable(true);
        editingBox.setValue(bookmark.name);
        editingBox.setTextColor(0xFFaa8979);
        editingBox.setFocused(true);
        editingBox.setResponder(text -> {
            if (editingSlot >= 0 && editingSlot < activeBookmarks.size()) {
                BookMarkManager manager = PersonaClient.getBookMarkManager();
                if (!manager.canReplace(activeBookmarks.get(editingSlot).name, text)) {
                    editingBox.setTextColor(0xFF9e3535);
                } else {
                    editingBox.setTextColor(0xFFaa8979);
                }
            }
        });
        this.addWidget(editingBox);
        this.setFocused(editingBox);
    }

    private void stopEditing() {
        if (editingBox != null && editingSlot >= 0 && editingSlot < activeBookmarks.size()) {
            String oldName = activeBookmarks.get(editingSlot).name;
            String newName = editingBox.getValue();

            BookMarkManager manager = PersonaClient.getBookMarkManager();

            if (manager.canReplace(oldName, newName) && manager.replace(oldName, newName)) {

                activeBookmarks.get(editingSlot).name = newName;

                availableBookmarks.stream()
                        .filter(b -> b.name.equals(oldName))
                        .forEach(b -> b.name = newName);
            }
        }

        if (editingBox != null) {
            this.removeWidget(editingBox);
            editingBox = null;
        }
        editingSlot = -1;
    }

    private void createNewBookmark() {
        if (activeBookmarks.size() >= MAX_ACTIVE_BOOKMARKS) {
            return;
        }

        int colorIndex = (int) (Math.random() * 8);
        BookmarkData newBookmark = new BookmarkData("New Bookmark " + nextCustomBookmarkIndex, colorIndex);
        nextCustomBookmarkIndex++;

        BookMarkManager manager = PersonaClient.getBookMarkManager();

        manager.add(newBookmark.name);
        availableBookmarks.add(newBookmark.copy());

        manager.activate(newBookmark.name);
        activeBookmarks.add(newBookmark);

        int totalPages = (int) Math.ceil((double) availableBookmarks.size() / itemsPerPage);
        currentPage = totalPages - 1;

        startEditing(activeBookmarks.size() - 1);
    }

    private void syncActiveToManager() {
        BookMarkManager manager = PersonaClient.getBookMarkManager();
        List<String> activeNames = activeBookmarks.stream()
                .map(b -> b.name)
                .toList();

        for (String name : manager.getActiveMarks()) {
            manager.deactivate(name);
        }
        for (String name : activeNames) {
            manager.activate(name);
        }
    }

    private boolean isMouseOverNewButton(int mouseX, int mouseY) {
        if (activeBookmarks.size() >= MAX_ACTIVE_BOOKMARKS) {
            return false;
        }

        int baseX = leftPos + EDIT_AREA_LEFT;
        int baseY = topPos + EDIT_AREA_TOP;
        int btnY = baseY + activeBookmarks.size() * EDIT_SLOT_HEIGHT;

        return mouseX >= baseX - 2 && mouseX < baseX + 110 &&
                mouseY >= btnY - 2 && mouseY < btnY + 18;
    }

    private int getSideBookmarkSlot(int mouseX, int mouseY) {
        int scaledWidth = 27;
        int scaledHeight = 11;
        int bookmarkX = leftPos - scaledWidth + ACTIVE_BOOKMARK_X_OFFSET;
        int startY = topPos + 20;

        for (int i = 0; i <= activeBookmarks.size() && i < MAX_ACTIVE_BOOKMARKS; i++) {
            int y = startY + i * (scaledHeight + BOOKMARK_SPACING);

            if (mouseX >= bookmarkX - 4 && mouseX < bookmarkX + scaledWidth &&
                    mouseY >= y && mouseY < y + scaledHeight) {
                return i;
            }
        }
        return -1;
    }

    private int getActiveSlot(int mouseX, int mouseY) {
        int baseX = leftPos + EDIT_AREA_LEFT;
        int baseY = topPos + EDIT_AREA_TOP;

        for (int i = 0; i < activeBookmarks.size(); i++) {
            int slotY = baseY + i * EDIT_SLOT_HEIGHT;

            if (mouseX >= baseX - 2 && mouseX < baseX + 110 &&
                    mouseY >= slotY - 2 && mouseY < slotY + EDIT_SLOT_HEIGHT) {
                return i;
            }
        }
        return -1;
    }

    private int getAvailableSlot(int mouseX, int mouseY) {
        int baseX = leftPos + PAGE_WIDTH + PAGE_GAP + AVAILABLE_GRID_LEFT;
        int baseY = topPos + AVAILABLE_GRID_TOP;

        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, availableBookmarks.size());

        int displayIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            int row = displayIndex / AVAILABLE_COLS;
            int col = displayIndex % AVAILABLE_COLS;

            if (row >= AVAILABLE_ROWS) break;

            int slotX = baseX + col * (AVAILABLE_SLOT_SIZE + AVAILABLE_SLOT_SPACING + 20);
            int slotY = baseY + row * (AVAILABLE_SLOT_SIZE + AVAILABLE_SLOT_SPACING - 15);

            if (mouseX >= slotX && mouseX < slotX + AVAILABLE_SLOT_SIZE + 20 &&
                    mouseY >= slotY && mouseY < slotY + AVAILABLE_SLOT_SIZE - 15) {
                return i;
            }

            displayIndex++;
        }
        return -1;
    }

    private Identifier getBookmarkTexture(int index) {
        return switch (index % 8) {
            case 0 -> BOOKMARK_1;
            case 1 -> BOOKMARK_2;
            case 2 -> BOOKMARK_3;
            case 3 -> BOOKMARK_4;
            case 4 -> BOOKMARK_5;
            case 5 -> BOOKMARK_6;
            case 6 -> BOOKMARK_7;
            default -> BOOKMARK_8;
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        PersonaClient.getRosterDataManager().getCurrentRoster().ifPresent(rosterData -> {
            if (PersonaClient.playerCategoryData.canUseRoster()) {
                Minecraft.getInstance().setScreen(new RosterViewScreen(rosterData));
            }
        });
    }

    private static class BookmarkData {
        String name;
        int colorIndex;

        public BookmarkData(String name, int colorIndex) {
            this.name = name;
            this.colorIndex = colorIndex;
        }

        public BookmarkData copy() {
            return new BookmarkData(this.name, this.colorIndex);
        }
    }

    private enum DragSource {
        NONE,
        ACTIVE,
        AVAILABLE
    }
}