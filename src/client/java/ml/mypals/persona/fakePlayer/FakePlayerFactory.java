package ml.mypals.persona.fakePlayer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import ml.mypals.persona.characterData.CharacterSkin;
import ml.mypals.persona.mixin.skin.PlayerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.GuiSkinRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class FakePlayerFactory {
    public static Map<String, AbstractClientPlayer> fakePlayers = new HashMap<>();
    private static Map<String, Supplier<PlayerSkin>> skinSuppliers = new HashMap<>();

    public static AbstractClientPlayer getOrGenerateFakePlayer(
            ClientLevel clientLevel,
            String characterName,
            CharacterSkin characterSkin
    ) {
        String characterId = characterSkin.getCharacterId();

        if (fakePlayers.containsKey(characterId)) {
            return fakePlayers.get(characterId);
        }

        GameProfile profile = new GameProfile(UUID.randomUUID(), characterName);
        Property skinProperty = new Property(
                "textures",
                characterSkin.getValue(),
                characterSkin.getSignature()
        );

        GameProfile profileWithSkin = applySkinToPlayer(profile, skinProperty);

        Minecraft mc = Minecraft.getInstance();
        SkinManager skinManager = mc.getSkinManager();

        Supplier<PlayerSkin> skinSupplier = skinManager.createLookup(profileWithSkin, true);
        skinSuppliers.put(characterId, skinSupplier);

        AbstractClientPlayer player = new AbstractClientPlayer(clientLevel, profileWithSkin) {
            @Override
            public @NotNull PlayerSkin getSkin() {
                PlayerSkin playerSkin = skinSuppliers.get(characterId).get();
                return playerSkin != null?playerSkin: DefaultPlayerSkin.getDefaultSkin();
            }
            @Override
            public boolean isModelPartShown(@NotNull PlayerModelPart playerModelPart) {
                return true;
            }

        };
     fakePlayers.put(characterId, player);
        return player;
    }

    private static GameProfile applySkinToPlayer(GameProfile old, Property skinProperty) {
        Multimap<String, Property> copy = HashMultimap.create();
        old.properties().entries().forEach(e -> copy.put(e.getKey(), e.getValue()));
        copy.removeAll("textures");
        copy.put("textures", skinProperty);
        PropertyMap newMap = new PropertyMap(copy);

        return new GameProfile(old.id(), old.name(), newMap);
    }

    public static void clearCache() {
        fakePlayers.clear();
        skinSuppliers.clear();
    }
}
