package ml.mypals.persona.items.rosterData;


import com.google.gson.annotations.SerializedName;
import ml.mypals.persona.characterData.CharacterSkin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class RosterEntry {
    @SerializedName("cached_texture")
    private CharacterSkin characterSkin;

    @SerializedName("player_id")
    private String playerId;

    @SerializedName("character_id")
    private String characterId;

    @SerializedName("record_time")
    private long recordTime;

    @SerializedName("nickname")
    private String nickname;

    @SerializedName("notes")
    private String notes;

    @SerializedName("group")
    private String group;

    @SerializedName("starred")
    private boolean starred;

    public static final StreamCodec<FriendlyByteBuf, RosterEntry> STREAM_CODEC =
            StreamCodec.composite(
                    CharacterSkin.STREAM_CODEC, RosterEntry::getCharacterSkin,
                    ByteBufCodecs.STRING_UTF8, RosterEntry::getPlayerId,
                    ByteBufCodecs.STRING_UTF8, RosterEntry::getCharacterId,
                    ByteBufCodecs.LONG, RosterEntry::getRecordTime,
                    ByteBufCodecs.STRING_UTF8, RosterEntry::getNickname,
                    ByteBufCodecs.STRING_UTF8, RosterEntry::getNotes,
                    ByteBufCodecs.STRING_UTF8, RosterEntry::getGroup,
                    ByteBufCodecs.BOOL, RosterEntry::isStarred,
                    RosterEntry::newNetwork
            );
    private static RosterEntry newNetwork(
            CharacterSkin characterSkin,
            String playerId,
            String characterId,
            long recordTime,
            String nickname,
            String notes,
            String group,
            boolean starred
    ) {
        RosterEntry e = new RosterEntry(playerId, characterId, nickname, notes);
        e.characterSkin = characterSkin;
        e.recordTime = recordTime;
        e.group = group;
        e.starred = starred;
        return e;
    }
    public RosterEntry(String playerId, String characterId, String nickname, String notes) {
        this.playerId = playerId;
        this.characterId = characterId;
        this.nickname = nickname;
        this.notes = notes;
        this.recordTime = System.currentTimeMillis();
        this.group = "default";
        this.starred = false;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getCharacterId() {
        return characterId;
    }

    public long getRecordTime() {
        return recordTime;
    }

    public String getNickname() {
        return nickname;
    }

    public String getNotes() {
        return notes;
    }

    public String getGroup() {
        return group;
    }

    public CharacterSkin getCharacterSkin() {
        return characterSkin;
    }

    public boolean isStarred() {
        return starred;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setStarred(boolean starred) {
        this.starred = starred;
    }

    public void setCharacterSkin(CharacterSkin texture) {
        this.characterSkin = texture;
    }

    public String getUniqueKey() {
        return playerId + "$" + characterId;
    }
}