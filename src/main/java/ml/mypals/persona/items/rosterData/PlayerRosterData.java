package ml.mypals.persona.items.rosterData;


import com.google.gson.annotations.SerializedName;
import ml.mypals.persona.characterData.CharacterData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlayerRosterData {
    @SerializedName("owner_player_id")
    private String ownerId;

    @SerializedName("owner_character_id")
    private String ownerCharacterId;

    @SerializedName("owner_name")
    private String ownerName;

    @SerializedName("entries")
    private List<RosterEntry> entries;

    public static final StreamCodec<FriendlyByteBuf, PlayerRosterData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, PlayerRosterData::getOwnerRawId,
                    ByteBufCodecs.STRING_UTF8, PlayerRosterData::getOwnerCharacterId,
                    ByteBufCodecs.STRING_UTF8, PlayerRosterData::getOwnerName,
                    RosterEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), PlayerRosterData::getEntries,
                    PlayerRosterData::new
            );
    public PlayerRosterData(
            String ownerId,
            String ownerCharacterId,
            String ownerName,
            List<RosterEntry> entries
    ) {
        this.ownerId = ownerId;
        this.ownerCharacterId = ownerCharacterId;
        this.ownerName = ownerName;
        this.entries = entries;
    }

    public PlayerRosterData(CharacterData owner) {
        this.ownerId = owner.getCorrespondingPlayer();
        this.ownerCharacterId = owner.getCharacterId();
        this.ownerName = owner.getCustomName();
        this.entries = new ArrayList<>();
    }

    public boolean addEntry(RosterEntry entry) {
        if (hasEntry(entry.getPlayerId(), entry.getCharacterId())) {
            return false;
        }
        entries.add(entry);
        return true;
    }

    public boolean hasEntry(String playerId, String characterId) {
        String key = playerId + "$" + characterId;
        return entries.stream()
                .anyMatch(e -> e.getUniqueKey().equals(key));
    }

    public Optional<RosterEntry> getEntry(String playerId, String characterId) {
        String key = playerId + "$" + characterId;
        return entries.stream()
                .filter(e -> e.getUniqueKey().equals(key))
                .findFirst();
    }

    public boolean removeEntry(String playerId, String characterId) {
        return entries.removeIf(e ->
                        e.getCharacterId().equals(characterId)
        );
    }

    public List<RosterEntry> getEntriesSortedByTime() {
        List<RosterEntry> sorted = new ArrayList<>(entries);
        sorted.sort((a, b) -> Long.compare(b.getRecordTime(), a.getRecordTime()));
        return sorted;
    }

    public List<RosterEntry> getStarredEntries() {
        return entries.stream()
                .filter(RosterEntry::isStarred)
                .toList();
    }

    public List<RosterEntry> getEntriesByGroup(String group) {
        return entries.stream()
                .filter(e -> e.getGroup().equals(group))
                .toList();
    }

    public UUID getOwnerId() {
        return UUID.fromString(ownerId);
    }
    public String getOwnerRawId() {
        return ownerId;
    }

    public String getOwnerCharacterId() {
        return ownerCharacterId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public List<RosterEntry> getEntries() {
        return new ArrayList<>(entries);
    }
}