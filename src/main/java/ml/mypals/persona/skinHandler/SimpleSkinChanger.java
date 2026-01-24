
// This code is adapted from SkinRestorer:
// https://github.com/Suiranoil/SkinRestorer
// The original license is included in the "licenses" directory.
package ml.mypals.persona.skinHandler;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import ml.mypals.persona.characterData.CharacterData;
import ml.mypals.persona.characterData.CharacterSkin;
import ml.mypals.persona.mixin.skin.PlayerAccessor;
import ml.mypals.persona.mixin.skin.TrackedEntityAccessor;
import ml.mypals.persona.skinHandler.gson.PostProcessingEnabler;
import ml.mypals.persona.skinHandler.mineskin.Java11RequestHandler;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.server.players.PlayerList;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import org.mineskin.MineSkinClient;
import org.mineskin.data.Variant;
import org.mineskin.data.Visibility;
import org.mineskin.request.GenerateRequest;
import org.mineskin.response.QueueResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.util.UUIDTypeAdapter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class SimpleSkinChanger {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(new PostProcessingEnabler())
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer())
            .setPrettyPrinting()
            .create();
    public static final String TEXTURES_KEY = "textures";
    private static MineSkinClient mineSkinClient;

    public static void initialize() {
        mineSkinClient = MineSkinClient.builder()
                .gson(GSON)
                .timeout((int) Duration.ofSeconds(60000).toMillis())
                .requestHandler((baseUrl, userAgent, apiKey, timeout, gson) -> new Java11RequestHandler(
                        baseUrl,
                        userAgent,
                        apiKey,
                        timeout,
                        gson,
                        null
                ))
                .apiKey("msk_yOFGTLeG_Afzr5gIAZI4UjU6iYHoKpR1fbWjFHGqqH7XLyQpn8XGLZBeIW1SSvZ4MlZdPNMu-")
                .build();

    }

    public static CompletableFuture<CharacterSkin> setPlayerTexture(CharacterData characterData, ServerPlayer player, String textureSource) {
        return setPlayerTexture(characterData, player, textureSource, SkinVariant.CLASSIC);
    }

    public static CompletableFuture<CharacterSkin> setPlayerTexture(CharacterData characterData, ServerPlayer player, String textureSource, SkinVariant variant) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Property skinProperty;

                if (characterData.getCharacterSkin() != null) {
                    skinProperty = new Property(TEXTURES_KEY,
                            characterData.getCharacterSkin().getValue(),
                            characterData.getCharacterSkin().getSignature()
                    );
                }else {
                    skinProperty = loadSkinFromUrl(textureSource, variant);
                }

                applySkinToPlayer(player,characterData, skinProperty);

                return skinProperty;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).thenApplyAsync(skin -> {
            if (skin != null) {
                refreshPlayer(player);
                return new CharacterSkin(characterData.getCharacterId(),skin.value(),skin.signature(),characterData.getSkinTexture());
            }
            return null;
        }, player.level().getServer());
    }

    /**
     * 有MineSkin真是太好了，没想到其实会这么简单
     */
    private static Property loadSkinFromUrl(String url, SkinVariant variant) throws Exception {
        URI uri = new URI(url);

        Variant mineskinVariant = variant == SkinVariant.SLIM ? Variant.SLIM : Variant.CLASSIC;

        GenerateRequest request = GenerateRequest.url(uri)
                .variant(mineskinVariant)
                .name("simple-skin-changer")
                .visibility(Visibility.UNLISTED);

        var skin = mineSkinClient.queue().submit(request)
                .thenApply(QueueResponse::getJob)
                .thenCompose(jobInfo -> jobInfo.waitForCompletion(mineSkinClient))
                .thenCompose(jobReference -> jobReference.getOrLoadSkin(mineSkinClient))
                .join();

        return new Property(
                TEXTURES_KEY,
                skin.texture().data().value(),
                skin.texture().data().signature()
        );
    }

    private static void applySkinToPlayer(ServerPlayer player,CharacterData characterData, Property skinProperty) {
        GameProfile old = player.getGameProfile();

        Multimap<String, Property> copy = HashMultimap.create();
        old.properties().entries().forEach(e -> copy.put(e.getKey(), e.getValue()));

        copy.removeAll(TEXTURES_KEY);
        copy.put(TEXTURES_KEY, skinProperty);
        copy.removeAll("PersonaCharacter");
        copy.put("PersonaCharacter",new Property("persona_character_id", characterData.getCharacterId()));

        PropertyMap newMap = new PropertyMap(copy);

        GameProfile newProfile = new GameProfile(old.id(), characterData.getCustomName(), newMap);

        ((PlayerAccessor) player).setGameProfile(newProfile);
    }

    private static void refreshPlayer(ServerPlayer player) {
        ServerLevel level = player.level();
        PlayerList playerList = level.getServer().getPlayerList();
        ChunkMap chunkMap = level.getChunkSource().chunkMap;

        playerList.broadcastAll(new ClientboundBundlePacket(
                List.of(
                        new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID())),
                        ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(Collections.singleton(player))
                )
        ));


        TrackedEntityAccessor trackedEntity = (TrackedEntityAccessor)chunkMap.entityMap.get(player.getId());
        Set<ServerPlayerConnection> seenBy = Set.copyOf(trackedEntity.getSeenBy());
        for (var observerConnection : seenBy) {
            var observer = observerConnection.getPlayer();
            trackedEntity.invokeRemovePlayer(observer);

            TrackedEntityAccessor trackedObserverEntity = (TrackedEntityAccessor)chunkMap.entityMap.get(observer.getId());
            trackedObserverEntity.invokeRemovePlayer(player);
            trackedObserverEntity.invokeUpdatePlayer(player);
            trackedEntity.invokeUpdatePlayer(observer);
        }

        if (!player.isDeadOrDying()) {
            player.connection.send(new ClientboundBundlePacket(
                    List.of(
                            new ClientboundRespawnPacket(
                                    player.createCommonSpawnInfo(level),
                                    ClientboundRespawnPacket.KEEP_ALL_DATA
                            ),
                            new ClientboundGameEventPacket(
                                    ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START,
                                    0
                            )
                    )
            ));

            player.connection.teleport(
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player.getYRot(),
                    player.getXRot()
            );

            player.connection.send(new ClientboundSetEntityMotionPacket(player));

            var vehicle = player.getVehicle();
            if (vehicle != null) {
                player.connection.send(new ClientboundSetPassengersPacket(vehicle));
            }

            if (!player.getPassengers().isEmpty()) {
                player.connection.send(new ClientboundSetPassengersPacket(player));
            }

            player.onUpdateAbilities();
            player.giveExperiencePoints(0);
            playerList.sendPlayerPermissionLevel(player);
            playerList.sendLevelInfo(player, level);
            playerList.sendAllPlayerInfo(player);

            sendActivePlayerEffects(player);
        }
    }

    private static void sendActivePlayerEffects(ServerPlayer player) {
        for (var effect : player.getActiveEffects()) {
            player.connection.send(new ClientboundUpdateMobEffectPacket(
                    player.getId(),
                    effect,
                    false
            ));
        }
    }

    public enum SkinVariant {
        CLASSIC,
        SLIM
    }
}