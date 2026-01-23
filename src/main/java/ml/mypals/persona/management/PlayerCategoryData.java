package ml.mypals.persona.management;
import com.google.gson.annotations.SerializedName;
import java.util.UUID;
import com.google.gson.annotations.SerializedName;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public class PlayerCategoryData {
    @SerializedName("player_id")
    private String playerId;

    @SerializedName("category_id")
    private String categoryId;

    @SerializedName("category_name")
    private String categoryName;

    @SerializedName("max_characters")
    private int maxCharacters;

    @SerializedName("can_use_roster")
    private boolean canUseRoster;

    @SerializedName("roster_level")
    private int rosterLevel;//0 : NO UI; 1:UI, NO DESC; 2:FULL

    public static final StreamCodec<FriendlyByteBuf, PlayerCategoryData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, PlayerCategoryData::getPlayerIdRaw,
                    ByteBufCodecs.STRING_UTF8, PlayerCategoryData::getCategoryId,
                    ByteBufCodecs.STRING_UTF8, PlayerCategoryData::getCategoryName,
                    ByteBufCodecs.INT, PlayerCategoryData::getMaxCharacters,
                    ByteBufCodecs.BOOL, PlayerCategoryData::canUseRoster,
                    ByteBufCodecs.INT, PlayerCategoryData::getRosterLevel,
                    PlayerCategoryData::new
            );

    public PlayerCategoryData(
            String playerId,
            String categoryId,
            String categoryName,
            int maxCharacters,
            boolean canUseRoster,
            int rosterLevel
    ) {
        this.playerId = playerId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.maxCharacters = maxCharacters;
        this.canUseRoster = canUseRoster;
        this.rosterLevel = rosterLevel;
    }

    public PlayerCategoryData(UUID playerId, MemberEntry category) {
        this.playerId = playerId.toString();
        this.categoryId = category.getCategoryId();
        this.categoryName = category.getCategoryName();
        this.maxCharacters = category.getMaxCharacters();
        this.canUseRoster = category.canUseRoster();
        this.rosterLevel = category.getRosterLevel();
    }

    public UUID getPlayerId() {
        return UUID.fromString(playerId);
    }

    public String getPlayerIdRaw() {
        return playerId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getMaxCharacters() {
        return maxCharacters;
    }

    public int getRosterLevel(){
        return rosterLevel;
    }

    public boolean canUseRoster() {
        return canUseRoster;
    }
}