package ml.mypals.persona.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import static ml.mypals.persona.Persona.MOD_ID;


public class RosterEditBox extends EditBox {

    private static final WidgetSprites NAME_EDIT_BOX =
            new WidgetSprites(
                    Identifier.fromNamespaceAndPath(MOD_ID,"textures/gui/name_edit_box.png"),
                    Identifier.fromNamespaceAndPath(MOD_ID,"textures/gui/name_edit_box_h.png"));

    public RosterEditBox(Font font, int i, int j, Component component) {
        super(font, i, j, component);
    }

    public RosterEditBox(Font font, int i, int j, int k, int l, Component component) {
        super(font, i, j, k, l, component);
    }

    public RosterEditBox(Font font, int i, int j, int k, int l, @Nullable EditBox editBox, Component component) {
        super(font, i, j, k, l, editBox, component);
    }

    @Override
    public int getInnerWidth() {
        return this.width - 20;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
        if (this.isVisible()) {

            this.setBordered(false);

            super.renderWidget(guiGraphics, i, j, f);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED,
                    NAME_EDIT_BOX.get(this.isActive(), this.isFocused()),
                    this.getX(), this.getY()+4-this.getHeight()/2, 0, 0,
                    this.getWidth(), this.getHeight(),this.getWidth(), this.getHeight());

        }
    }
}
