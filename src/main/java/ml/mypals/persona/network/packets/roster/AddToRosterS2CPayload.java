package ml.mypals.persona.network.packets.roster;

import ml.mypals.persona.items.rosterData.AddCharacterToRosterData;
import ml.mypals.persona.network.PersonaNetworkingConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record AddToRosterS2CPayload(AddCharacterToRosterData data) implements CustomPacketPayload {

    public static final Type<@NotNull AddToRosterS2CPayload> TYPE =
            new Type<>(PersonaNetworkingConstants.ADD_TO_ROSTER_S2C);

    public static final StreamCodec<FriendlyByteBuf, AddToRosterS2CPayload> STREAM_CODEC =
            AddCharacterToRosterData.STREAM_CODEC.map(
                    AddToRosterS2CPayload::new,
                    AddToRosterS2CPayload::data
            );


    public static final CustomPacketPayload.TypeAndCodec<@NotNull FriendlyByteBuf, @NotNull AddToRosterS2CPayload> TYPE_AND_CODEC =
            new CustomPacketPayload.TypeAndCodec<>(TYPE, STREAM_CODEC);

    @Override
    public @NotNull Type<@NotNull AddToRosterS2CPayload> type() {return TYPE;}
}

