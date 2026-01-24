package ml.mypals.persona.screen;

import com.mojang.authlib.properties.Property;
import ml.mypals.persona.PersonaClient;
import ml.mypals.persona.roster.ClientRosterDataManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.atomic.AtomicReference;

public class CharacterNameHud {
    private static float reveal = 0f;
    public static void render(GuiGraphics context, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.crosshairPickEntity;

        String name = "";

        if (entity instanceof Player player) {
            name = getNameIfRecorded(player);
        }

        boolean hasName = !name.isEmpty();

        float dt = tickCounter.getGameTimeDeltaPartialTick(false);
        float speed = 0.1f;

        if (hasName) {
            reveal = Math.min(1f, reveal + dt * speed);
        } else {
            reveal = Math.max(0f, reveal - dt * speed);
        }

        drawAnimatedName(context, mc, name, reveal);
    }
    private static void drawAnimatedName(GuiGraphics context, Minecraft mc, String name, float t) {
        var font = mc.font;

        int fullWidth = font.width(Component.literal(name).withStyle(ChatFormatting.GOLD));
        int height = 9;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int x = screenW / 2 - fullWidth / 2;
        int y = screenH / 2 + 12;

        int visibleWidth = (int) (fullWidth * easeOutCubic(t));
        context.enableScissor(x, y, x + visibleWidth, y + height);
        context.drawString(font, Component.literal(name).withStyle(ChatFormatting.GOLD), x, y, -2039584, true);
        context.disableScissor();
    }

    private static float easeOutCubic(float t) {
        return 1f - (float) Math.pow(1f - t, 3);

    }

    private static String getNameIfRecorded(Player player){
        ClientRosterDataManager clientRosterDataManager = PersonaClient.getRosterDataManager();
        Property property = player.getGameProfile().properties().get("persona_character_id").stream().findFirst().orElse(null);

        AtomicReference<String> name = new AtomicReference<>("");
        if(property != null){
            clientRosterDataManager.getEntry(player.getUUID().toString(),property.value()).ifPresent(rosterEntry -> {
                name.set(rosterEntry.getNickname());
            });
        }
        return name.get();
    }
}
