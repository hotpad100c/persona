package ml.mypals.persona.network.packets.roster;

import ml.mypals.persona.items.rosterData.AddCharacterToRosterData;
import ml.mypals.persona.network.PersonaNetworkingConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record AddToRosterC2SPayload(AddCharacterToRosterData data) implements CustomPacketPayload {

    public static final Type<@NotNull AddToRosterC2SPayload> TYPE =
            new Type<>(PersonaNetworkingConstants.ADD_TO_ROSTER_C2S);

    public static final StreamCodec<FriendlyByteBuf, AddToRosterC2SPayload> STREAM_CODEC =
            AddCharacterToRosterData.STREAM_CODEC.map(
                    AddToRosterC2SPayload::new,
                    AddToRosterC2SPayload::data
            );


    public static final TypeAndCodec<@NotNull FriendlyByteBuf, @NotNull AddToRosterC2SPayload> TYPE_AND_CODEC =
            new TypeAndCodec<>(TYPE, STREAM_CODEC);

    @Override
    public @NotNull Type<@NotNull AddToRosterC2SPayload> type() {
        return TYPE;
    }
}

