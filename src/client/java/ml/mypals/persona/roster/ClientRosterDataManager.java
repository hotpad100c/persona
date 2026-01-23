package ml.mypals.persona.roster;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ml.mypals.persona.PersonaClient;
import ml.mypals.persona.items.rosterData.PlayerRosterData;
import ml.mypals.persona.items.rosterData.RosterEntry;
import ml.mypals.persona.network.packets.roster.RosterDeltaSyncS2CPayload;
import ml.mypals.persona.network.packets.roster.RosterSyncS2CPayload;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ClientRosterDataManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CACHE_DIR = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("persona")
            .resolve("client_rosters");

    private PlayerRosterData currentRoster = null;
    private String currentOwnerPlayerId = null;
    private String currentCharacterId = null;
    public static Map<UUID,String> uUIDToNameCache = new HashMap<>();

    public ClientRosterDataManager() {
        try {
            Files.createDirectories(CACHE_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Path getCacheFilePath(String playerUUID, String characterId) {
        String safeFileName = playerUUID + "_" + characterId + "_roster.json";
        return CACHE_DIR.resolve(safeFileName);
    }

    public void loadFromCache(String playerUUID, String characterId) {

        this.currentOwnerPlayerId = playerUUID;
        this.currentCharacterId = characterId;
        Path path = getCacheFilePath(playerUUID, characterId);
        if (!Files.exists(path)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            PlayerRosterData data = GSON.fromJson(reader, PlayerRosterData.class);
            if (data == null || data.getOwnerCharacterId() == null) {
                return;
            }

            this.currentRoster = data;

        } catch (Exception e) {
            e.printStackTrace();
            try { Files.deleteIfExists(path); } catch (Exception ignored) {}
        }
    }
    private void refreshUUIDToNameCache(){

    }

    private void saveToCache() {
        if (currentRoster == null || currentOwnerPlayerId == null || currentCharacterId == null) {
            return;
        }

        Path path = getCacheFilePath(currentOwnerPlayerId, currentCharacterId);
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(currentRoster, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleSync(RosterDeltaSyncS2CPayload payload) {
        String payloadCharId = payload.ownerCharacterId();
        if (currentCharacterId == null || !currentCharacterId.equals(payloadCharId)) {
            return;
        }

        if (currentRoster == null) {
            PersonaClient.getCharacterManager().getCurrentCharacter().ifPresent(characterData -> {
                currentRoster = new PlayerRosterData(characterData);
            });
            if(currentRoster == null) return;
        }

        boolean changed = false;

        List<String> toRemoveIds = payload.toRemove();
        if (toRemoveIds != null && !toRemoveIds.isEmpty()) {
            for (String charId : toRemoveIds) {
                boolean removed = currentRoster.removeEntry(currentRoster.getOwnerRawId(), charId);
                if (removed) {
                    changed = true;
                }
            }
        }

        List<RosterEntry> toAdd = payload.toAdd();
        if (toAdd != null && !toAdd.isEmpty()) {
            for (RosterEntry newEntry : toAdd) {
                String entryCharId = newEntry.getCharacterId();

                Optional<RosterEntry> existingOpt = currentRoster.getEntry(
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
                    boolean added = currentRoster.addEntry(newEntry);
                    if (added) {
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            saveToCache();
        }
    }

    public void clear() {
        this.currentRoster = null;
        this.currentOwnerPlayerId = null;
        this.currentCharacterId = null;
    }

    public Optional<PlayerRosterData> getCurrentRoster() {
        return Optional.ofNullable(currentRoster);
    }

    public boolean hasData() {
        return currentRoster != null;
    }

    public Optional<String> getCurrentCharacterId() {
        return Optional.ofNullable(currentCharacterId);
    }

    public Optional<RosterEntry> getEntry(String playerId, String charId) {
        if (currentRoster == null) return Optional.empty();
        return currentRoster.getEntry(playerId, charId);
    }

    public String getNicknameOrDefault(String playerId, String charId, String fallback) {
        return getEntry(playerId, charId)
                .map(RosterEntry::getNickname)
                .filter(n -> !n.isBlank())
                .orElse(fallback);
    }

    public boolean isStarred(String playerId, String charId) {
        return getEntry(playerId, charId).map(RosterEntry::isStarred).orElse(false);
    }

    public boolean tryUpdateNicknameLocally(String playerId, String charId, String newNick) {
        if (currentRoster == null) return false;
        return currentRoster.getEntry(playerId, charId).map(entry -> {
            entry.setNickname(newNick);
            saveToCache();
            // sendUpdateNicknameC2S(playerId, charId, newNick);
            return true;
        }).orElse(false);
    }
    public boolean tryUpdateDescriptionLocally(String playerId, String charId, String newDesc) {
        if (currentRoster == null) return false;
        return currentRoster.getEntry(playerId, charId).map(entry -> {
            entry.setNotes(newDesc);
            saveToCache();
            // sendUpdateNicknameC2S(playerId, charId, newNick);
            return true;
        }).orElse(false);
    }

    public boolean tryToggleStarLocally(String playerId, String charId) {
        if (currentRoster == null) return false;
        return currentRoster.getEntry(playerId, charId).map(entry -> {
            entry.setStarred(!entry.isStarred());
            saveToCache();
            // sendToggleStarC2S(...)
            return true;
        }).orElse(false);
    }

    public List<String> getRecorded(){
        if(currentRoster == null){
            return List.of();
        }
        return currentRoster.getEntries().stream().map(RosterEntry::getCharacterId).toList();
    }

}