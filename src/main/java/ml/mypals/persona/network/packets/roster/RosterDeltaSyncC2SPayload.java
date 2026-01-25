package ml.mypals.persona.network.packets.roster;

import ml.mypals.persona.items.rosterData.RosterEntry;
import ml.mypals.persona.network.PersonaNetworkingConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record RosterDeltaSyncC2SPayload(List<RosterEntry> toAdd, List<String> toRemove,
                                        String ownerCharacterId) implements CustomPacketPayload {


    public RosterDeltaSyncC2SPayload(
            List<RosterEntry> toAdd,
            List<String> toRemove,
            String ownerCharacterId
    ) {
        this.toAdd = List.copyOf(toAdd);
        this.toRemove = List.copyOf(toRemove);
        this.ownerCharacterId = ownerCharacterId;
    }

    public static final StreamCodec<FriendlyByteBuf, RosterDeltaSyncC2SPayload> STREAM_CODEC =
            StreamCodec.composite(
                    RosterEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), RosterDeltaSyncC2SPayload::toAdd,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), RosterDeltaSyncC2SPayload::toRemove,
                    ByteBufCodecs.STRING_UTF8, RosterDeltaSyncC2SPayload::ownerCharacterId,
                    RosterDeltaSyncC2SPayload::new
            );
    public static final Type<@NotNull RosterDeltaSyncC2SPayload> TYPE =
            new Type<>(PersonaNetworkingConstants.ROSTER_DELTA_SYNC_C2S);

    public static final TypeAndCodec<@NotNull FriendlyByteBuf, @NotNull RosterDeltaSyncC2SPayload> TYPE_AND_CODEC =
            new TypeAndCodec<>(TYPE, STREAM_CODEC);

    @Override
    public @NotNull Type<@NotNull RosterDeltaSyncC2SPayload> type() {
        return TYPE;
    }
}