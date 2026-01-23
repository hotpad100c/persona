package ml.mypals.persona.network.packets.roster;

import ml.mypals.persona.items.rosterData.RosterEntry;
import ml.mypals.persona.network.PersonaNetworkingConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record RosterDeltaSyncS2CPayload(List<RosterEntry> toAdd, List<String> toRemove,
                                        String ownerCharacterId) implements CustomPacketPayload {


    public RosterDeltaSyncS2CPayload(
            List<RosterEntry> toAdd,
            List<String> toRemove,
            String ownerCharacterId
    ) {
        this.toAdd = List.copyOf(toAdd);
        this.toRemove = List.copyOf(toRemove);
        this.ownerCharacterId = ownerCharacterId;
    }

    public static final StreamCodec<FriendlyByteBuf, RosterDeltaSyncS2CPayload> STREAM_CODEC =
            StreamCodec.composite(
                    RosterEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), RosterDeltaSyncS2CPayload::toAdd,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), RosterDeltaSyncS2CPayload::toRemove,
                    ByteBufCodecs.STRING_UTF8, RosterDeltaSyncS2CPayload::ownerCharacterId,
                    RosterDeltaSyncS2CPayload::new
            );
    public static final Type<@NotNull RosterDeltaSyncS2CPayload> TYPE =
            new Type<>(PersonaNetworkingConstants.ROSTER_DELTA_SYNC_S2C);

    public static final TypeAndCodec<@NotNull FriendlyByteBuf, @NotNull RosterDeltaSyncS2CPayload> TYPE_AND_CODEC =
            new TypeAndCodec<>(TYPE, STREAM_CODEC);

    @Override
    public @NotNull Type<@NotNull RosterDeltaSyncS2CPayload> type() {
        return TYPE;
    }
}