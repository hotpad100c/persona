package ml.mypals.persona.network.packets.roster;

import ml.mypals.persona.network.PersonaNetworkingConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record OpenRosterViewScreenPayload() implements CustomPacketPayload {

    public static final Type<@NotNull OpenRosterViewScreenPayload> TYPE =
            new Type<>(PersonaNetworkingConstants.OPEN_ROSTER);

    public static final StreamCodec<FriendlyByteBuf, OpenRosterViewScreenPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenRosterViewScreenPayload());

    public static final TypeAndCodec<@NotNull FriendlyByteBuf, @NotNull OpenRosterViewScreenPayload> TYPE_AND_CODEC =
            new TypeAndCodec<>(TYPE, STREAM_CODEC);

    @Override
    public @NotNull Type<@NotNull OpenRosterViewScreenPayload> type() {
        return TYPE;
    }
}