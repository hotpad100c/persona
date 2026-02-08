package ml.mypals.persona.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import static ml.mypals.persona.Persona.MOD_ID;

public class RosterPageButton extends PageButton {
    public RosterPageButton(int i, int j, boolean bl, OnPress onPress, boolean bl2) {
        super(i, j, bl, onPress, bl2);
        this.isForward = bl;
    }
    private static final Identifier PAGE_FORWARD_SPRITE =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/flip_right.png");

    private static final Identifier PAGE_FORWARD_HIGHLIGHTED_SPRITE =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/flip_right_h.png");

    private static final Identifier PAGE_BACKWARD_SPRITE =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/flip_left.png");

    private static final Identifier PAGE_BACKWARD_HIGHLIGHTED_SPRITE =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/flip_left_h.png");


    private final boolean isForward;

    public void renderContents(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
        Identifier identifier;
        if (this.isForward) {
            identifier = this.isHoveredOrFocused() ? PAGE_FORWARD_HIGHLIGHTED_SPRITE : PAGE_FORWARD_SPRITE;
        } else {
            identifier = this.isHoveredOrFocused() ? PAGE_BACKWARD_HIGHLIGHTED_SPRITE : PAGE_BACKWARD_SPRITE;
        }

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, identifier, this.getX(), this.getY(),0,0,29,23,29,23);
    }


    public boolean shouldTakeFocusAfterInteraction() {
        return false;
    }
}
