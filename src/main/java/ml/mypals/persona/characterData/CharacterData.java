package ml.mypals.persona.characterData;

import com.google.gson.annotations.SerializedName;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

public class CharacterData {
    @SerializedName("corresponding_player")
    private String correspondingPlayer;

    @SerializedName("character_id")
    private String characterId;

    @SerializedName("custom_name")
    private String customName;

    @SerializedName("skin_texture")
    private String skinTexture;

    @SerializedName("create_time")
    private long createTime;

    @SerializedName("deleted")
    private boolean discarded;

    private CharacterSkin characterSkin;

    public static final StreamCodec<FriendlyByteBuf, CharacterData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    CharacterData::getCorrespondingPlayer,
                    ByteBufCodecs.STRING_UTF8,
                    CharacterData::getCharacterId,
                    ByteBufCodecs.STRING_UTF8,
                    CharacterData::getCustomName,
                    ByteBufCodecs.STRING_UTF8,
                    CharacterData::getSkinTexture,
                    ByteBufCodecs.VAR_LONG,
                    CharacterData::getCreateTime,
                    ByteBufCodecs.BOOL,
                    CharacterData::isDiscarded,
                    CharacterData::fromPacket
            );
    public static CharacterData fromPacket(String correspondingPlayer, String characterId, String customName, String skinTexture, long createTime, boolean deleted) {
        CharacterData characterData = new CharacterData(correspondingPlayer,characterId,customName,skinTexture);
        characterData.createTime = createTime;
        if(deleted)characterData.setDiscard();
        return characterData;
    }
    public CharacterData(String correspondingPlayer, String characterId, String customName, String skinTexture) {
        this.correspondingPlayer = correspondingPlayer;
        this.characterId = characterId;
        this.customName = customName;
        this.skinTexture = skinTexture;
        this.createTime = System.currentTimeMillis();
        this.discarded = false;
    }

    public String getCharacterId() {
        return characterId;
    }

    public String getCustomName() {
        return customName;
    }

    public String getSkinTexture() {
        return skinTexture;
    }

    public String getCorrespondingPlayer() {
        return correspondingPlayer;
    }

    public long getCreateTime() {
        return createTime;
    }
    public boolean isDiscarded(){
        return discarded;
    }
    public void setCharacterSkin(CharacterSkin characterSkin){this.characterSkin = characterSkin;}
    public @Nullable CharacterSkin getCharacterSkin(){return characterSkin;}
    public void setCustomName(String customName) {
        this.customName = customName;
    }
    public void setDiscard(){
        discarded = true;
    }
}
