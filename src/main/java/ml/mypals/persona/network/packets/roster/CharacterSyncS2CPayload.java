package ml.mypals.persona.network.packets.roster;

import ml.mypals.persona.characterData.CharacterData;
import ml.mypals.persona.items.rosterData.AddCharacterToRosterData;
import ml.mypals.persona.network.PersonaNetworkingConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record CharacterSyncS2CPayload(CharacterData characterData) implements CustomPacketPayload {

    public static final Type<@NotNull CharacterSyncS2CPayload> TYPE =
            new Type<>(PersonaNetworkingConstants.CHARACTER_SYNC);

    public static final StreamCodec<FriendlyByteBuf, CharacterSyncS2CPayload> STREAM_CODEC =
            CharacterData.STREAM_CODEC.map(
                    CharacterSyncS2CPayload::new,
                    CharacterSyncS2CPayload::characterData
            );


    public static final CustomPacketPayload.TypeAndCodec<@NotNull FriendlyByteBuf, @NotNull CharacterSyncS2CPayload> TYPE_AND_CODEC =
            new CustomPacketPayload.TypeAndCodec<>(TYPE, STREAM_CODEC);

    @Override
    public @NotNull Type<@NotNull CharacterSyncS2CPayload> type() {return TYPE;}
}