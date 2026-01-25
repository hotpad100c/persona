package ml.mypals.persona.network;

import ml.mypals.persona.Persona;
import ml.mypals.persona.characterData.CharacterData;
import ml.mypals.persona.characterData.CharacterManager;
import ml.mypals.persona.characterData.PlayerCharacterStorage;
import ml.mypals.persona.items.rosterData.PlayerRosterData;
import ml.mypals.persona.items.rosterData.RosterDataManager;
import ml.mypals.persona.items.rosterData.RosterEntry;
import ml.mypals.persona.management.PlayerCategoryData;
import ml.mypals.persona.network.packets.category.PlayerCategoryDataPayload;
import ml.mypals.persona.network.packets.roster.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ServerPacketRegister {
    public static void initialize(){
        PayloadTypeRegistry.playS2C().register(PlayerCategoryDataPayload.TYPE, PlayerCategoryDataPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(AddToRosterS2CPayload.TYPE, AddToRosterS2CPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(AddToRosterC2SPayload.TYPE, AddToRosterC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RosterRequestC2SPayload.TYPE, RosterRequestC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(CharacterSyncS2CPayload.TYPE,CharacterSyncS2CPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(RosterDeltaSyncS2CPayload.TYPE,RosterDeltaSyncS2CPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(RosterDeltaSyncC2SPayload.TYPE,RosterDeltaSyncC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenRosterViewScreenPayload.TYPE,OpenRosterViewScreenPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(
                RosterRequestC2SPayload.TYPE,
                (payload, context) -> {

                    context.server().execute(() -> {
                        ServerPlayer player = context.player();
                        RosterDataManager rosterDataManager = Persona.getRosterDataManager();
                        CharacterManager characterManager = Persona.getCharacterManager();

                        Optional<CharacterData> optCharData = characterManager
                                .getPlayerCharacters(player, player.getUUID())
                                .getCurrentCharacter();

                        if (optCharData.isEmpty()) {
                            return;
                        }

                        CharacterData currentChar = optCharData.get();
                        PlayerRosterData serverRoster = rosterDataManager.getPlayerRoster(currentChar);

                        List<String> clientKnownIds = payload.recordedCharacterIds();

                        List<RosterEntry> serverEntries = serverRoster.getEntries();

                        Set<String> serverIdSet = serverEntries.stream()
                                .map(RosterEntry::getCharacterId)
                                .collect(Collectors.toSet());

                        List<RosterEntry> toAdd = serverEntries.stream()
                                .filter(entry -> !clientKnownIds.contains(entry.getCharacterId()))
                                .toList();

                        List<String> toRemove = clientKnownIds.stream()
                                .filter(id -> !serverIdSet.contains(id))
                                .toList();

                        RosterDeltaSyncS2CPayload deltaPayload = new RosterDeltaSyncS2CPayload(
                                toAdd,
                                toRemove,
                                currentChar.getCharacterId()
                        );

                        ServerPlayNetworking.send(player, deltaPayload);

                    });
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(RosterDeltaSyncC2SPayload.TYPE,(payload, context)-> {
            context.server().execute(() -> {

            });
        });

        ServerPlayNetworking.registerGlobalReceiver(AddToRosterC2SPayload.TYPE,(payload, context)->{
            context.server().execute(() -> {
                MinecraftServer minecraftServer = context.server();
                ServerPlayer serverUser = context.player();

                PlayerCharacterStorage userData = Persona.getCharacterManager().getPlayerCharacters(serverUser, serverUser.getUUID());
                Optional<CharacterData> user = userData.getCurrentCharacter();

                ServerPlayer targetPlayer = minecraftServer.getPlayerList().getPlayer(UUID.fromString(payload.data().playerId()));
                if (targetPlayer == null) return;

                PlayerCharacterStorage targetData = Persona.getCharacterManager().getPlayerCharacters(targetPlayer, targetPlayer.getUUID());
                Optional<CharacterData> target = targetData.getCurrentCharacter();

                user.ifPresent(userCharacter -> {
                    target.ifPresent(targetCharacter -> {
                        RosterDataManager rosterManager = Persona.getRosterDataManager();
                        RosterEntry success = rosterManager.recordCharacter(
                                userCharacter,
                                targetPlayer,
                                targetPlayer.getUUID(),
                                targetCharacter.getCharacterId(),
                                payload.data().data(),
                                ""
                        );
                        if (success != null) {
                            serverUser.sendSystemMessage(
                                    Component.translatable("persona.roster.recorded", payload.data().data())
                                            .withStyle(ChatFormatting.GREEN)
                            );

                            List<RosterEntry> toAdd = List.of(success);
                            List<String> toRemove = List.of();
                            RosterDeltaSyncS2CPayload deltaPayload = new RosterDeltaSyncS2CPayload(
                                    toAdd,
                                    toRemove,
                                    userCharacter.getCharacterId()
                            );
                            ServerPlayNetworking.send(serverUser, deltaPayload);

                        } else {
                            serverUser.sendSystemMessage(
                                    Component.translatable("persona.roster.already")
                                            .withStyle(ChatFormatting.YELLOW)
                            );
                        }
                    });
                });
            });
        });
    }
}
