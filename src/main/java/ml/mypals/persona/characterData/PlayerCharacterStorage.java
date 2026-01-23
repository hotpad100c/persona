package ml.mypals.persona.characterData;

import com.google.gson.annotations.SerializedName;
import ml.mypals.persona.Persona;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerCharacterStorage {
    @SerializedName("player_id")
    private String playerId;

    @SerializedName("fallback_character_id")
    private String fallbackCharacter;

    @SerializedName("current_character_id")
    private String currentCharacterId;

    @SerializedName("characters")
    private List<CharacterData> characters;

    public PlayerCharacterStorage(UUID playerId, String fallbackCharacter) {
        this.playerId = playerId.toString();
        this.characters = new ArrayList<>();
        this.currentCharacterId = null;
    }

    public boolean addCharacter(CharacterData character) {

        if (getCharacterByName(character.getCharacterId()).isPresent()) {
            return false;
        }
        characters.add(character);
        if (characters.size() == 1) {
            currentCharacterId = character.getCharacterId();
        }
        return true;
    }

    public boolean switchCharacter(String characterName) {
        Optional<CharacterData> characterId = getCharacterByName(characterName);
        if (characterId.isPresent()) {
            currentCharacterId = characterId.get().getCharacterId();
            return true;
        }
        return false;
    }

    public boolean deleteCharacter(String characterId) {
        Optional<CharacterData> character = getCharacterByName(characterId);
        if (character.isPresent()) {
            //characters.remove(character.get());
            character.get().setDiscard();
            Persona.getRosterDataManager().unloadCharacterRoster(character.get());
            if (characterId.equals(currentCharacterId)) {
                currentCharacterId = characters.isEmpty() ? null : characters.getFirst().getCharacterId();
            }
            return true;
        }
        return false;
    }

    public Optional<CharacterData> getCharacterByName(String characterName) {
        return characters.stream()
                .filter(c -> c.getCustomName().equals(characterName))
                .findFirst();
    }
    public Optional<CharacterData> getCharacterByID(String currentCharacterId) {
        return characters.stream()
                .filter(c -> c.getCharacterId().equals(currentCharacterId))
                .findFirst();
    }
    public String getFallbackCharacter(){
        return fallbackCharacter;
    }
    public Optional<CharacterData> getCurrentCharacter() {
        if (currentCharacterId == null) {
            return Optional.empty();
        }
        return getCharacterByID(currentCharacterId);
    }

    public UUID getPlayerId() {
        return UUID.fromString(playerId);
    }
    public int getCount(){return characters.size();}
    public List<CharacterData> getCharacters() {
        return new ArrayList<>(characters);
    }

    public @Nullable String getCurrentCharacterId() {
        return currentCharacterId;
    }
    public void foreach(Consumer<CharacterData> consumer){
        for(CharacterData character : characters){
            consumer.accept(character);
        }
    }
}
