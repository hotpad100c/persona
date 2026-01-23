package ml.mypals.persona.network.packets.roster;

import ml.mypals.persona.items.rosterData.PlayerRosterData;
import ml.mypals.persona.items.rosterData.RosterEntry;
import ml.mypals.persona.network.PersonaNetworkingConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record RosterSyncC2SPayload(PlayerRosterData playerRosterData)
        implements CustomPacketPayload {

    public static final Type<@NotNull RosterSyncC2SPayload> TYPE =
            new Type<>(PersonaNetworkingConstants.ROSTER_SYNC_C2S);

    public static final StreamCodec<FriendlyByteBuf, RosterSyncC2SPayload> STREAM_CODEC =
            PlayerRosterData.STREAM_CODEC
                    .map(RosterSyncC2SPayload::new, RosterSyncC2SPayload::playerRosterData);

    public static final TypeAndCodec<@NotNull FriendlyByteBuf, @NotNull RosterSyncC2SPayload> TYPE_AND_CODEC =
            new TypeAndCodec<>(TYPE, STREAM_CODEC);


    @Override
    public @NotNull Type<@NotNull RosterSyncC2SPayload> type() { return TYPE; }
}

