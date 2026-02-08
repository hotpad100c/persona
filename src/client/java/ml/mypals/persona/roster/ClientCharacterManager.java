package ml.mypals.persona.roster;


import com.google.common.collect.Iterables;
import ml.mypals.persona.Persona;
import ml.mypals.persona.PersonaClient;
import ml.mypals.persona.characterData.CharacterData;
import ml.mypals.persona.network.packets.roster.CharacterSyncS2CPayload;

import java.util.Optional;
import java.util.UUID;

public class ClientCharacterManager {

    private CharacterData currentCharacter = null;
    private UUID currentPlayerId = null;

    public ClientCharacterManager() {
    }

    public void handleSync(CharacterSyncS2CPayload payload) {
        CharacterData newChar = payload.characterData();

        if (newChar == null) {
            clear();
            return;
        }

        this.currentCharacter = newChar;
        this.currentPlayerId = UUID.fromString(newChar.getCorrespondingPlayer());
        PersonaClient.getRosterDataManager().loadFromCache(newChar.getCorrespondingPlayer(),newChar.getCharacterId());
        PersonaClient.getBookMarkManager().loadFromCache(newChar.getCharacterId());
    }

    public void clear() {
        this.currentCharacter = null;
        this.currentPlayerId = null;
    }

    public Optional<CharacterData> getCurrentCharacter() {
        return Optional.ofNullable(currentCharacter);
    }

    public boolean hasCurrentCharacter() {
        return currentCharacter != null;
    }

    public Optional<String> getCurrentCharacterId() {
        return getCurrentCharacter().map(CharacterData::getCharacterId);
    }

    public String getCurrentCharacterNameOrDefault(String fallback) {
        return getCurrentCharacter()
                .map(CharacterData::getCustomName)
                .filter(name -> !name.isBlank())
                .orElse(fallback);
    }

    public Optional<String> getCurrentSkinTexture() {
        return getCurrentCharacter().map(CharacterData::getSkinTexture);
    }

    public boolean isForPlayer(UUID playerId) {
        return currentPlayerId != null && currentPlayerId.equals(playerId);
    }

    private void requestCurrentCharacterFromServer() {
    }
}