package ml.mypals.persona.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import static ml.mypals.persona.Persona.MOD_ID;

public class RosterMultiLineEditBox extends MultiLineEditBox {

    private static final Identifier SCROLLER_SPRITE = Identifier.fromNamespaceAndPath(MOD_ID,"textures/gui/scroller.png");
    private static final Identifier SCROLLER_BACKGROUND_SPRITE = Identifier.fromNamespaceAndPath(MOD_ID,"textures/gui/scroller_background.png");


    RosterMultiLineEditBox(Font font, int i, int j, int k, int l, Component component, Component component2, int m, boolean bl, int n, boolean bl2, boolean bl3) {
        super(font, i, j, k, l, component, component2, m, bl, n, bl2, bl3);
    }

    protected void renderScrollbar(@NotNull GuiGraphics guiGraphics, int i, int j) {
        if (this.scrollbarVisible()) {
            int k = this.scrollBarX();
            int l = Math.min(32,this.scrollerHeight());
            int m = this.scrollBarY();
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLER_BACKGROUND_SPRITE, k, this.getY(),0,0, 6, m-50, 6, m);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, k, m,0,0, 6, l, 6, l);
            if (this.isOverScrollbar((double)i, (double)j)) {
                guiGraphics.requestCursor(this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
            }
        }

    }
}
