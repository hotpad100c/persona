package ml.mypals.persona.characterData;


import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.properties.Property;
import ml.mypals.persona.Persona;
import ml.mypals.persona.management.MemberCategoryManager;
import ml.mypals.persona.network.packets.roster.CharacterSyncS2CPayload;
import ml.mypals.persona.skinHandler.SimpleSkinChanger;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CharacterManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_DIR = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("persona")
            .resolve("characters");

    private final Map<UUID, PlayerCharacterStorage> activeCharacterStorages = new HashMap<>();

    public CharacterManager() {
        try {
            Files.createDirectories(DATA_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create data directory", e);
        }
    }

    public PlayerCharacterStorage getPlayerCharacters(Player player, UUID playerId) {

        if (activeCharacterStorages.containsKey(playerId)) {
            return activeCharacterStorages.get(playerId);
        }

        File file = getPlayerFile(playerId);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                PlayerCharacterStorage data = GSON.fromJson(reader, PlayerCharacterStorage.class);
                activeCharacterStorages.put(playerId, data);
                return data;
            } catch (IOException e) {
                Persona.LOGGER.warn("Error loading player character storage data for player {}", playerId, e);
                e.printStackTrace();
            }
        }

        if(player == null) return null;

        PlayerCharacterStorage data = new PlayerCharacterStorage(playerId, player.getPlainTextName());

        activeCharacterStorages.put(playerId, data);

        //The player dont have a PlayerCharacterStorage, so we create one for him and giv hime a default character!
        createCharacter(player,playerId,player.getPlainTextName(),
                getPlayerSkinTexture(player) == null?
                        "https://textures.minecraft.net/texture/3482c90457e8818c8e62b1f16d9150e080f02010dc5842ce2a83acc740b5dceb"
                        :getPlayerSkinTexture(player).value());

        return data;
    }

    public void savePlayerCharacters(UUID playerId) {
        PlayerCharacterStorage data = activeCharacterStorages.get(playerId);
        if (data == null) {
            return;
        }

        File file = getPlayerFile(playerId);
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (Throwable e) {
            Persona.LOGGER.warn("Error saving player character storage data for player {}", playerId, e);
            e.printStackTrace();
        }
    }

    public boolean createCharacter(Player player, UUID playerId, String characterName, String skinTexture) {
        PlayerCharacterStorage data = getPlayerCharacters(player, playerId);
        MemberCategoryManager memberCategoryManager = Persona.getMemberCategoryManager();
        if(data.getCount() >= memberCategoryManager.getPlayerMaxCharacters(playerId)){
            return false;
        }
        String characterId = "char_" + System.currentTimeMillis();
        CharacterData character = new CharacterData(playerId.toString(), characterId, characterName, skinTexture);

        boolean success = data.addCharacter(character);
        if (success) {
            savePlayerCharacters(playerId);
        }
        return success;
    }



    public boolean switchCharacter(ServerPlayer serverPlayer, UUID playerId, String characterName) {
        PlayerCharacterStorage data = getPlayerCharacters(serverPlayer, playerId);
        boolean success = data.switchCharacter(characterName);
        if (success) {
            savePlayerCharacters(playerId);
            Optional<CharacterData> characterData = getPlayerCharacters(serverPlayer,playerId).getCurrentCharacter();
            characterData.ifPresent(characterData1 ->{
                SimpleSkinChanger.setPlayerTexture(characterData1,serverPlayer, characterData1.getSkinTexture());
                ServerPlayNetworking.send(serverPlayer,new CharacterSyncS2CPayload(characterData1));
            });
        }
        return success;
    }

    public boolean deleteCharacter(ServerPlayer serverPlayer, UUID playerId, String characterId) {
        PlayerCharacterStorage data = getPlayerCharacters(serverPlayer,playerId);
        boolean success = data.deleteCharacter(characterId);
        if (success) {
            if(data.getCurrentCharacterId() == null){
                switchCharacter(serverPlayer,playerId,data.getFallbackCharacter());
            }
            savePlayerCharacters(playerId);
        }
        return success;
    }

    public Property getPlayerSkinTexture(Player player) {
        return Iterables.getFirst(player.getGameProfile().properties().get("textures"),null);
    }

    private File getPlayerFile(UUID playerId) {
        try {
            Files.createDirectories(DATA_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create data dir: " + DATA_DIR, e);
        }
        return DATA_DIR.resolve(playerId.toString() + ".json").toFile();
    }

    public void unloadPlayer(UUID playerId) {
        savePlayerCharacters(playerId);
        activeCharacterStorages.remove(playerId);
    }
    public void loadPlayer(ServerPlayer serverPlayer, UUID playerID){
        Optional<CharacterData> characterData = getPlayerCharacters(serverPlayer,playerID).getCurrentCharacter();
        characterData.ifPresent(characterData1 -> {
            CompletableFuture<CharacterSkin> future = SimpleSkinChanger.setPlayerTexture(characterData1, serverPlayer, characterData1.getSkinTexture());
            future.thenAccept(characterData1::setCharacterSkin).exceptionally(ex -> {
                ex.printStackTrace();
                return null;
            });
        });
    }
}