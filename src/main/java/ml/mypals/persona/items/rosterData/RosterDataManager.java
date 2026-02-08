package ml.mypals.persona.items.rosterData;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.properties.Property;
import ml.mypals.persona.Persona;
import ml.mypals.persona.characterData.CharacterData;
import ml.mypals.persona.characterData.CharacterManager;
import ml.mypals.persona.characterData.CharacterSkin;
import ml.mypals.persona.network.packets.roster.RosterDeltaSyncC2SPayload;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class RosterDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_DIR = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("persona")
            .resolve("rosters");

    private final Map<CharacterData, PlayerRosterData> activeRosterStorage = new HashMap<>();

    public RosterDataManager() {
        try {
            Files.createDirectories(DATA_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create roster data directory", e);
        }
    }

    public PlayerRosterData getPlayerRoster(CharacterData owner) {
        if (activeRosterStorage.containsKey(owner)) {
            return activeRosterStorage.get(owner);
        }

        File file = getRosterFile(
                UUID.fromString(owner.getCorrespondingPlayer()),
                owner.getCharacterId()
        );

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                PlayerRosterData data = GSON.fromJson(reader, PlayerRosterData.class);
                activeRosterStorage.put(owner, data);
                return data;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        PlayerRosterData data = new PlayerRosterData(owner);
        activeRosterStorage.put(owner, data);
        return data;
    }

    public void savePlayerRoster(CharacterData owner) {
        PlayerRosterData data = activeRosterStorage.get(owner);
        if (data == null) {
            return;
        }

        File file = getRosterFile(
                UUID.fromString(owner.getCorrespondingPlayer()),
                owner.getCharacterId()
        );

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public RosterEntry recordCharacter(CharacterData owner, ServerPlayer targetPlayer, UUID playerId, String characterId,
                                   String nickname, String notes) {
        PlayerRosterData roster = getPlayerRoster(owner);
        RosterEntry entry = new RosterEntry(playerId.toString(), characterId, nickname, notes);

        entry.setCharacterSkin(generateCharacterSkinData(targetPlayer,playerId,characterId));

        boolean success = roster.addEntry(entry);

        if (success) {
            savePlayerRoster(owner);
        }
        return entry;
    }

    public CharacterSkin generateCharacterSkinData(ServerPlayer serverPlayer,UUID playerId, String characterId){
        Property property = Persona.getCharacterManager().getPlayerSkinTexture(serverPlayer);
        Optional<CharacterData> characterData = Persona.getCharacterManager().getPlayerCharacters(serverPlayer,playerId).getCurrentCharacter();
        AtomicReference<String> skinUrl = new AtomicReference<>(CharacterSkin.TEMP_URL);
        characterData.ifPresent(cd-> skinUrl.set(cd.getSkinTexture()));
        return  new CharacterSkin(characterId,property.value(), property.signature(), skinUrl.get());

    }

    public boolean updateNickname(CharacterData owner, UUID playerId, String characterId, String newNickname) {
        PlayerRosterData roster = getPlayerRoster(owner);
        return roster.getEntry(playerId.toString(), characterId).map(entry -> {
            entry.setNickname(newNickname);
            savePlayerRoster(owner);
            return true;
        }).orElse(false);
    }

    public boolean updateNotes(CharacterData owner, UUID playerId, String characterId, String newNotes) {
        PlayerRosterData roster = getPlayerRoster(owner);
        return roster.getEntry(playerId.toString(), characterId).map(entry -> {
            entry.setNotes(newNotes);
            savePlayerRoster(owner);
            return true;
        }).orElse(false);
    }

    public boolean setGroup(CharacterData owner, UUID playerId, String characterId, String group) {
        PlayerRosterData roster = getPlayerRoster(owner);
        return roster.getEntry(playerId.toString(), characterId).map(entry -> {
            entry.setGroup(group);
            savePlayerRoster(owner);
            return true;
        }).orElse(false);
    }

    public boolean toggleStar(CharacterData owner, UUID playerId, String characterId) {
        PlayerRosterData roster = getPlayerRoster(owner);
        return roster.getEntry(playerId.toString(), characterId).map(entry -> {
            entry.setStarred(!entry.isStarred());
            savePlayerRoster(owner);
            return true;
        }).orElse(false);
    }

    public boolean removeEntry(CharacterData owner, UUID playerId, String characterId) {
        PlayerRosterData roster = getPlayerRoster(owner);
        boolean success = roster.removeEntry(playerId.toString(), characterId);
        if (success) {
            savePlayerRoster(owner);
        }
        return success;
    }

    private File getRosterFile(UUID playerId, String characterId) {
        try {
            Files.createDirectories(DATA_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create roster directory: " + DATA_DIR, e);
        }

        String fileName = playerId + "_" + characterId + "_roster.json";
        return DATA_DIR.resolve(fileName).toFile();
    }


    public void unloadCharacterRoster(CharacterData owner) {
        savePlayerRoster(owner);
        activeRosterStorage.remove(owner);
    }

    public void loadCharacterRoster(ServerPlayer serverPlayer, CharacterData owner) {
        PlayerRosterData playerRosterData = getPlayerRoster(owner);
        //if(playerRosterData != null) ServerPlayNetworking.send(serverPlayer, new RosterSyncS2CPayload(playerRosterData));
    }

    public void unloadPlayer(UUID playerId) {
        activeRosterStorage.entrySet().removeIf(entry -> {
            CharacterData character = entry.getKey();
            if (character.getCorrespondingPlayer().equals(playerId.toString())) {
                savePlayerRoster(character);
                return true;
            }
            return false;
        });
    }
    public void loadPlayer(ServerPlayer owner, UUID ownerId) {
        Optional<CharacterData> characterData = Persona.getCharacterManager().getPlayerCharacters(owner, ownerId).getCurrentCharacter();
        characterData.ifPresent(cd->loadCharacterRoster(owner,cd));
    }

    public void handleClientSync(
            ServerPlayer player,
            RosterDeltaSyncC2SPayload payload
    ) {
        CharacterManager characterManager = Persona.getCharacterManager();
        RosterDataManager rosterDataManager = Persona.getRosterDataManager();

        Optional<CharacterData> optCharData = characterManager
                .getPlayerCharacters(player, player.getUUID())
                .getCurrentCharacter();

        if (optCharData.isEmpty()) {
            return;
        }

        CharacterData currentChar = optCharData.get();

        String payloadCharId = payload.ownerCharacterId();
        if (!currentChar.getCharacterId().equals(payloadCharId)) {
            return;
        }

        PlayerRosterData serverRoster =
                rosterDataManager.getPlayerRoster(currentChar);

        boolean changed = false;

        List<String> toRemoveIds = payload.toRemove();
        if (toRemoveIds != null && !toRemoveIds.isEmpty()) {
            for (String charId : toRemoveIds) {
                boolean removed = serverRoster.removeEntry(
                        serverRoster.getOwnerRawId(),
                        charId
                );
                if (removed) {
                    changed = true;
                }
            }
        }


        List<RosterEntry> toAdd = payload.toAdd();
        if (toAdd != null && !toAdd.isEmpty()) {
            for (RosterEntry newEntry : toAdd) {

                String entryCharId = newEntry.getCharacterId();

                Optional<RosterEntry> existingOpt =
                        serverRoster.getEntry(
                                newEntry.getPlayerId(),
                                entryCharId
                        );

                if (existingOpt.isPresent()) {
                    RosterEntry existing = existingOpt.get();
                    existing.setNickname(newEntry.getNickname());
                    existing.setNotes(newEntry.getNotes());
                    existing.setStarred(newEntry.isStarred());
                    existing.setGroup(newEntry.getGroup());
                    changed = true;
                } else {
                    boolean added = serverRoster.addEntry(newEntry);
                    if (added) {
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            rosterDataManager.savePlayerRoster(currentChar);
        }
    }

}