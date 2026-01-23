package ml.mypals.persona.characterData;

import com.google.gson.annotations.SerializedName;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class CharacterSkin {
    public static String TEMP_URL = "TEMP";
    @SerializedName("characterId")
    private String characterId;

    @SerializedName("value")
    private String value;

    @SerializedName("signature")
    private String signature;

    @SerializedName("texture_url")
    private String url;
    public static final StreamCodec<FriendlyByteBuf, CharacterSkin> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, CharacterSkin::getCharacterId,
                    ByteBufCodecs.STRING_UTF8, CharacterSkin::getValue,
                    ByteBufCodecs.STRING_UTF8, CharacterSkin::getSignature,
                    ByteBufCodecs.STRING_UTF8, CharacterSkin::getUrl,
                    CharacterSkin::new
            );
    public CharacterSkin(String characterId, String value, String signature, String url) {
        this.characterId = characterId;
        this.value = value;
        this.signature = signature;
        this.url = url;
    }

    public String getCharacterId() {
        return characterId;
    }

    public String getValue() {
        return value;
    }

    public String getSignature() {
        return signature;
    }

    public String getUrl() {
        return url;
    }

}
