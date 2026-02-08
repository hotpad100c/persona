package ml.mypals.persona.items.rosterData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record AddCharacterToRosterData(String playerId, String data, String memo) {

    public static final StreamCodec<FriendlyByteBuf, AddCharacterToRosterData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, AddCharacterToRosterData::playerId,
                    ByteBufCodecs.STRING_UTF8, AddCharacterToRosterData::data,
                    ByteBufCodecs.STRING_UTF8, AddCharacterToRosterData::memo,
                    AddCharacterToRosterData::new
            );
}
