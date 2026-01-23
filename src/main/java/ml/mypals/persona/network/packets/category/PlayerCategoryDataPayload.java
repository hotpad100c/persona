package ml.mypals.persona.network.packets.category;

import ml.mypals.persona.management.PlayerCategoryData;
import ml.mypals.persona.network.PersonaNetworkingConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record PlayerCategoryDataPayload (PlayerCategoryData playerCategoryData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<@NotNull PlayerCategoryDataPayload> TYPE =
            new CustomPacketPayload.Type<>(PersonaNetworkingConstants.PLAYER_CATEGORY);

    public static final StreamCodec<FriendlyByteBuf, PlayerCategoryDataPayload> STREAM_CODEC =
            PlayerCategoryData.STREAM_CODEC
                    .map(PlayerCategoryDataPayload::new,
                            PlayerCategoryDataPayload::playerCategoryData);

    public static final CustomPacketPayload.TypeAndCodec<@NotNull FriendlyByteBuf, @NotNull PlayerCategoryDataPayload> TYPE_AND_CODEC =
            new CustomPacketPayload.TypeAndCodec<>(TYPE, STREAM_CODEC);

    @Override
    public @NotNull Type<@NotNull PlayerCategoryDataPayload> type() {
        return TYPE;
    }
}
