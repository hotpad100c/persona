package ml.mypals.persona.network.packets.roster;

import ml.mypals.persona.items.rosterData.PlayerRosterData;
import ml.mypals.persona.items.rosterData.RosterEntry;
import ml.mypals.persona.network.PersonaNetworkingConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;


public record RosterSyncS2CPayload(PlayerRosterData playerRosterData)
        implements CustomPacketPayload {

    public static final Type<@NotNull RosterSyncS2CPayload> TYPE =
            new Type<>(PersonaNetworkingConstants.ROSTER_SYNC_S2C);

    public static final StreamCodec<FriendlyByteBuf, RosterSyncS2CPayload> STREAM_CODEC =
            PlayerRosterData.STREAM_CODEC
                    .map(RosterSyncS2CPayload::new, RosterSyncS2CPayload::playerRosterData);

    public static final TypeAndCodec<@NotNull FriendlyByteBuf, @NotNull RosterSyncS2CPayload> TYPE_AND_CODEC =
            new TypeAndCodec<>(TYPE, STREAM_CODEC);

    @Override
    public @NotNull Type<@NotNull RosterSyncS2CPayload> type() { return TYPE; }
}
