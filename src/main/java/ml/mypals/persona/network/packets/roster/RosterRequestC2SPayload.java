package ml.mypals.persona.network.packets.roster;

import ml.mypals.persona.items.rosterData.PlayerRosterData;
import ml.mypals.persona.network.PersonaNetworkingConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record RosterRequestC2SPayload(List<String> recordedCharacterIds) implements CustomPacketPayload {

    public static final Type<@NotNull RosterRequestC2SPayload> TYPE =
            new Type<>(PersonaNetworkingConstants.ROSTER_REQUEST_C2S);

    public static final StreamCodec<FriendlyByteBuf, RosterRequestC2SPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),RosterRequestC2SPayload::recordedCharacterIds,
                    RosterRequestC2SPayload::new
            );

    public static final TypeAndCodec<@NotNull FriendlyByteBuf, @NotNull RosterRequestC2SPayload> TYPE_AND_CODEC =
            new TypeAndCodec<>(TYPE, STREAM_CODEC);

    @Override
    public @NotNull Type<@NotNull RosterRequestC2SPayload> type() {
        return TYPE;
    }
}
